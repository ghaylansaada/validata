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

package io.ghaylan.validata.intellij.discovery

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Shared PSI class lookup for discovery (short-name index + capped file-walk fallback).*
 * 
 * @author Ghaylan Saada
 */
internal object PsiClassLookup {
	
	/** Hard cap for the short-name-index miss fallback (avoids O(#project files) on hot paths).	 */
	private const val FILE_WALK_FALLBACK_MAX_FILES: Int = 512
	
	fun findClassByFqName(
		project: Project,
		fqName: String,
		scope: SearchScope,
	): PsiElement? {
		val global = scope as? GlobalSearchScope
			?: GlobalSearchScope.allScope(project)
		JavaPsiFacade.getInstance(project)
			.findClass(fqName, global)
			?.let { return it }
		val shortName = fqName.substringAfterLast('.')
		val pkg = fqName.substringBeforeLast('.', missingDelimiterValue = "")
		for (cls in findClassesByShortName(project, shortName, scope)) {
			when (cls) {
				is KtClass -> if (cls.fqName?.asString() == fqName) return cls
				is PsiClass -> if (cls.qualifiedName == fqName) return cls
			}
		}
		if (pkg.isNotEmpty()) {
			for (cls in findClassesByShortName(project, shortName, scope)) {
				if (cls is KtClass && cls.containingKtFile.packageFqName.asString() == pkg) {
					return cls
				}
			}
		}
		return null
	}
	
	/**
	 * Collects classes with the given simple name from the short-name index, falling back to a
	 * Kotlin file walk only when the index is empty. Fallback is capped and cancellable.	 
	 */
	fun findClassesByShortName(
		project: Project,
		shortName: String,
		scope: SearchScope,
	): List<PsiElement> {
		val global = scope as? GlobalSearchScope
			?: GlobalSearchScope.allScope(project)
		val result = ArrayList<PsiElement>()
		PsiShortNamesCache.getInstance(project)
			.getClassesByName(shortName, global)
			.forEach { result += it }
		if (result.isNotEmpty()) return result
		var scanned = 0
		for (vf in FileTypeIndex.getFiles(KotlinFileType.INSTANCE, global)) {
			ProgressManager.checkCanceled()
			if (scanned++ >= FILE_WALK_FALLBACK_MAX_FILES) break
			val psiFile = PsiManager.getInstance(project)
				.findFile(vf) as? KtFile
				?: continue
			psiFile.declarations.filterIsInstance<KtClass>()
				.forEach { cls ->
					if (cls.name == shortName) result += cls
				}
			if (result.isNotEmpty()) break
		}
		return result
	}
	
	/**
	 * Preference order: exact [preferredPackage] → `io.ghaylan.validata.*` → first hit.
	 */
	fun pickBestClass(
		candidates: List<PsiElement>,
		preferredPackage: String? = null,
	): PsiElement? {
		if (candidates.isEmpty()) return null
		if (candidates.size == 1) return candidates.first()
		
		fun fqOf(element: PsiElement): String? = when (element) {
			is KtClass -> element.fqName?.asString()
			is PsiClass -> element.qualifiedName
			else -> null
		}
		
		fun packageOf(fq: String?): String? = fq?.substringBeforeLast('.', missingDelimiterValue = "")
			?.takeIf { it.isNotEmpty() }
		
		if (!preferredPackage.isNullOrBlank()) {
			val inPkg = candidates.filter { packageOf(fqOf(it)) == preferredPackage }
			if (inPkg.isNotEmpty()) return inPkg.first()
		}
		val validata = candidates.filter { fqOf(it)?.startsWith("io.ghaylan.validata.") == true }
		if (validata.isNotEmpty()) return validata.first()
		
		return candidates.first()
	}
}
