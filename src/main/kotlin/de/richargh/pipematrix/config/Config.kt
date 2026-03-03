package de.richargh.pipematrix.config

data class Config(
    val failureThreshold: Int = 20
) {
    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): Config {
            // Get optional variables with defaults
            val failureThreshold = env["FAILURE_THRESHOLD"]?.toIntOrNull() ?: 20

            return Config(
                failureThreshold = failureThreshold
            )
        }
    }
}