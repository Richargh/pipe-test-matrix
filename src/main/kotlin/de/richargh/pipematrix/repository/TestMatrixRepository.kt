package de.richargh.pipematrix.repository

import com.gitlab.api.GitLabClient
import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse
import de.richargh.pipematrix.domain.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant

/**
 * Repository for fetching and building test matrices from GitLab.
 *
 * @property gitLabClient The GitLab API client to use for fetching data
 * @property failureThreshold The threshold for marking pipelines as overloaded
 */
class TestMatrixRepository(
    private val gitLabClient: GitLabClient,
    private val failureThreshold: de.richargh.pipematrix.domain.FailureThreshold
) {
    /**
     * Fetches test matrix data for a specific project and branch.
     *
     * @param projectPath The GitLab project path
     * @param branch The branch name to fetch data for
     * @param pipelineCount The number of pipelines to fetch
     * @param onProgress Optional callback invoked after processing each pipeline (current, total)
     * @param debugTestNames If true, prints raw test case data to help identify available fields
     * @return A TestMatrix containing pipeline and test failure data
     */
    suspend fun fetchTestMatrix(
        projectPath: de.richargh.pipematrix.domain.ProjectPath,
        branch: de.richargh.pipematrix.domain.BranchName,
        pipelineCount: Int = 10,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
        debugTestNames: Boolean = false
    ): de.richargh.pipematrix.domain.TestMatrix {
        // Fetch pipelines
        val pipelineResponses = gitLabClient.fetchPipelines(projectPath, branch, pipelineCount)

        // Debug pipeline user data if requested
        if (debugTestNames && pipelineResponses.isNotEmpty()) {
            val firstPipeline = pipelineResponses.first()
            println("\n=== DEBUG: Pipeline Structure ===")
            println("id: ${firstPipeline.id}")
            println("sha: ${firstPipeline.sha}")
            println("status: ${firstPipeline.status}")
            println("user: ${firstPipeline.user}")
            println("user.name: ${firstPipeline.user?.name}")
            println("user.username: ${firstPipeline.user?.username}")
            println("username: ${firstPipeline.username}")
            println("web_url: ${firstPipeline.webUrl}")
            println("source: ${firstPipeline.source}")
            println("==================================\n")
        }

        // Transform pipelines to domain model
        val pipelines = pipelineResponses.map { it.toDomain() }

        // Fetch test reports for each pipeline in parallel
        val failuresByPipeline = coroutineScope {
            pipelineResponses.mapIndexed { index, pipelineResponse ->
                async {
                    val pipelineId = PipelineId(pipelineResponse.id)
                    val testReport = gitLabClient.fetchTestReport(projectPath, pipelineId)

                    // Extract failures from test report
                    val failures = extractFailures(testReport, pipelineId, debugTestNames && index == 0)

                    // Report progress
                    onProgress?.invoke(index + 1, pipelineResponses.size)

                    // Return pair of pipelineId and failures
                    pipelineId to failures
                }
            }.awaitAll()
                .filter { (_, failures) -> failures.isNotEmpty() }
                .toMap()
        }

        // Build and return test matrix
        return MatrixBuilder.build(
            pipelines = pipelines,
            failuresByPipeline = failuresByPipeline,
            threshold = failureThreshold
        )
    }

    /**
     * Extracts test failures from a GitLab test report.
     */
    private fun extractFailures(
        testReport: GitLabTestReportResponse,
        pipelineId: PipelineId,
        debug: Boolean = false
    ): List<TestFailure> {
        val failures = mutableListOf<TestFailure>()
        var debugPrinted = false

        for (testSuite in testReport.testSuites) {
            for (testCase in testSuite.testCases) {
                if (testCase.status == "failed") {
                    // Print debug info for first failed test
                    if (debug && !debugPrinted) {
                        println("\n=== DEBUG: Raw Test Case Structure ===")
                        println("status: ${testCase.status}")
                        println("name: ${testCase.name}")
                        println("classname: ${testCase.classname}")
                        println("executionTime: ${testCase.executionTime}")
                        println("file: ${testCase.file}")
                        println("line: ${testCase.line}")
                        println("systemOutput: ${testCase.systemOutput?.take(100)}")
                        println("stackTrace: ${testCase.stackTrace?.take(200)}")
                        println("======================================\n")
                        debugPrinted = true
                    }

                    failures.add(
                        TestFailure(
                            classname = testCase.classname,
                            testName = testCase.name,
                            pipelineId = pipelineId,
                            systemOutput = testCase.systemOutput,
                            stackTrace = testCase.stackTrace
                        )
                    )
                }
            }
        }

        return failures
    }

}

/**
 * Extension function to convert GitLabPipelineResponse to domain Pipeline.
 */
private fun GitLabPipelineResponse.toDomain(): Pipeline {
    return Pipeline(
        id = PipelineId(this.id),
        sha = CommitSha(this.sha),
        createdAt = Instant.parse(this.createdAt),
        status = mapPipelineStatus(this.status),
        author = extractAuthorName(this)
    )
}

/**
 * Extracts author name from pipeline response, trying multiple sources.
 */
private fun extractAuthorName(pipeline: GitLabPipelineResponse): AuthorName? {
    // Try user object first (if present)
    val userName = pipeline.user?.name ?: pipeline.user?.username
    if (userName != null) {
        return AuthorName(userName)
    }

    // Try username field at pipeline level
    if (pipeline.username != null) {
        return AuthorName(pipeline.username)
    }

    // Try extracting from web_url (e.g., "https://gitlab.com/username")
    if (pipeline.webUrl != null) {
        val username = pipeline.webUrl.substringAfterLast('/').takeIf { it.isNotBlank() }
        if (username != null) {
            return AuthorName(username)
        }
    }

    // Try source field for automated pipelines
    if (pipeline.source != null) {
        return AuthorName(pipeline.source)
    }

    // No author information available
    return null
}

/**
 * Maps GitLab pipeline status string to domain PipelineStatus enum.
 */
private fun mapPipelineStatus(status: String): PipelineStatus {
    return when (status.lowercase()) {
        "success" -> PipelineStatus.SUCCESS
        "failed" -> PipelineStatus.FAILED
        "canceled", "cancelled" -> PipelineStatus.CANCELED
        "running" -> PipelineStatus.RUNNING
        "pending" -> PipelineStatus.PENDING
        "skipped" -> PipelineStatus.SKIPPED
        else -> PipelineStatus.FAILED  // Default to FAILED for unknown statuses
    }
}
