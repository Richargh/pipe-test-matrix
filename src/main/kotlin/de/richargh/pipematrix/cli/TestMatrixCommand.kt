package de.richargh.pipematrix.cli

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

/**
 * CLI command for displaying GitLab CI test failure matrix.
 */
@Command(
    name = "gitlab-test-matrix",
    mixinStandardHelpOptions = true,
    version = ["1.0.0"]
)
class TestMatrixCommand : Runnable {
    /**
     * The GitLab project path (e.g., "mygroup/myproject").
     */
    @Parameters(
        index = "0",
        description = ["GitLab project path (e.g., 'mygroup/myproject')"],
        paramLabel = "PROJECT_PATH"
    )
    lateinit var projectPath: String

    /**
     * The GitLab instance URL.
     */
    @Option(
        names = ["-u", "--url"],
        description = ["GitLab instance URL (e.g., 'https://gitlab.com')"],
        required = true
    )
    lateinit var gitlabUrl: String

    /**
     * The GitLab access token.
     */
    @Option(
        names = ["-t", "--token"],
        description = ["GitLab access token"],
        required = true
    )
    lateinit var gitlabToken: String

    /**
     * The branch name to fetch pipelines for.
     */
    @Option(
        names = ["-b", "--branch"],
        description = ["Branch name to analyze (default: 'main')"],
        defaultValue = "main"
    )
    var branch: String = "main"

    /**
     * The number of recent pipelines to analyze.
     */
    @Option(
        names = ["-c", "--count"],
        description = ["Number of recent pipelines to analyze (default: 10)"],
        defaultValue = "10"
    )
    var count: Int = 10

    override fun run() {
        // The actual execution logic will be in main()
        // This is just for parsing and validation
    }
}
