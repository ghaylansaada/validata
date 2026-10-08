/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ghaylan.validata.constraint.validator.string.html

import io.ghaylan.validata.constraint.annotation.HtmlConstraint
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * Jsoup-backed Html checks. Loaded only after optional Jsoup is present on the classpath.
 *
 * Allow-lists and the dynamic [Safelist] are memoized per [HtmlConstraint] instance.
 * Tag / attribute / protocol violations are detected in a **single** DOM walk so a dirty
 * document does not pay three full `allElements` iterations.
 *
 * A `"*"` entry in an allow-list disables that dimension (any tag / attribute / protocol).
 *
 * Errors: [ConstraintErrorCode.VALUE_NOT_ALLOWED],
 * [ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH]. Failures attach the [HtmlConstraint];
 * messages include the forbidden tag / attribute / scheme name without echoing attribute values
 * or the Html body.
 *
 * @author Ghaylan Saada
 */
internal object HtmlJsoupSupport {

	/**
	 * Parses [rawHtml], enforces tag/attr/protocol allow-lists, and checks Jsoup clean equality.
	 *
	 * Fail-closed: disallowed markup or sanitization drift returns a violation.
	 * Allow-list policy is memoized per [constraint] via [ConstraintLiteralCache].
	 * Neither messages nor the attached constraint carry the submitted Html body or attribute values.
	 * Side effects: may populate [ConstraintLiteralCache] for [constraint].
	 *
	 * @param rawHtml Non-null Html string under validation.
	 * @param constraint Html constraint arguments (allow-lists).
	 * @return Violation with [constraint] attached when applicable, or `null` when acceptable.
	 */
	fun validate(
		rawHtml: String,
		constraint: HtmlConstraint,
	): ConstraintError<*>? {
		val policy = ConstraintLiteralCache.getOrCompute(constraint) { HtmlPolicy.from(constraint) }

		val document = Jsoup.parseBodyFragment(rawHtml)

		checkMarkupPolicy(document, policy, constraint)?.let { return it }

		if (policy.allowAnyTag && policy.allowAnyAttr && policy.allowAnyProtocol) {
			return null
		}

		val cleanedHtml = Jsoup.clean(rawHtml, policy.safelist)
		if (cleanedHtml != rawHtml) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH,
				message = "Html contains markup that is not allowed.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * One pass over body elements for tag, attribute, and protocol policy.
	 */
	private fun checkMarkupPolicy(
		doc: Document,
		policy: HtmlPolicy,
		constraint: HtmlConstraint,
	): ConstraintError<*>? {
		doc.body().allElements.forEach { element ->
			val tag = element.tagName()
			if (tag == "#root" || tag == "body") return@forEach

			if (!policy.allowAnyTag && tag !in policy.allowedTags) {
				return ConstraintError(
					code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
					message = "Html contains a tag that is not allowed: '$tag'.",
					metadata = constraint,
				)
			}

			if (policy.allowAnyAttr && policy.allowAnyProtocol) return@forEach

			val allowedAttrs = if (policy.allowAnyAttr) null else policy.attrsPerTag[tag].orEmpty()
			val attrProtocols = if (policy.allowAnyProtocol) emptyMap() else policy.protocolsPerTagAttr[tag].orEmpty()

			element.attributes().forEach { attr ->
				if (allowedAttrs != null && attr.key !in allowedAttrs) {
					return ConstraintError(
						code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
						message = "Html contains an attribute that is not allowed on tag '$tag': '${attr.key}'.",
						metadata = constraint,
					)
				}

				val allowedProtocols = attrProtocols[attr.key].orEmpty()
				if (allowedProtocols.isNotEmpty()) {
					val protocol = attr.value.substringBefore(':', missingDelimiterValue = "").lowercase()
					if (protocol.isNotEmpty() && protocol !in allowedProtocols) {
						return ConstraintError(
							code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
							message = "Html contains a link using a scheme that is not allowed on '$tag[${attr.key}]': '$protocol'; permitted schemes: ${allowedProtocols.joinToString(", ")}.",
							metadata = constraint,
						)
					}
				}
			}
		}
		return null
	}

	private data class HtmlPolicy(
		val allowAnyTag: Boolean,
		val allowAnyAttr: Boolean,
		val allowAnyProtocol: Boolean,
		val allowedTags: Set<String>,
		val attrsPerTag: Map<String, Set<String>>,
		val protocolsPerTagAttr: Map<String, Map<String, Set<String>>>,
		val safelist: Safelist,
	) {
		companion object {
			fun from(constraint: HtmlConstraint): HtmlPolicy {
				val allowAnyTag = constraint.allowedTags.contains("*")
				val allowAnyAttr = constraint.allowedAttrs.contains("*")
				val allowAnyProtocol = constraint.allowedProtocols.contains("*")
				val allowedTags = if (allowAnyTag) emptySet() else constraint.allowedTags
				val attrsPerTag = if (allowAnyAttr) emptyMap() else parseAllowedAttributes(constraint.allowedAttrs)
				val protocolsPerTagAttr =
					if (allowAnyProtocol) emptyMap() else parseAllowedProtocols(constraint.allowedProtocols)
				return HtmlPolicy(
					allowAnyTag = allowAnyTag,
					allowAnyAttr = allowAnyAttr,
					allowAnyProtocol = allowAnyProtocol,
					allowedTags = allowedTags,
					attrsPerTag = attrsPerTag,
					protocolsPerTagAttr = protocolsPerTagAttr,
					safelist = buildDynamicSafelist(allowedTags, attrsPerTag, protocolsPerTagAttr),
				)
			}

			private fun buildDynamicSafelist(
				allowedTags: Set<String>,
				allowedAttributes: Map<String, Set<String>>,
				allowedProtocols: Map<String, Map<String, Set<String>>>,
			): Safelist {
				val safelist = Safelist.none()
				allowedTags.forEach { safelist.addTags(it) }
				allowedAttributes.forEach { (tag, attrs) ->
					safelist.addAttributes(tag, *attrs.toTypedArray())
				}
				allowedProtocols.forEach { (tag, attrProtocols) ->
					attrProtocols.forEach { (attr, protocols) ->
						safelist.addProtocols(tag, attr, *protocols.toTypedArray())
					}
				}
				return safelist
			}

			private fun parseAllowedAttributes(allowedAttrs: Set<String>): Map<String, Set<String>> {
				val map = mutableMapOf<String, MutableSet<String>>()
				allowedAttrs.forEach { entry ->
					val sep = entry.indexOf(':')
					if (sep > 0) {
						map.getOrPut(entry.substring(0, sep)) { mutableSetOf() }
							.add(entry.substring(sep + 1))
					}
				}
				return map
			}

			private fun parseAllowedProtocols(
				allowedProtocols: Set<String>,
			): Map<String, Map<String, Set<String>>> {
				val map = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
				allowedProtocols.forEach { entry ->
					val parts = entry.split(':', limit = 3)
					if (parts.size == 3) {
						val protocols = parts[2].split(',').mapTo(mutableSetOf()) { it.lowercase() }
						map.getOrPut(parts[0]) { mutableMapOf() }[parts[1]] = protocols
					}
				}
				return map
			}
		}
	}
}
