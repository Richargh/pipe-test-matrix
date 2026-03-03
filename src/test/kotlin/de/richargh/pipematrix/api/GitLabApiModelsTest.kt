package de.richargh.pipematrix.api

import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GitLabPipelineResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `should deserialize pipeline response`() {
        val jsonString = """
            {
                "id": 12345,
                "sha": "a1b2c3d4e5f6",
                "ref": "main",
                "status": "failed",
                "created_at": "2024-01-15T10:30:00.000Z",
                "updated_at": "2024-01-15T10:35:00.000Z"
            }
        """.trimIndent()

        val response = json.decodeFromString<GitLabPipelineResponse>(jsonString)

        response.id shouldBe 12345
        response.sha shouldBe "a1b2c3d4e5f6"
        response.ref shouldBe "main"
        response.status shouldBe "failed"
        response.createdAt shouldBe "2024-01-15T10:30:00.000Z"
    }

    @Test
    fun `should deserialize list of pipelines`() {
        val jsonString = """
            [
                {
                    "id": 1,
                    "sha": "abc",
                    "ref": "main",
                    "status": "success",
                    "created_at": "2024-01-15T10:30:00.000Z"
                },
                {
                    "id": 2,
                    "sha": "def",
                    "ref": "main",
                    "status": "failed",
                    "created_at": "2024-01-15T10:35:00.000Z"
                }
            ]
        """.trimIndent()

        val responses = json.decodeFromString<List<GitLabPipelineResponse>>(jsonString)

        responses.size shouldBe 2
        responses[0].id shouldBe 1
        responses[1].id shouldBe 2
    }
}

class GitLabTestReportResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `should deserialize test report with failures`() {
        val jsonString = """
            {
                "total_time": 5.4,
                "total_count": 10,
                "success_count": 8,
                "failed_count": 2,
                "skipped_count": 0,
                "error_count": 0,
                "test_suites": [
                    {
                        "name": "AuthenticationTests",
                        "total_time": 2.1,
                        "total_count": 5,
                        "success_count": 4,
                        "failed_count": 1,
                        "skipped_count": 0,
                        "error_count": 0,
                        "test_cases": [
                            {
                                "status": "failed",
                                "name": "test_user_login",
                                "classname": "AuthenticationTests",
                                "execution_time": 0.5,
                                "system_output": "AssertionError: Expected 200 but got 500"
                            },
                            {
                                "status": "success",
                                "name": "test_user_logout",
                                "classname": "AuthenticationTests",
                                "execution_time": 0.3
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val report = json.decodeFromString<GitLabTestReportResponse>(jsonString)

        report.totalCount shouldBe 10
        report.failedCount shouldBe 2
        report.testSuites.size shouldBe 1
        report.testSuites[0].name shouldBe "AuthenticationTests"
        report.testSuites[0].testCases.size shouldBe 2
        report.testSuites[0].testCases[0].status shouldBe "failed"
        report.testSuites[0].testCases[0].name shouldBe "test_user_login"
        report.testSuites[0].testCases[0].systemOutput shouldBe "AssertionError: Expected 200 but got 500"
    }

    @Test
    fun `should deserialize test report with no failures`() {
        val jsonString = """
            {
                "total_time": 3.2,
                "total_count": 5,
                "success_count": 5,
                "failed_count": 0,
                "skipped_count": 0,
                "error_count": 0,
                "test_suites": []
            }
        """.trimIndent()

        val report = json.decodeFromString<GitLabTestReportResponse>(jsonString)

        report.totalCount shouldBe 5
        report.failedCount shouldBe 0
        report.testSuites.size shouldBe 0
    }
}
