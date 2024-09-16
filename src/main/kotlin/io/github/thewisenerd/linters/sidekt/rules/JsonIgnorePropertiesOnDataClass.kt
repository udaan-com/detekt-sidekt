package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtClass

/**
 * This rule enforces the use of the `@JsonIgnoreProperties` annotation on all data classes within the codebase.
 *
 * Severity: Maintainability
 * Debt: 5min
 */
class JsonIgnorePropertiesOnDataClass(config: Config) : Rule(config) {

    companion object {
        const val JSON_IGNORE_ANNOTATION = "JsonIgnoreProperties"
        const val IGNORE_UNKNOWN = "ignoreUnknown"
    }

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
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
        // Check if the class is a data class
        if (kclass.isData()) {
            // Check if @JsonIgnoreProperties(ignoreUnknown = true) annotation exists
            val hasJsonIgnoreProperties = kclass.annotationEntries.any { annotation ->
                annotation.shortName?.asString() == JSON_IGNORE_ANNOTATION &&
                    annotation.valueArguments.any {
                        it.getArgumentExpression()?.text == true.toString() &&
                            it.getArgumentName()?.asName.toString() == IGNORE_UNKNOWN
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
