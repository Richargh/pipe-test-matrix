package de.richargh.pipematrix.app.exposed

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
 * Sort mode for test matrix display.
 */
enum class SortMode {
    /** Sort by classname alphabetically (descending Z-A) */
    NAME,
    /** Sort by total failure count (descending, most failures first) */
    COUNT
}

/**
 * Represents a GitLab pipeline run.
 */
data class Pipeline(
    val id: PipelineId,
    val sha: CommitSha,
    val createdAt: Instant,
    val status: PipelineStatus,
    val author: AuthorName?
)

/**
 * Represents a single test failure.
 */
data class TestFailure(
    val classname: String,
    val testName: String,
    val pipelineId: PipelineId,
    val systemOutput: String?,
    val stackTrace: String?
)

/**
 * Represents a unique failure variant within a classname group.
 * Multiple pipelines may have the same failure variant.
 */
data class FailureVariant(
    val letter: String,
    val testName: String,
    val systemOutput: String?,
    val stackTrace: String?,
    val pipelineIds: Set<PipelineId>
)

/**
 * Represents all failure variants for a specific test classname.
 */
data class ClassnameGroup(
    val classname: String,
    val variants: List<FailureVariant>
) {
    /**
     * Returns the letter for a specific failure variant in a pipeline, or null if not present.
     */
    fun getLetterForPipeline(pipelineId: PipelineId): List<String> {
        return variants.filter { it.pipelineIds.contains(pipelineId) }.map { it.letter }
    }
}

/**
 * Represents the complete test matrix.
 *
 * @property pipelines List of pipelines, ordered chronologically (most recent first)
 * @property classnameGroups List of classname groups with their failure variants
 * @property overloadedPipelines Set of pipeline IDs that exceeded the failure threshold
 * @property overloadedPipelineFailures Map of overloaded pipeline IDs to their failure variants
 * @property totalPipelinesAnalyzed Total number of pipelines analyzed (before any filtering)
 * @property analyzedDateRange Date range of all analyzed pipelines (min to max), null if no pipelines
 */
data class TestMatrix(
    val pipelines: List<Pipeline>,
    val classnameGroups: List<ClassnameGroup>,
    val overloadedPipelines: Set<PipelineId>,
    val overloadedPipelineFailures: Map<PipelineId, List<FailureVariant>> = emptyMap(),
    val totalPipelinesAnalyzed: Int = pipelines.size,
    val analyzedDateRange: Pair<Instant, Instant>? = if (pipelines.isNotEmpty()) {
        pipelines.minOf { it.createdAt } to pipelines.maxOf { it.createdAt }
    } else null
) {
    /**
     * Checks if a pipeline is marked as overloaded (>20 failures).
     */
    fun isOverloaded(pipelineId: PipelineId): Boolean =
        pipelineId in overloadedPipelines

    /**
     * Returns letters that failed in a specific pipeline for overloaded pipelines.
     */
    fun getOverloadedFailureLetters(pipelineId: PipelineId): List<String> =
        overloadedPipelineFailures[pipelineId]?.map { it.letter } ?: emptyList()

    /**
     * Returns classname groups sorted according to the specified mode.
     */
    fun getSortedClassnameGroups(sortMode: SortMode): List<ClassnameGroup> {
        return when (sortMode) {
            SortMode.NAME -> classnameGroups.sortedByDescending { it.classname }
            SortMode.COUNT -> classnameGroups.sortedByDescending { group ->
                // Total failure count is sum of all pipeline IDs across all variants
                group.variants.sumOf { it.pipelineIds.size }
            }
        }
    }

    /**
     * Returns a filtered copy of this matrix containing only the specified test class.
     * Pipelines with no failures for this class are excluded.
     *
     * Note: Overloaded pipelines are excluded from filtered results as their failure
     * details don't include classname information needed for accurate filtering.
     *
     * @param filter The filter to match test class names (case-insensitive partial match)
     * @return A new TestMatrix with filtered classname groups and pipelines
     */
    fun filterByClassname(filter: TestClassnameFilter): TestMatrix {
        // Filter classname groups to only those matching the filter
        val filteredGroups = classnameGroups.filter { filter.matches(it.classname) }

        // Collect all pipeline IDs that have failures in the filtered groups
        val relevantPipelineIds = filteredGroups
            .flatMap { group -> group.variants.flatMap { it.pipelineIds } }
            .toSet()

        // Filter pipelines to only those with relevant failures
        val filteredPipelines = pipelines.filter { it.id in relevantPipelineIds }

        // Note: Overloaded pipelines are excluded as FailureVariant for overloaded
        // pipelines don't include classname information, making accurate filtering impossible
        return TestMatrix(
            pipelines = filteredPipelines,
            classnameGroups = filteredGroups,
            overloadedPipelines = emptySet(),
            overloadedPipelineFailures = emptyMap(),
            totalPipelinesAnalyzed = this.totalPipelinesAnalyzed,
            analyzedDateRange = this.analyzedDateRange
        )
    }
}
