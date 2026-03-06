package de.richargh.pipematrix.infrastructure

import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse
import de.richargh.pipematrix.app.exposed.BranchName
import de.richargh.pipematrix.app.exposed.IsoDate
import de.richargh.pipematrix.app.exposed.PipelineClient
import de.richargh.pipematrix.app.exposed.PipelineId
import de.richargh.pipematrix.app.exposed.ProjectPath
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Client for interacting with the GitLab API.
 *
 * @property httpClient The HTTP client to use for requests
 * @property baseUrl The base URL of the GitLab instance (e.g., "https://gitlab.com")
 * @property token The GitLab personal access token for authentication
 */
class GitLabClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val token: String
) : PipelineClient {

    override suspend fun verifyConnection() {
        val url = "$baseUrl/api/v4/user"

        try {
            val response = httpClient.get(url) {
                header("PRIVATE-TOKEN", token)
            }

            // Check for error status codes
            if (!response.status.isSuccess()) {
                throw createExceptionFromStatus(response.status)
            }
        } catch (e: GitLabApiException) {
            throw e
        } catch (e: Exception) {
            throw GitLabApiException(
                "Cannot reach GitLab at $baseUrl: ${e.message}",
                e
            )
        }
    }

    /**
     * Verifies that the specified project exists and is accessible.
     *
     * @param projectPath The GitLab project path to verify
     * @throws GitLabApiException if the project doesn't exist or isn't accessible
     */
    override suspend fun verifyProject(projectPath: ProjectPath) {
        val encodedPath = encodeProjectPath(projectPath.value)
        val url = "$baseUrl/api/v4/projects/$encodedPath"

        try {
            val response = httpClient.get(url) {
                header("PRIVATE-TOKEN", token)
            }

            // Check for error status codes
            if (!response.status.isSuccess()) {
                throw createExceptionFromStatus(response.status)
            }
        } catch (e: GitLabApiException) {
            throw e
        } catch (e: Exception) {
            throw GitLabApiException(
                "Cannot verify project $projectPath: ${e.message}",
                e
            )
        }
    }

    override suspend fun fetchPipelines(
        projectPath: ProjectPath,
        branch: BranchName,
        maxCount: Int?,
        updatedAfter: IsoDate?,
        updatedBefore: IsoDate?
    ): List<GitLabPipelineResponse> {
        val encodedPath = encodeProjectPath(projectPath.value)
        val url = "$baseUrl/api/v4/projects/$encodedPath/pipelines"

        return try {
            val response = httpClient.get(url) {
                header("PRIVATE-TOKEN", token)
                parameter("ref", branch.value)

                var maxPipelines = maxCount ?: 100
                if (updatedAfter != null || updatedBefore != null) {
                    updatedAfter?.let { parameter("updated_after", it.toGitLabApiFormatStartOfDay()) }
                    updatedBefore?.let { parameter("updated_before", it.toGitLabApiFormatEndOfDay()) }
                } else {
                    // if no date range is set, we have to constrain how many pipes we fetch
                    maxPipelines = 10
                }
                if(maxPipelines != null) {
                    parameter("per_page", maxCount)
                }
            }

            // Check for error status codes
            if (!response.status.isSuccess()) {
                throw createExceptionFromStatus(response.status)
            }

            response.body()
        } catch (e: GitLabApiException) {
            throw e
        } catch (e: Exception) {
            throw GitLabApiException(
                "Network error while connecting to GitLab: ${e.message}",
                e
            )
        }
    }

    override suspend fun fetchTestReport(
        projectPath: ProjectPath,
        pipelineId: PipelineId
    ): GitLabTestReportResponse {
        val encodedPath = encodeProjectPath(projectPath.value)
        val url = "$baseUrl/api/v4/projects/$encodedPath/pipelines/${pipelineId.value}/test_report"

        return try {
            val response = httpClient.get(url) {
                header("PRIVATE-TOKEN", token)
            }

            // Check for error status codes
            if (!response.status.isSuccess()) {
                throw createExceptionFromStatus(response.status)
            }

            response.body()
        } catch (e: GitLabApiException) {
            throw e
        } catch (e: Exception) {
            throw GitLabApiException(
                "Network error while connecting to GitLab: ${e.message}",
                e
            )
        }
    }

    private fun encodeProjectPath(path: String): String {
        return path.replace("/", "%2F")
    }

    /**
     * Creates a GitLabApiException based on HTTP status code.
     */
    private fun createExceptionFromStatus(status: HttpStatusCode): GitLabApiException {
        return when (status) {
            HttpStatusCode.Unauthorized ->
                GitLabApiException("GitLab API authentication failed. Please check your token.")
            HttpStatusCode.NotFound ->
                GitLabApiException("GitLab resource not found. Please check project path and permissions.")
            HttpStatusCode.TooManyRequests ->
                GitLabApiException("GitLab API rate limit exceeded. Please try again later.")
            else ->
                GitLabApiException("GitLab API request failed: $status")
        }
    }

    /**
     * Closes the HTTP client. Should be called when done using the client.
     */
    override fun close() {
        httpClient.close()
    }
}

/**
 * Exception thrown when GitLab API operations fail.
 */
class GitLabApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
