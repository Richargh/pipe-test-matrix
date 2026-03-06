package de.richargh.pipematrix

import de.richargh.pipematrix.infrastructure.GitLabApiException
import de.richargh.pipematrix.infrastructure.GitLabClient
import de.richargh.pipematrix.presentation.TestMatrixCommand
import de.richargh.pipematrix.config.Config.Companion.fromEnvironment
import de.richargh.pipematrix.app.exposed.BranchName
import de.richargh.pipematrix.app.exposed.FailureThreshold
import de.richargh.pipematrix.app.exposed.HeaderMode
import de.richargh.pipematrix.app.exposed.IsoDate
import de.richargh.pipematrix.app.exposed.ProjectPath
import de.richargh.pipematrix.app.exposed.SortMode
import de.richargh.pipematrix.app.exposed.TestClassnameFilter
import de.richargh.pipematrix.presentation.TableRenderer
import de.richargh.pipematrix.app.TestMatrixFacade
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * Main entry point for the GitLab CI Test Matrix application.
 */
fun main(args: Array<String>) {
    val command = TestMatrixCommand()
    val commandLine = CommandLine(command)

    try {
        // Parse arguments
        val parseResult = commandLine.parseArgs(*args)

        // Check if help or version was requested
        if (CommandLine.printHelpIfRequested(parseResult)) {
            return
        }

        // Now run the application with the parsed arguments
        runApplication(command)
    } catch (e: CommandLine.ParameterException) {
        // Handle CLI parsing errors
        commandLine.err.println(e.message)
        commandLine.usage(commandLine.err)
        exitProcess(1)
    } catch (e: Exception) {
        // If anything goes wrong, print error and exit
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

/**
 * Runs the main application logic.
 */
private fun runApplication(command: TestMatrixCommand) {
    runBlocking {
    try {
        // Load configuration from environment
        val config = fromEnvironment()

        // Normalize GitLab URL (remove trailing slash)
        val normalizedUrl = command.gitlabUrl.trim().trimEnd('/')

        // Trim the token
        val token = command.gitlabToken.trim()

        // Get branch and count from CLI (now with defaults)
        val branch = BranchName(command.branch)
        val count = command.count

        // Parse and validate date range
        val dateFrom = command.fromDate?.let { IsoDate.parse(it) }
        val dateTo = command.toDate?.let { IsoDate.parse(it) }

        // Validate date range
        if (dateFrom != null && dateTo != null) {
            if (dateFrom.isAfter(dateTo)) {
                System.err.println("Error: --from date must be before or equal to --to date")
                exitProcess(1)
            }
        }

        // Parse filter
        val filter = command.filterTestClass?.let { TestClassnameFilter(it) }

        // Create HTTP client
        val httpClient = createHttpClient()

        try {
            // Create GitLab client
            val gitLabClient = GitLabClient(
                httpClient = httpClient,
                baseUrl = normalizedUrl,
                token = token
            )

            // Validate connection, token, and project before proceeding
            println("Verifying GitLab connection...")
            gitLabClient.verifyConnection()
            println("✓ Connection and auth token verified")

            println("Verifying project exists...")
            gitLabClient.verifyProject(ProjectPath(command.projectPath))
            println("✓ Project verified")
            println()

            // Create repository
            val repository = TestMatrixFacade(
                pipelineClient = gitLabClient,
                failureThreshold = FailureThreshold(config.failureThreshold)
            )

            // Fetch test matrix
            println("Fetching test matrix for ${command.projectPath} on branch $branch...")
            if (filter != null) {
                println("Filtering for test class: ${filter.value}")
            }
            if (dateFrom != null || dateTo != null) {
                val fromStr = dateFrom?.value?.toString() ?: "earliest"
                val toStr = dateTo?.value?.toString() ?: "latest"
                println("Date range: $fromStr to $toStr")
            }
            println()

            val matrix = repository.fetchTestMatrix(
                projectPath = ProjectPath(command.projectPath),
                branch = branch,
                pipelineCount = count,
                dateFrom = dateFrom,
                dateTo = dateTo,
                classnameFilter = filter,
                onProgress = { current, total ->
                    print("\rAnalyzing pipeline $current/$total...")
                },
                debugTestNames = command.debugTestNames
            )

            // Print summary that overwrites the progress line
            if (matrix.totalPipelinesAnalyzed > 0 && matrix.analyzedDateRange != null) {
                val (minDate, maxDate) = matrix.analyzedDateRange
                val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val summary = "Analyzed ${matrix.totalPipelinesAnalyzed} pipelines between ${minDate.atZone(java.time.ZoneId.of("UTC")).format(dateFormatter)} and ${maxDate.atZone(java.time.ZoneId.of("UTC")).format(dateFormatter)} UTC"
                // Clear the line by overwriting with spaces, then print summary
                print("\r" + " ".repeat(120) + "\r$summary")
            } else {
                // Clear the progress line
                print("\r" + " ".repeat(120) + "\r")
            }
            println() // Move to next line after summary
            println()

            // Parse sort mode
            val sortMode = when (command.sortMode.lowercase()) {
                "name" -> SortMode.NAME
                "count" -> SortMode.COUNT
                else -> {
                    System.err.println("Warning: Invalid sort mode '${command.sortMode}'. Using 'count' as default.")
                    SortMode.COUNT
                }
            }

            // Parse header mode
            val headerMode = when (command.headerMode.lowercase()) {
                "full" -> HeaderMode.FULL
                "none" -> HeaderMode.NONE
                else -> {
                    System.err.println("Warning: Invalid header mode '${command.headerMode}'. Using 'full' as default.")
                    HeaderMode.FULL
                }
            }

            // Render and display table
            val table = TableRenderer.render(matrix, sortMode, headerMode)
            println(table)

            // Display summary
            println()
            println("Summary:")
            println("  Pipelines analyzed: ${matrix.pipelines.size}")
            println("  Test classes with failures: ${matrix.classnameGroups.size}")
            println("  Total failure variants: ${matrix.classnameGroups.sumOf { it.variants.size }}")
            println("  Overloaded pipelines (>20 failures): ${matrix.overloadedPipelines.size}")

        } finally {
            // Always close the HTTP client
            httpClient.close()
        }

    } catch (e: IllegalArgumentException) {
        // Catch validation errors from value types
        System.err.println("Invalid input: ${e.message}")
        exitProcess(1)
    } catch (e: GitLabApiException) {
        System.err.println("GitLab API error: ${e.message}")
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println("Unexpected error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
    }
}

/**
 * Creates and configures an HTTP client for API requests.
 */
private fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            })
        }

        // Configure timeouts
        engine {
            requestTimeout = 60_000  // 60 seconds
        }
    }
}
