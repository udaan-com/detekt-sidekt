package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.IncorrectClientInitialization
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class IncorrectClientInitializationTest {
    companion object {
        private val incorrectClientInitialization = IncorrectClientInitialization::class.java.simpleName

        private fun ensureIncorrectClientInitializationFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) = TestUtils.ensureFindings(incorrectClientInitialization, findings, requiredFindings)
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
    private val subject = IncorrectClientInitialization(testConfig)

    @Test
    fun testIncorrectClientInitialization() {
        val code = TestUtils.readFile("IncorrectClientInitialization.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureIncorrectClientInitializationFindings(
            findings, listOf(
                SourceLocation(10, 17)
            )
        )
    }
}