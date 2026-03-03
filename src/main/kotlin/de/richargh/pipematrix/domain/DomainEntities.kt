package de.richargh.pipematrix.domain

import java.time.Instant

/**
 * Represents the status of a pipeline.
 */
enum class PipelineStatus {
    SUCCESS,
    FAILED,
    CANCELED,
    RUNNING,
    PENDING,
    SKIPPED
}

/**
 * Represents a GitLab pipeline run.
 */
data class Pipeline(
    val id: de.richargh.pipematrix.domain.PipelineId,
    val sha: de.richargh.pipematrix.domain.CommitSha,
    val createdAt: Instant,
    val status: de.richargh.pipematrix.domain.PipelineStatus,
    val author: de.richargh.pipematrix.domain.AuthorName?
)

/**
 * Represents a single test failure.
 */
data class TestFailure(
    val testName: de.richargh.pipematrix.domain.TestName,
    val pipelineId: de.richargh.pipematrix.domain.PipelineId,
    val message: String?
)

/**
 * Represents the complete test matrix.
 *
 * @property pipelines List of pipelines, ordered chronologically (most recent first)
 * @property testFailures Map of test names to the set of pipeline IDs where they failed
 * @property overloadedPipelines Set of pipeline IDs that exceeded the failure threshold
 * @property overloadedPipelineFailures Map of overloaded pipeline IDs to the tests that failed in them
 */
data class TestMatrix(
    val pipelines: List<de.richargh.pipematrix.domain.Pipeline>,
    val testFailures: Map<de.richargh.pipematrix.domain.TestName, Set<de.richargh.pipematrix.domain.PipelineId>>,
    val overloadedPipelines: Set<de.richargh.pipematrix.domain.PipelineId>,
    val overloadedPipelineFailures: Map<de.richargh.pipematrix.domain.PipelineId, Set<de.richargh.pipematrix.domain.TestName>> = emptyMap()
) {
    /**
     * Checks if a pipeline is marked as overloaded (>20 failures).
     */
    fun isOverloaded(pipelineId: de.richargh.pipematrix.domain.PipelineId): Boolean =
        pipelineId in overloadedPipelines

    /**
     * Checks if a specific test failed in a specific pipeline.
     */
    fun didTestFail(testName: de.richargh.pipematrix.domain.TestName, pipelineId: de.richargh.pipematrix.domain.PipelineId): Boolean =
        testFailures[testName]?.contains(pipelineId) ?: false

    /**
     * Checks if a specific test failed in an overloaded pipeline.
     */
    fun didTestFailInOverloadedPipeline(testName: de.richargh.pipematrix.domain.TestName, pipelineId: de.richargh.pipematrix.domain.PipelineId): Boolean =
        overloadedPipelineFailures[pipelineId]?.contains(testName) ?: false

    /**
     * Returns all unique test names that failed across all pipelines.
     */
    fun getAllFailedTests(): List<de.richargh.pipematrix.domain.TestName> =
        testFailures.keys.toList()

    /**
     * Returns all unique test names sorted by failure frequency (most frequent first).
     */
    fun getAllFailedTestsSortedByFrequency(): List<de.richargh.pipematrix.domain.TestName> =
        testFailures.entries
            .sortedByDescending { it.value.size }
            .map { it.key }
}
