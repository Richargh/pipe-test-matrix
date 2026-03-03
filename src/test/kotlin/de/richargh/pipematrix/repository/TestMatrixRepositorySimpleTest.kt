package de.richargh.pipematrix.repository

import com.gitlab.api.GitLabApiException
import com.gitlab.api.GitLabClient
import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestCase
import com.gitlab.api.GitLabTestReportResponse
import com.gitlab.api.GitLabTestSuite
import com.gitlab.api.GitLabUser
import de.richargh.pipematrix.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Fake implementation of GitLabClient for testing.
 */
class FakeGitLabClient : GitLabClient(
    httpClient = HttpClient(),
    baseUrl = "https://fake.gitlab.com",
    token = "fake-token"
) {
    var pipelinesToReturn: List<GitLabPipelineResponse> = emptyList()
    var testReportsToReturn: Map<Long, GitLabTestReportResponse> = emptyMap()
    var shouldThrowException: Boolean = false
    var exceptionToThrow: Exception? = null

    override suspend fun fetchPipelines(
        projectPath: ProjectPath,
        branch: BranchName,
        count: Int
    ): List<GitLabPipelineResponse> {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }
        return pipelinesToReturn
    }

    override suspend fun fetchTestReport(
        projectPath: ProjectPath,
        pipelineId: PipelineId
    ): GitLabTestReportResponse {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }
        return testReportsToReturn[pipelineId.value]
            ?: GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList())
    }
}

class TestMatrixRepositorySimpleTest {

    @Test
    fun `should fetch and build test matrix successfully`() = runTest {
        val fakeClient = FakeGitLabClient()
        val repository = TestMatrixRepository(fakeClient, FailureThreshold(20))

        // Setup fake data
        fakeClient.pipelinesToReturn = listOf(
            GitLabPipelineResponse(
                id = 1,
                sha = "abc123",
                ref = "main",
                status = "failed",
                createdAt = "2024-01-15T10:00:00.000Z"
            ),
            GitLabPipelineResponse(
                id = 2,
                sha = "def456",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T09:00:00.000Z"
            )
        )

        fakeClient.testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(
                totalTime = 5.4,
                totalCount = 5,
                successCount = 3,
                failedCount = 2,
                skippedCount = 0,
                errorCount = 0,
                testSuites = listOf(
                    GitLabTestSuite(
                        name = "AuthTests",
                        totalTime = 2.1,
                        totalCount = 2,
                        successCount = 0,
                        failedCount = 2,
                        skippedCount = 0,
                        errorCount = 0,
                        testCases = listOf(
                            GitLabTestCase(
                                status = "failed",
                                name = "test_login",
                                classname = "AuthTests",
                                executionTime = 0.5,
                                systemOutput = "Login failed"
                            ),
                            GitLabTestCase(
                                status = "failed",
                                name = "test_logout",
                                classname = "AuthTests",
                                executionTime = 0.3,
                                systemOutput = "Logout failed"
                            )
                        )
                    )
                )
            ),
            2L to GitLabTestReportResponse(
                totalTime = 2.0,
                totalCount = 3,
                successCount = 3,
                failedCount = 0,
                skippedCount = 0,
                errorCount = 0,
                testSuites = emptyList()
            )
        )

        // Execute
        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Verify
        matrix.pipelines.size shouldBe 2
        matrix.pipelines[0].id.value shouldBe 1
        matrix.pipelines[0].sha.value shouldBe "abc123"
        matrix.pipelines[0].status shouldBe PipelineStatus.FAILED
        matrix.pipelines[1].id.value shouldBe 2

