package de.richargh.pipematrix.domain

/**
 * Helper functions for creating test data.
 */

fun createTestFailure(
    classname: String,
    testName: String,
    pipelineId: PipelineId,
    systemOutput: String? = null,
    stackTrace: String? = null
) = TestFailure(
    classname = classname,
    testName = testName,
    pipelineId = pipelineId,
    systemOutput = systemOutput,
    stackTrace = stackTrace
)
