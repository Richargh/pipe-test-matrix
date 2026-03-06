package de.richargh.pipematrix.presentation

import picocli.CommandLine

/**
 * CLI command for displaying GitLab CI test failure matrix.
 */
@CommandLine.Command(
    name = "gitlab-test-matrix",
    mixinStandardHelpOptions = true,
    version = ["1.0.0"]
)
class TestMatrixCommand : Runnable {
    /**
     * The GitLab project path (e.g., "mygroup/myproject").
     */
    @CommandLine.Parameters(
        index = "0",
        description = ["GitLab project path (e.g., 'mygroup/myproject')"],
        paramLabel = "PROJECT_PATH"
    )
    lateinit var projectPath: String

    /**
     * The GitLab instance URL.
     */
    @CommandLine.Option(
        names = ["-u", "--url"],
        description = ["GitLab instance URL (e.g., 'https://gitlab.com')"],
        required = true
    )
    lateinit var gitlabUrl: String

    /**
     * The GitLab access token.
     */
    @CommandLine.Option(
        names = ["-t", "--token"],
        description = ["GitLab access token"],
        required = true
    )
    lateinit var gitlabToken: String

    /**
     * The branch name to fetch pipelines for.
     */
    @CommandLine.Option(
        names = ["-b", "--branch"],
        description = ["Branch name to analyze (default: 'main')"],
        defaultValue = "main"
    )
    var branch: String = "main"

    /**
     * The number of recent pipelines to analyze.
     */
    @CommandLine.Option(
        names = ["-c", "--count"],
        description = ["Number of recent pipelines to analyze (default: 10)"],
    )
    var count: Int? = null

    /**
     * Debug mode - prints raw test case data to help identify available fields.
     */
    @CommandLine.Option(
        names = ["--debug-test-names"],
        description = ["Print raw test case data from first failed pipeline to identify field structure"]
    )
    var debugTestNames: Boolean = false

    /**
     * Sort mode for test results.
     */
    @CommandLine.Option(
        names = ["-s", "--sort"],
        description = ["Sort mode: 'name' (alphabetically descending) or 'count' (by failure count descending, default)"],
        defaultValue = "count"
    )
    var sortMode: String = "count"

    /**
     * Header display mode for the test matrix table.
     */
    @CommandLine.Option(
        names = ["--headers"],
        description = ["Header display mode: 'full' (all header info, default), 'none' (no headers)"],
        defaultValue = "full"
    )
    var headerMode: String = "full"

    /**
     * Filter to show only specific test class.
     */
    @CommandLine.Option(
        names = ["--filter-test-class"],
        description = ["Filter to show only specific test class (case-insensitive partial match)"]
    )
    var filterTestClass: String? = null

    /**
     * Start date for pipeline range (ISO-8601 format: YYYY-MM-DD).
     */
    @CommandLine.Option(
        names = ["--from"],
        description = ["Start date for pipeline range (ISO-8601 format: YYYY-MM-DD)"]
    )
    var fromDate: String? = null

    /**
     * End date for pipeline range (ISO-8601 format: YYYY-MM-DD).
     */
    @CommandLine.Option(
        names = ["--to"],
        description = ["End date for pipeline range (ISO-8601 format: YYYY-MM-DD)"]
    )
    var toDate: String? = null

    override fun run() {
        // The actual execution logic will be in main()
        // This is just for parsing and validation
    }
}