        matrix.testFailures.size shouldBe 2
        matrix.testFailures[TestName("test_login")] shouldBe setOf(PipelineId(1))
        matrix.testFailures[TestName("test_logout")] shouldBe setOf(PipelineId(1))
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should handle pipeline with more than 20 failures`() = runTest {
        val fakeClient = FakeGitLabClient()
        val repository = TestMatrixRepository(fakeClient, FailureThreshold(20))

        fakeClient.pipelinesToReturn = listOf(
            GitLabPipelineResponse(
                id = 1,
                sha = "abc123",
                ref = "main",
                status = "failed",
                createdAt = "2024-01-15T10:00:00.000Z"
            )
        )

        // Create 25 test failures
        val testCases = (1..25).map { i ->
            GitLabTestCase(
                status = "failed",
                name = "test_$i",
                classname = "Tests",
                executionTime = 0.1,
                systemOutput = "Failed"
            )
        }

        fakeClient.testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(
                totalTime = 10.0,
                totalCount = 25,
                successCount = 0,
                failedCount = 25,
                skippedCount = 0,
                errorCount = 0,
                testSuites = listOf(
                    GitLabTestSuite(
                        name = "Tests",
                        totalTime = 10.0,
                        totalCount = 25,
                        successCount = 0,
                        failedCount = 25,
                        skippedCount = 0,
                        errorCount = 0,
                        testCases = testCases
                    )
                )
            )
        )

        // Execute
        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Verify
        matrix.overloadedPipelines shouldBe setOf(PipelineId(1))
        // Overloaded pipelines don't record individual test failures to keep the table manageable
        matrix.testFailures.size shouldBe 0
    }

    @Test
    fun `should propagate GitLab API exceptions`() = runTest {
        val fakeClient = FakeGitLabClient()
        val repository = TestMatrixRepository(fakeClient, FailureThreshold(20))

        fakeClient.shouldThrowException = true
        fakeClient.exceptionToThrow = GitLabApiException("Authentication failed")

        val exception = shouldThrow<GitLabApiException> {
            repository.fetchTestMatrix(
                ProjectPath("mygroup/myproject"),
                BranchName("main"),
                pipelineCount = 10
            )
        }

        exception.message shouldBe "Authentication failed"
    }

    @Test
    fun `should skip test reports for pipelines with no test data`() = runTest {
        val fakeClient = FakeGitLabClient()
        val repository = TestMatrixRepository(fakeClient, FailureThreshold(20))

        fakeClient.pipelinesToReturn = listOf(
            GitLabPipelineResponse(
                id = 1,
                sha = "abc123",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T10:00:00.000Z"
            )
        )

        fakeClient.testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(
                totalTime = 0.0,
                totalCount = 0,
                successCount = 0,
                failedCount = 0,
                skippedCount = 0,
                errorCount = 0,
                testSuites = emptyList()
            )
        )

        // Execute
        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Verify
        matrix.pipelines.size shouldBe 1
        matrix.testFailures shouldBe emptyMap()
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should invoke progress callback for each pipeline`() = runTest {
        val fakeClient = FakeGitLabClient()
        val repository = TestMatrixRepository(fakeClient, FailureThreshold(20))

        fakeClient.pipelinesToReturn = listOf(
            GitLabPipelineResponse(
                id = 1,
                sha = "abc123",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T10:00:00.000Z"
            ),
            GitLabPipelineResponse(
                id = 2,
                sha = "def456",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T09:00:00.000Z"
            ),
            GitLabPipelineResponse(
                id = 3,
                sha = "ghi789",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T08:00:00.000Z"
            )
        )

        fakeClient.testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            2L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            3L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList())
        )

        // Track progress callbacks
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        // Execute with progress callback
        repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10,
            onProgress = { current, total ->
                progressUpdates.add(current to total)
            }
        )

        // Verify progress callback was invoked correctly
        progressUpdates.size shouldBe 3
        progressUpdates[0] shouldBe (1 to 3)
        progressUpdates[1] shouldBe (2 to 3)
        progressUpdates[2] shouldBe (3 to 3)
    }

    @Test
    fun `should map author name from user data`() = runTest {
        val fakeClient = FakeGitLabClient()
        val repository = TestMatrixRepository(fakeClient, FailureThreshold(20))

        fakeClient.pipelinesToReturn = listOf(
            GitLabPipelineResponse(
                id = 1,
                sha = "abc123",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T10:00:00.000Z",
                user = GitLabUser(
                    id = 123,
                    username = "johndoe",
                    name = "John Doe"
                )
            ),
            GitLabPipelineResponse(
                id = 2,
                sha = "def456",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T09:00:00.000Z",
                user = GitLabUser(
                    id = 456,
                    username = "janedoe",
                    name = null  // Only username available
                )
            ),
            GitLabPipelineResponse(
                id = 3,
                sha = "ghi789",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T08:00:00.000Z",
                user = null  // No user data
            )
        )

        fakeClient.testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            2L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            3L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList())
        )

        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Verify author mapping
        matrix.pipelines[0].author?.value shouldBe "John Doe"
        matrix.pipelines[1].author?.value shouldBe "janedoe"
        matrix.pipelines[2].author shouldBe null
    }
}
