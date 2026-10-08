/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.discovery.cache

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import io.ghaylan.validata.intellij.model.ConstraintArgMetadataHost
import io.ghaylan.validata.intellij.model.PropertyRefMetadataHost
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-project memoization of classpath discovery for constraint annotations.
 *
 * Caches `@PropertyRef` hosts, `@ConstraintArg` hosts, and `validatedBy` analysis keyed by
 * annotation FQCN. Invalidates **user** FQCNs when IntelliJ’s [PsiModificationTracker] stamp
 * changes; sticky-keeps entries under [LIBRARY_FQCN_PREFIX] across stamp advances (library jars
 * do not change with local PSI edits).
 *
 * Registered as a project-level [Service]; obtain via [getInstance].*
 * 
 * @author Ghaylan Saada
 */
@Service(Service.Level.PROJECT)
internal class ConstraintDiscoveryCache(
	private val project: Project,
) {
	
	private val byAnnotationFqName = ConcurrentHashMap<String, PropertyRefDiscoveryHostEntry>()
	private val constraintArgByFqName = ConcurrentHashMap<String, ConstraintArgDiscoveryHostEntry>()
	private val validatedByFqName = ConcurrentHashMap<String, ValidatedByDiscoveryEntry>()
	
	@Volatile
	private var cachedStamp: Long = -1L
	
	/**
	 * Returns cached `@PropertyRef` hosts for [annotationFqName], computing once per FQCN+stamp
	 * under concurrent highlighting (single-flight via [ConcurrentHashMap.compute]).	 
	 */
	fun hosts(
		annotationFqName: String,
		compute: () -> List<PropertyRefMetadataHost>,
	): List<PropertyRefMetadataHost> {
		val stamp = currentStamp()
		val entry = byAnnotationFqName.compute(annotationFqName) { key, existing ->
			when {
				existing != null && existing.stamp == stamp -> existing
				existing != null && isLibraryFqcn(key) -> PropertyRefDiscoveryHostEntry(stamp, existing.hosts)
				else -> PropertyRefDiscoveryHostEntry(stamp, compute())
			}
		}
			?: error("ConstraintDiscoveryCache.hosts compute returned null")
		return if (entry.stamp == stamp) entry.hosts
		else {
			val hosts = compute()
			byAnnotationFqName[annotationFqName] = PropertyRefDiscoveryHostEntry(currentStamp(), hosts)
			hosts
		}
	}
	
	/**
	 * Returns cached `@ConstraintArg` hosts for [annotationFqName], single-flight per stamp.	 
	 */
	fun constraintArgHosts(
		annotationFqName: String,
		compute: () -> List<ConstraintArgMetadataHost>,
	): List<ConstraintArgMetadataHost> {
		val stamp = currentStamp()
		val entry = constraintArgByFqName.compute(annotationFqName) { key, existing ->
			when {
				existing != null && existing.stamp == stamp -> existing
				existing != null && isLibraryFqcn(key) -> ConstraintArgDiscoveryHostEntry(stamp, existing.hosts)
				else -> ConstraintArgDiscoveryHostEntry(stamp, compute())
			}
		}
			?: error("ConstraintDiscoveryCache.constraintArgHosts compute returned null")
		return if (entry.stamp == stamp) entry.hosts
		else {
			val hosts = compute()
			constraintArgByFqName[annotationFqName] = ConstraintArgDiscoveryHostEntry(currentStamp(), hosts)
			hosts
		}
	}
	
	/**
	 * Returns cached `validatedBy` analysis for [annotationFqName], single-flight per stamp.	 
	 */
	fun validatedBy(
		annotationFqName: String,
		compute: () -> ConstraintValidatedByResult,
	): ConstraintValidatedByResult {
		val stamp = currentStamp()
		val entry = validatedByFqName.compute(annotationFqName) { key, existing ->
			when {
				existing != null && existing.stamp == stamp -> existing
				existing != null && isLibraryFqcn(key) -> ValidatedByDiscoveryEntry(stamp, existing.result)
				else -> ValidatedByDiscoveryEntry(stamp, compute())
			}
		}
			?: error("ConstraintDiscoveryCache.validatedBy compute returned null")
		return if (entry.stamp == stamp) entry.result
		else {
			val result = compute()
			validatedByFqName[annotationFqName] = ValidatedByDiscoveryEntry(currentStamp(), result)
			result
		}
	}
	
	private fun currentStamp(): Long {
		val stamp = PsiModificationTracker.getInstance(project).modificationCount
		if (cachedStamp != stamp) {
			synchronized(this) {
				if (cachedStamp != stamp) {
					clearUserEntries(byAnnotationFqName)
					clearUserEntries(constraintArgByFqName)
					clearUserEntries(validatedByFqName)
					cachedStamp = stamp
				}
			}
		}
		return stamp
	}
	
	private fun <V> clearUserEntries(map: ConcurrentHashMap<String, V>) {
		map.keys.removeIf { !isLibraryFqcn(it) }
	}
	
	companion object {
		
		/**
		 * Library annotation FQCNs kept across PSI stamp clears (jars / published core).
		 */
		const val LIBRARY_FQCN_PREFIX: String = "io.ghaylan.validata."
		
		fun isLibraryFqcn(fqcn: String): Boolean = fqcn.startsWith(LIBRARY_FQCN_PREFIX)
		
		fun getInstance(project: Project): ConstraintDiscoveryCache = project.getService(ConstraintDiscoveryCache::class.java)
	}
}
