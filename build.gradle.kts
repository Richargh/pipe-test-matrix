plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.serialization") version "1.9.25"
    application
}

group = "de.richargh.pipematrix"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // CLI
    implementation("info.picocli:picocli:4.7.5")

    // Table rendering
    implementation("com.jakewharton.picnic:picnic:0.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.ktor:ktor-client-mock:2.3.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
}

application {
    mainClass.set("de.richargh.pipematrix.MainKt")
}

tasks.named<JavaExec>("run") {
    description = """
        Display a matrix of test failures across recent GitLab CI pipeline runs.

        Shows which tests failed in which pipelines, helping identify flaky tests
        and patterns of failures across multiple pipeline runs.

        Usage:
          ./gradlew run --args="PROJECT_PATH --url GITLAB_URL --token GITLAB_TOKEN [OPTIONS]"

        Arguments:
          PROJECT_PATH              GitLab project path (e.g., 'mygroup/myproject')

        Required Options:
          -u, --url URL            GitLab instance URL (e.g., 'https://gitlab.com')
          -t, --token TOKEN        GitLab access token

        Optional Options:
          -b, --branch BRANCH      Branch name to analyze (default: 'main')
          -c, --count COUNT        Number of recent pipelines to analyze (default: 10)

        Optional Environment Variables:
          FAILURE_THRESHOLD        Failure threshold for overloaded pipelines (default: 20)

        Examples:
          # Analyze last 10 pipelines on main branch (uses defaults)
          ./gradlew run --args="mygroup/myproject --url https://gitlab.com --token glpat-xxx"

          # Analyze last 20 pipelines on develop branch
          ./gradlew run --args="mygroup/myproject --url https://gitlab.com --token glpat-xxx --branch develop --count 20"
    """.trimIndent()

    // Show help when no arguments provided
    args = args ?: listOf("--help")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
    )
}

kotlin {
    jvmToolchain(21)
}
