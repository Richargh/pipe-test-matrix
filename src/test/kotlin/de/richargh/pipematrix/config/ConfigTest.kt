package de.richargh.pipematrix.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConfigTest {

    @Test
    fun `should load config with default values when no environment variables set`() {
        val env = emptyMap<String, String>()

        val config = Config.fromEnvironment(env)

        config.failureThreshold shouldBe 20
    }

    @Test
    fun `should allow overriding failure threshold`() {
        val env = mapOf(
            "FAILURE_THRESHOLD" to "30"
        )

        val config = Config.fromEnvironment(env)

        config.failureThreshold shouldBe 30
    }

    @Test
    fun `should load from system environment when no map provided`() {
        // This test just verifies the method can be called with system environment
        val config = Config.fromEnvironment()

        // Should always succeed with defaults
        config.failureThreshold shouldBe 20
    }

    @Test
    fun `should use default when failure threshold is not a valid integer`() {
        val env = mapOf(
            "FAILURE_THRESHOLD" to "not-a-number"
        )

        val config = Config.fromEnvironment(env)

        config.failureThreshold shouldBe 20
    }
}
