package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * This rule enforces the use of the `@JsonIgnoreProperties` annotation on all data classes within the codebase.
 * Data classes must be annotated with `@JsonIgnoreProperties(ignoreUnknown = true)` to ensure compatibility with JSON deserialization.
 *
 * **Exclusions**: Packages listed in the `excludedPackages` configuration are excluded from this check.
 *
 * **Severity**: Maintainability
 * **Debt**: 5 minutes
 *
 * **Usage**:
 * ```yaml
 * JsonIgnorePropertiesOnDataClass:
 *   active: true
 *   excludes: "com.example.excluded, another.package"
 * ```
 */

class JsonIgnorePropertiesOnDataClass(config: Config) : Rule(config) {

    companion object {
        private const val JSON_IGNORE_ANNOTATION = "JsonIgnoreProperties"
        private val JSON_IGNORE_ANNOTATION_FQ = FqName("com.fasterxml.jackson.annotation.JsonIgnoreProperties")
        private val JSON_IGNORE_ANNOTATION_PACKAGE_FQ = JSON_IGNORE_ANNOTATION_FQ.parent()
        private const val IGNORE_UNKNOWN = "ignoreUnknown"
    }

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    private fun readConfig(key: String, initial: Set<String>? = null): Set<String> {
        val result = initial?.toMutableSet() ?: mutableSetOf()
        valueOrNull<ArrayList<String>>(key)?.let {
            result.addAll(it)
        }
        return result
    }

    private val excludedPackages: Set<String> by lazy {
        readConfig("excludes")
    }

    override val issue: Issue = Issue(
        id = JsonIgnorePropertiesOnDataClass::class.java.simpleName,
        severity = Severity.Maintainability,
        description = "JsonIgnoreProperties(ignoreUnknown = true) is not annotated on the data class",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(kclass: KtClass) {
        super.visitClass(kclass)
        val dbg = Debugger.make(JsonIgnorePropertiesOnDataClass::class.java.simpleName, debugStream)

        val packageName = kclass.containingKtFile.packageFqName.asString()

        // Check if the current class's package is in the excluded list
        if (excludedPackages.any { packageName.startsWith(it) }) {
            dbg.i("Package $packageName is excluded under JsonIgnoreProperties check")
            return
        }

        // Check if the class is a data class
        if (kclass.isData()) {
            // Check if @JsonIgnoreProperties(ignoreUnknown = true) annotation exists
            val hasJsonIgnoreProperties = kclass.annotationEntries.any { annotation ->
                if (annotation.shortName?.identifier == JSON_IGNORE_ANNOTATION) {
                    val correctPackageImport = hasDirectImport(
                        kclass.containingKtFile,
                        JSON_IGNORE_ANNOTATION_FQ
                    ) || hasWildCardImport(kclass.containingKtFile, JSON_IGNORE_ANNOTATION_PACKAGE_FQ)
                    if (correctPackageImport) {
                        val ignoreUnknown = annotation.valueArguments.find {
                            it.getArgumentName()?.asName?.identifier == IGNORE_UNKNOWN
                        }

                        // assuming no compilation errors, getArgumentExpression() would be a boolean constant
                        ignoreUnknown != null && ignoreUnknown.getArgumentExpression()?.text == "true"
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            // Report if annotation is missing
            if (!hasJsonIgnoreProperties) {
                dbg.i("JsonIgnoreProperties annotation is missed for data class ${kclass.name}")
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(kclass),
                        message = "Data class '${kclass.name}' should be annotated with @JsonIgnoreProperties(ignoreUnknown = true)"
                    )
                )
            } else {
                dbg.i("JsonIgnoreProperties annotation is present for data class ${kclass.name}")
            }
        }
    }
}

private fun hasDirectImport(file: KtFile, fqName: FqName): Boolean {
    return file.importDirectives.any { import ->
        import.importedFqName == fqName
    }
}

// rudimentary, does not handle cases where people alias imports
private fun hasWildCardImport(file: KtFile, parentFqName: FqName): Boolean {
    return file.importDirectives.any { import ->
        import.isAllUnder && import.importPath?.fqName == parentFqName
    }
}