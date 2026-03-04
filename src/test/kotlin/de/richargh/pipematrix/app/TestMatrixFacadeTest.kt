package de.richargh.pipematrix.app

import de.richargh.pipematrix.infrastructure.GitLabApiException
import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestCase
import com.gitlab.api.GitLabTestReportResponse
import com.gitlab.api.GitLabTestSuite
import com.gitlab.api.GitLabUser
import de.richargh.pipematrix.app.exposed.BranchName
import de.richargh.pipematrix.app.exposed.FailureThreshold
import de.richargh.pipematrix.app.exposed.PipelineClient
import de.richargh.pipematrix.app.exposed.PipelineId
import de.richargh.pipematrix.app.exposed.PipelineStatus
import de.richargh.pipematrix.app.exposed.ProjectPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FakeGitLabClient(
    val pipelinesToReturn: List<GitLabPipelineResponse> = emptyList(),
    val testReportsToReturn: Map<Long, GitLabTestReportResponse> = emptyMap(),
    val exceptionToThrow: Exception? = null
) : PipelineClient {


    override suspend fun verifyConnection() {
        TODO("Not yet implemented")
    }

    override suspend fun verifyProject(projectPath: ProjectPath) {
        TODO("Not yet implemented")
    }

    override suspend fun fetchPipelines(
        projectPath: ProjectPath,
        branch: BranchName,
        count: Int
    ): List<GitLabPipelineResponse> {
        if (exceptionToThrow != null) {
            throw exceptionToThrow
        }
        return pipelinesToReturn
    }

    override suspend fun fetchTestReport(
        projectPath: ProjectPath,
        pipelineId: PipelineId
    ): GitLabTestReportResponse {
        if (exceptionToThrow != null) {
            throw exceptionToThrow
        }
        return testReportsToReturn[pipelineId.value]
            ?: GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList())
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

class TestMatrixFacadeTest {

    @Test
    fun `should fetch and build test matrix successfully`() = runTest {
        // Given
        val pipelinesToReturn = listOf(
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

        val testReportsToReturn = mapOf(
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
        val fakeClient = FakeGitLabClient(pipelinesToReturn, testReportsToReturn)
        val testee = TestMatrixFacade(fakeClient, FailureThreshold(20))

        // When
        val matrix = testee.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Then
        matrix.pipelines.size shouldBe 2
        matrix.pipelines[0].id.value shouldBe 1
        matrix.pipelines[0].sha.value shouldBe "abc123"
        matrix.pipelines[0].status shouldBe PipelineStatus.FAILED
        matrix.pipelines[1].id.value shouldBe 2

        matrix.classnameGroups.size shouldBe 1
        val group = matrix.classnameGroups.first()
        group.classname shouldBe "AuthTests"
        group.variants.size shouldBe 2
        group.variants[0].testName shouldBe "test_login"
        group.variants[1].testName shouldBe "test_logout"
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should handle pipeline with more than 20 failures`() = runTest {
        val pipelinesToReturn = listOf(
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

        val testReportsToReturn = mapOf(
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
        val fakeClient = FakeGitLabClient(pipelinesToReturn, testReportsToReturn)
        val repository = TestMatrixFacade(fakeClient, FailureThreshold(20))


        // When
        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Then
        matrix.overloadedPipelines shouldBe setOf(PipelineId(1))
        // Overloaded pipelines don't appear in the main classname groups
        matrix.classnameGroups.size shouldBe 0
        // But they do appear in overloadedPipelineFailures
        matrix.overloadedPipelineFailures[PipelineId(1)]?.size shouldBe 25
    }

    @Test
    fun `should propagate GitLab API exceptions`() = runTest {
        // Given
        val exceptionToThrow = GitLabApiException("Authentication failed")
        val fakeClient = FakeGitLabClient(exceptionToThrow = exceptionToThrow)
        val repository = TestMatrixFacade(fakeClient, FailureThreshold(20))

        // When & Then
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
        // Given
        val pipelinesToReturn = listOf(
            GitLabPipelineResponse(
                id = 1,
                sha = "abc123",
                ref = "main",
                status = "success",
                createdAt = "2024-01-15T10:00:00.000Z"
            )
        )

        val testReportsToReturn = mapOf(
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
        val fakeClient = FakeGitLabClient(pipelinesToReturn, testReportsToReturn)
        val repository = TestMatrixFacade(fakeClient, FailureThreshold(20))

        // When
        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Then
        matrix.pipelines.size shouldBe 1
        matrix.classnameGroups shouldBe emptyList()
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should invoke progress callback for each pipeline`() = runTest {
        val pipelinesToReturn = listOf(
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

        val testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            2L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            3L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList())
        )
        val fakeClient = FakeGitLabClient(pipelinesToReturn, testReportsToReturn)
        val repository = TestMatrixFacade(fakeClient, FailureThreshold(20))
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        // When
        repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10,
            onProgress = { current, total ->
                progressUpdates.add(current to total)
            }
        )

        // Then
        progressUpdates.size shouldBe 3
        progressUpdates[0] shouldBe (1 to 3)
        progressUpdates[1] shouldBe (2 to 3)
        progressUpdates[2] shouldBe (3 to 3)
    }

    @Test
    fun `should map author name from user data`() = runTest {
        // Given
        val pipelinesToReturn = listOf(
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
        val testReportsToReturn = mapOf(
            1L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            2L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList()),
            3L to GitLabTestReportResponse(0.0, 0, 0, 0, 0, 0, emptyList())
        )
        val fakeClient = FakeGitLabClient(pipelinesToReturn, testReportsToReturn)
        val repository = TestMatrixFacade(fakeClient, FailureThreshold(20))
        // When
        val matrix = repository.fetchTestMatrix(
            ProjectPath("mygroup/myproject"),
            BranchName("main"),
            pipelineCount = 10
        )

        // Then
        matrix.pipelines[0].author?.value shouldBe "John Doe"
        matrix.pipelines[1].author?.value shouldBe "janedoe"
        matrix.pipelines[2].author shouldBe null
    }
}
