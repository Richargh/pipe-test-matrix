package de.richargh.pipematrix.domain

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
     * @return A TestMatrix with organized test failure data
     */
    fun build(
        pipelines: List<de.richargh.pipematrix.domain.Pipeline>,
        failuresByPipeline: Map<de.richargh.pipematrix.domain.PipelineId, List<de.richargh.pipematrix.domain.TestFailure>>,
        threshold: de.richargh.pipematrix.domain.FailureThreshold
    ): de.richargh.pipematrix.domain.TestMatrix {
        // Identify overloaded pipelines (those exceeding the threshold)
        val overloadedPipelines = failuresByPipeline
            .filter { (_, failures) -> threshold.isExceeded(failures.size) }
            .keys

        // Build the test failures map: TestName -> Set<PipelineId>
        // Exclude failures from overloaded pipelines to keep the table manageable
        val testFailuresMap = mutableMapOf<de.richargh.pipematrix.domain.TestName, MutableSet<de.richargh.pipematrix.domain.PipelineId>>()

        // Build the overloaded pipeline failures map: PipelineId -> Set<TestName>
        // This tracks which tests failed in overloaded pipelines for display purposes
        val overloadedPipelineFailuresMap = mutableMapOf<de.richargh.pipematrix.domain.PipelineId, MutableSet<de.richargh.pipematrix.domain.TestName>>()

        for ((pipelineId, failures) in failuresByPipeline) {
            // For overloaded pipelines, track their failures separately
            if (pipelineId in overloadedPipelines) {
                val testNames = failures.map { it.testName }.toMutableSet()
                overloadedPipelineFailuresMap[pipelineId] = testNames
                continue
            }

            // For non-overloaded pipelines, add to the main test failures map
            for (failure in failures) {
                testFailuresMap
                    .getOrPut(failure.testName) { mutableSetOf() }
                    .add(pipelineId)
            }
        }

        return TestMatrix(
            pipelines = pipelines,
            testFailures = testFailuresMap,
            overloadedPipelines = overloadedPipelines,
            overloadedPipelineFailures = overloadedPipelineFailuresMap
        )
    }
}
