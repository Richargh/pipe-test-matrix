package de.richargh.pipematrix.app.hidden

import de.richargh.pipematrix.app.exposed.ClassnameGroup
import de.richargh.pipematrix.app.exposed.FailureThreshold
import de.richargh.pipematrix.app.exposed.FailureVariant
import de.richargh.pipematrix.app.exposed.Pipeline
import de.richargh.pipematrix.app.exposed.PipelineId
import de.richargh.pipematrix.app.exposed.TestFailure
import de.richargh.pipematrix.app.exposed.TestMatrix
import kotlin.collections.iterator

/**
 * Builder for constructing TestMatrix from pipelines and test failures.
 */
object MatrixBuilder {
    /**
     * Builds a TestMatrix from pipelines and their associated test failures.
     *
     * @param pipelines List of pipelines to include in the matrix
     * @param failuresByPipeline Map of pipeline IDs to their test failures
     * @param threshold The failure threshold for marking pipelines as overloaded
     * @return A TestMatrix with organized test failure data grouped by classname
     */
    fun build(
        pipelines: List<Pipeline>,
        failuresByPipeline: Map<PipelineId, List<TestFailure>>,
        threshold: FailureThreshold
    ): TestMatrix {
        // Identify overloaded pipelines (those exceeding the threshold)
        val overloadedPipelines = failuresByPipeline
            .filter { (_, failures) -> threshold.isExceeded(failures.size) }
            .keys

        // Collect all failures, separating overloaded from normal pipelines
        val normalFailures = mutableListOf<TestFailure>()
        val overloadedFailures = mutableListOf<TestFailure>()

        for ((pipelineId, failures) in failuresByPipeline) {
            if (pipelineId in overloadedPipelines) {
                overloadedFailures.addAll(failures)
            } else {
                normalFailures.addAll(failures)
            }
        }

        // Build classname groups from normal pipeline failures
        val classnameGroups = buildClassnameGroups(normalFailures)

        // Build overloaded pipeline failures grouped by pipeline
        val overloadedPipelineFailuresMap = buildOverloadedFailures(overloadedFailures, overloadedPipelines)

        return TestMatrix(
            pipelines = pipelines,
            classnameGroups = classnameGroups,
            overloadedPipelines = overloadedPipelines,
            overloadedPipelineFailures = overloadedPipelineFailuresMap,
            totalPipelinesAnalyzed = pipelines.size,
            analyzedDateRange = if (pipelines.isNotEmpty()) {
                pipelines.minOf { it.createdAt } to pipelines.maxOf { it.createdAt }
            } else null
        )
    }

    /**
     * Builds classname groups with failure variants and letter assignments.
     */
    private fun buildClassnameGroups(failures: List<TestFailure>): List<ClassnameGroup> {
        // Group failures by classname
        val failuresByClassname = failures.groupBy { it.classname }

        // Build classname groups
        return failuresByClassname.map { (classname, classnameFailures) ->
            // Create unique variants based on (testName, systemOutput, stackTrace)
            val variantMap = mutableMapOf<VariantKey, MutableSet<PipelineId>>()

            for (failure in classnameFailures) {
                val key = VariantKey(failure.testName, failure.systemOutput, failure.stackTrace)
                variantMap.getOrPut(key) { mutableSetOf() }.add(failure.pipelineId)
            }

            // Sort variants by test name for consistent ordering
            val sortedVariants = variantMap.entries.sortedBy { it.key.testName }

            // Assign letters A, B, C...
            val variants = sortedVariants.mapIndexed { index, (key, pipelineIds) ->
                FailureVariant(
                    letter = ('A' + index).toString(),
                    testName = key.testName,
                    systemOutput = key.systemOutput,
                    stackTrace = key.stackTrace,
                    pipelineIds = pipelineIds
                )
            }

            ClassnameGroup(classname, variants)
        }.sortedBy { it.classname }  // Sort groups by classname
    }

    /**
     * Builds overloaded pipeline failures map.
     */
    private fun buildOverloadedFailures(
        failures: List<TestFailure>,
        overloadedPipelines: Set<PipelineId>
    ): Map<PipelineId, List<FailureVariant>> {
        val result = mutableMapOf<PipelineId, List<FailureVariant>>()

        for (pipelineId in overloadedPipelines) {
            val pipelineFailures = failures.filter { it.pipelineId == pipelineId }

            // Create variants for this pipeline
            val variantMap = mutableMapOf<VariantKey, MutableSet<PipelineId>>()

            for (failure in pipelineFailures) {
                val key = VariantKey(failure.testName, failure.systemOutput, failure.stackTrace)
                variantMap.getOrPut(key) { mutableSetOf() }.add(failure.pipelineId)
            }

            val sortedVariants = variantMap.entries.sortedBy { it.key.testName }

            val variants = sortedVariants.mapIndexed { index, (key, pipelineIds) ->
                FailureVariant(
                    letter = ('A' + index).toString(),
                    testName = key.testName,
                    systemOutput = key.systemOutput,
                    stackTrace = key.stackTrace,
                    pipelineIds = pipelineIds
                )
            }

            result[pipelineId] = variants
        }

        return result
    }

    /**
     * Key for identifying unique failure variants.
     */
    private data class VariantKey(
        val testName: String,
        val systemOutput: String?,
        val stackTrace: String?
    )
}