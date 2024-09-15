package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

/**
 * This rule enforces the use of the `@JsonIgnoreProperties` annotation on all data classes within the codebase.
 *
 * Severity: Maintainability
 * Debt: 5min
 */
class JsonIgnorePropertiesOnDataClass(config: Config) : Rule(config) {

    companion object {
        val JSON_IGNORE_ANNOTATION = "JsonIgnoreProperties"
    }

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = JsonIgnorePropertiesOnDataClass::class.java.simpleName,
        severity = Severity.Maintainability,
        description = "JsonIgnoreProperties is not annotated on the data class",
        debt = Debt.FIVE_MINS,
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val dbg = Debugger.make(JsonIgnorePropertiesOnDataClass::class.java.simpleName, debugStream)
        // Check if the class is a data class
        if (klass.isData()) {
            // Check if @JsonIgnoreProperties annotation exists
            val hasJsonIgnoreProperties = klass.annotationEntries.any { annotation ->
                annotation.shortName?.asString() == JSON_IGNORE_ANNOTATION
            }

            // Report if annotation is missing
            if (!hasJsonIgnoreProperties) {
                dbg.i("JsonIgnoreProperties annotation is missed for data class ${kclass.name}")
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(klass),
                        message = "Data class '${klass.name}' should be annotated with @JsonIgnoreProperties(ignoreUnknown = true)",
                    ),
                )
            } else {
                dbg.i("JsonIgnoreProperties annotation is present for data class ${kclass.name}")
            }
        }
    }
}
