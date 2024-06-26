package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.JerseyMainThreadBlockingCall
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class JerseyMainThreadBlockingCallTest {
    companion object {
        private val blockingJerseyMethod = JerseyMainThreadBlockingCall::class.java.simpleName

        private fun ensureJerseyMethodParameterDefaultValueFindings(findings: List<Finding>, requiredFindings: List<SourceLocation>) =
            TestUtils.ensureFindings(blockingJerseyMethod, findings, requiredFindings)
    }

    private val testConfig = object : Config {
        override fun subConfig(key: String): Config = this

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> valueOrNull(key: String): T? {
            return when (key) {
                "active" -> true as? T
                "debug" -> "stderr" as? T
                else -> null
            }
        }
    }
    private val subject = JerseyMainThreadBlockingCall(testConfig)


    @Test
    fun simple06() {
        val code = TestUtils.readFile("simple06.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureJerseyMethodParameterDefaultValueFindings(findings, listOf(
            SourceLocation(4, 5),
            SourceLocation(9, 5),
            SourceLocation(14, 5),
            SourceLocation(19, 5),
            SourceLocation(26, 5),
            SourceLocation(43, 5),
            SourceLocation(48, 5)
        ))
    }

}