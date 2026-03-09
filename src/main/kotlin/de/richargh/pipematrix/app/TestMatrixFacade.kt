package de.richargh.pipematrix.app

import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse
import de.richargh.pipematrix.app.exposed.PipelineClient
import de.richargh.pipematrix.app.exposed.AuthorName
import de.richargh.pipematrix.app.exposed.BranchName
import de.richargh.pipematrix.app.exposed.CommitSha
import de.richargh.pipematrix.app.exposed.FailureThreshold
import de.richargh.pipematrix.app.exposed.IsoDate
import de.richargh.pipematrix.app.exposed.Pipeline
import de.richargh.pipematrix.app.exposed.PipelineId
import de.richargh.pipematrix.app.exposed.PipelineStatus
import de.richargh.pipematrix.app.exposed.ProjectPath
import de.richargh.pipematrix.app.exposed.TestClassnameFilter
import de.richargh.pipematrix.app.exposed.TestFailure
import de.richargh.pipematrix.app.exposed.TestMatrix
import de.richargh.pipematrix.app.hidden.MatrixBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant

class TestMatrixFacade(
    private val pipelineClient: PipelineClient,
    private val failureThreshold: FailureThreshold
) {
    suspend fun fetchTestMatrix(
        projectPath: ProjectPath,
        branch: BranchName,
        pipelineCount: Int? = null,
        dateFrom: IsoDate? = null,
        dateTo: IsoDate? = null,
        classnameFilter: TestClassnameFilter? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
        debugTestNames: Boolean = false
    ): TestMatrix {
        // Fetch pipelines (with optional date range)
        val pipelineResponses = pipelineClient.fetchPipelines(
            projectPath = projectPath,
            branch = branch,
            maxCount = pipelineCount,
            updatedAfter = dateFrom,
            updatedBefore = dateTo
        )

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
                    val testReport = pipelineClient.fetchTestReport(projectPath, pipelineId)

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

        // Build test matrix
        val matrix = MatrixBuilder.build(
            pipelines = pipelines,
            failuresByPipeline = failuresByPipeline,
            threshold = failureThreshold
        )

        // Apply classname filter if provided
        return if (classnameFilter != null) {
            matrix.filterByClassname(classnameFilter)
        } else {
            matrix
        }
    }

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
                            jobName = testSuite.name,
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

private fun GitLabPipelineResponse.toDomain(): Pipeline {
    return Pipeline(
        id = PipelineId(this.id),
        sha = CommitSha(this.sha),
        createdAt = Instant.parse(this.createdAt),
        status = mapPipelineStatus(this.status),
        author = extractAuthorName(this)
    )
}

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
