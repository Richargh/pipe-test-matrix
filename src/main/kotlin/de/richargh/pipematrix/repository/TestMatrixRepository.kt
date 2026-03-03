package de.richargh.pipematrix.repository

import com.gitlab.api.GitLabClient
import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse
import de.richargh.pipematrix.domain.*
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
     * @return A TestMatrix containing pipeline and test failure data
     */
    suspend fun fetchTestMatrix(
        projectPath: de.richargh.pipematrix.domain.ProjectPath,
        branch: de.richargh.pipematrix.domain.BranchName,
        pipelineCount: Int = 10,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): de.richargh.pipematrix.domain.TestMatrix {
        // Fetch pipelines
        val pipelineResponses = gitLabClient.fetchPipelines(projectPath, branch, pipelineCount)

        // Transform pipelines to domain model
        val pipelines = pipelineResponses.map { it.toDomain() }

        // Fetch test reports for each pipeline
        val failuresByPipeline = mutableMapOf<de.richargh.pipematrix.domain.PipelineId, List<de.richargh.pipematrix.domain.TestFailure>>()

        for ((index, pipelineResponse) in pipelineResponses.withIndex()) {
            val pipelineId = PipelineId(pipelineResponse.id)
            val testReport = gitLabClient.fetchTestReport(projectPath, pipelineId)

            // Extract failures from test report
            val failures = extractFailures(testReport, pipelineId)
            if (failures.isNotEmpty()) {
                failuresByPipeline[pipelineId] = failures
            }

            // Report progress
            onProgress?.invoke(index + 1, pipelineResponses.size)
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
        pipelineId: PipelineId
    ): List<TestFailure> {
        val failures = mutableListOf<TestFailure>()

        for (testSuite in testReport.testSuites) {
            for (testCase in testSuite.testCases) {
                if (testCase.status == "failed") {
                    failures.add(
                        TestFailure(
                            testName = TestName(testCase.name),
                            pipelineId = pipelineId,
                            message = testCase.systemOutput
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
        author = this.user?.let { user ->
            // Prefer the full name, fall back to username, or use "Unknown"
            val name = user.name ?: user.username ?: "Unknown"
            AuthorName(name)
        }
    )
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
