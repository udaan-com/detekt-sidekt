package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.*

class ResourceOnboardedOnApsec(config: Config) : Rule(config) {

    companion object {
        val APSEC_ANNOTATION = arrayOf("UDErrorMonitoredApi")
        val httpMethodList = arrayOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
    }

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = ResourceOnboardedOnApsec::class.java.simpleName,
        severity = Severity.Performance,
        description = "APSEC annotation is missed resource method",
        debt = Debt.TEN_MINS
    )


    override fun visitNamedFunction(resourceMethod: KtNamedFunction) {
        super.visitNamedFunction(resourceMethod)
        val dbg = Debugger.make(ResourceOnboardedOnApsec::class.java.simpleName, debugStream)

        val hasPathAnnotation = resourceMethod.hasAnnotation("Path")
        val hasAnyOfHttpMethodAnnotation = resourceMethod.hasAnnotation(*httpMethodList)

        if (hasPathAnnotation.not() && hasAnyOfHttpMethodAnnotation.not()) {
            dbg.i("  hasPathAnnotation=false or none of http method annotation available")
            return
        }

        // Check APSEC annotation is missed.
        var hasApsecAnnotation = resourceMethod.hasAnnotation(*APSEC_ANNOTATION)

        if (hasApsecAnnotation.not()) {
            dbg.i("APSEC annotation is missed for resource")
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(resourceMethod),
                    message = "Onboard Resources (${resourceMethod.name} on APSEC))"
                )
            )
        }
    }
}
