package com.gitlab.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitLab API response for a pipeline.
 */
@Serializable
data class GitLabPipelineResponse(
    val id: Long,
    val sha: String,
    val ref: String,
    val status: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val user: GitLabUser? = null
)

/**
 * GitLab API response for a user.
 */
@Serializable
data class GitLabUser(
    val id: Long? = null,
    val username: String? = null,
    val name: String? = null,
    val state: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null
)

/**
 * GitLab API response for a test report.
 */
@Serializable
data class GitLabTestReportResponse(
    @SerialName("total_time")
    val totalTime: Double,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("success_count")
    val successCount: Int,
    @SerialName("failed_count")
    val failedCount: Int,
    @SerialName("skipped_count")
    val skippedCount: Int,
    @SerialName("error_count")
    val errorCount: Int,
    @SerialName("test_suites")
    val testSuites: List<GitLabTestSuite>
)

/**
 * Represents a test suite within a test report.
 */
@Serializable
data class GitLabTestSuite(
    val name: String,
    @SerialName("total_time")
    val totalTime: Double,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("success_count")
    val successCount: Int,
    @SerialName("failed_count")
    val failedCount: Int,
    @SerialName("skipped_count")
    val skippedCount: Int,
    @SerialName("error_count")
    val errorCount: Int,
    @SerialName("test_cases")
    val testCases: List<GitLabTestCase>
)

/**
 * Represents an individual test case within a test suite.
 */
@Serializable
data class GitLabTestCase(
    val status: String,
    val name: String,
    val classname: String,
    @SerialName("execution_time")
    val executionTime: Double,
    @SerialName("system_output")
    val systemOutput: String? = null,
    @SerialName("stack_trace")
    val stackTrace: String? = null
)
