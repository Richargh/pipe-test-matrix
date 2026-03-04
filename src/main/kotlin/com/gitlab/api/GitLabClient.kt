package com.gitlab.api

import de.richargh.pipematrix.app.exposed.BranchName
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
open class GitLabClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val token: String
) {
    /**
     * Verifies that the GitLab URL is reachable and the auth token is valid.
     *
     * @throws GitLabApiException if the URL is unreachable or token is invalid
     */
    open suspend fun verifyConnection() {
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
    open suspend fun verifyProject(projectPath: ProjectPath) {
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

    /**
     * Fetches pipelines for a specific project and branch.
     *
     * @param projectPath The GitLab project path (e.g., "mygroup/myproject")
     * @param branch The branch name to fetch pipelines for
     * @param count The number of pipelines to fetch (default: 10)
     * @return List of pipeline responses from GitLab API
     * @throws GitLabApiException if the API request fails
     */
    open suspend fun fetchPipelines(
        projectPath: ProjectPath,
        branch: BranchName,
        count: Int = 10
    ): List<GitLabPipelineResponse> {
        val encodedPath = encodeProjectPath(projectPath.value)
        val url = "$baseUrl/api/v4/projects/$encodedPath/pipelines"

        return try {
            val response = httpClient.get(url) {
                header("PRIVATE-TOKEN", token)
                parameter("ref", branch.value)
                parameter("per_page", count)
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

    /**
     * Fetches the test report for a specific pipeline.
     *
     * @param projectPath The GitLab project path
     * @param pipelineId The pipeline ID to fetch the test report for
     * @return Test report response from GitLab API
     * @throws GitLabApiException if the API request fails
     */
    open suspend fun fetchTestReport(
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

    /**
     * Encodes a project path for use in URLs.
     * GitLab requires project paths to be URL-encoded (e.g., "group/project" becomes "group%2Fproject").
     */
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
    fun close() {
        httpClient.close()
    }
}

/**
 * Exception thrown when GitLab API operations fail.
 */
class GitLabApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
