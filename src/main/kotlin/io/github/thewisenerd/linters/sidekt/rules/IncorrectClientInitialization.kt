package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty

class IncorrectClientInitialization(config: Config) : Rule(config) {

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = IncorrectClientInitialization::class.java.simpleName,
        severity = Severity.Performance,
        description = "Some issue text.",
        debt = Debt.TEN_MINS
    )

//    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
//        super.visitStringTemplateExpression(expression)
//        val dbg = Debugger.make(ImageQuality::class.java.simpleName, debugStream)
//        val text = expression.text
//        val patternQuality = Regex("https://ud-img.azureedge.net/.*q_75.*")
//        if(patternQuality.containsMatchIn(text)) {
//            dbg.i("Wasteful parameters detected q_75 in string $text")
//            report(
//                CodeSmell(
//                    issue = issue,
//                    entity = Entity.from(expression),
//                    message = "Wasteful parameters q_75 detected. Use q_auto instead. Refer to $cloudinaryRefUrlForQuality."
//                )
//            )
//        }
//    }

//    override fun visitCallExpression(expression: KtCallExpression) {
//        super.visitCallExpression(expression)
//        println("visitCallExpression")
//        println(expression.text)
//        println((expression.calleeExpression as KtNameReferenceExpression).getReferencedNameAsName().identifier)
////        check(expression)
//    }
    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        println("name: ${property.name}")
        println("has Delegate ${property.hasDelegate()}")
        if(property.hasDelegate()) {
            println("delegation: ${((property.delegateExpression as KtCallExpression).calleeExpression as KtNameReferenceExpression).getReferencedNameAsName().identifier}")
        }
        if(property.annotationEntries.size > 0) {
            println("annotations: ${ property.annotationEntries[0].shortName }")
        } else {
            println("no annotations")
        }
        println(property.toString())
    }
}