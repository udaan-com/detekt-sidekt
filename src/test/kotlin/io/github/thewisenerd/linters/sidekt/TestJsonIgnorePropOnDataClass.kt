package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.JsonIgnorePropertiesOnDataClass
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class TestJsonIgnorePropOnDataClass {

    companion object {
        private val jsonIgnorePropOnDataClass = JsonIgnorePropertiesOnDataClass::class.java.simpleName

        private fun ensureJsonIgnorePropOnDataClassFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) = TestUtils.ensureFindings(jsonIgnorePropOnDataClass, findings, requiredFindings)
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
    private val subject = JsonIgnorePropertiesOnDataClass(testConfig)

    @Test
    fun testDataClasses() {
        val code = TestUtils.readFile("TestJsonIgnorePropertiesOnDataClass.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureJsonIgnorePropOnDataClassFindings(
            findings,
            listOf(
                SourceLocation(9, 1),
                SourceLocation(14, 1),
                SourceLocation(19, 1)
            )
        )
    }
}
