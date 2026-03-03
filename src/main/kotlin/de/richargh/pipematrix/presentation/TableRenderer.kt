package de.richargh.pipematrix.presentation

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Renders a test matrix as an ASCII table.
 */
object TableRenderer {
    private const val MAX_TEST_NAME_LENGTH = 60

    /**
     * Renders a test matrix as a formatted ASCII table string.
     *
     * @param matrix The test matrix to render
     * @return A formatted string representation of the matrix
     */
    fun render(matrix: de.richargh.pipematrix.domain.TestMatrix): String {
        // Handle empty matrix
        if (matrix.testFailures.isEmpty()) {
            return "No test failures found across ${matrix.pipelines.size} pipeline(s)."
        }

        // Get tests sorted by failure frequency (most frequent first)
        val sortedTests = matrix.getAllFailedTestsSortedByFrequency()

        return table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            // Header row with pipeline information
            row {
                cell("Test Name") {
                    rowSpan = 4
                    alignment = TextAlignment.MiddleLeft
                }

                // Add column for each pipeline
                for (pipeline in matrix.pipelines) {
                    cell(formatPipelineHeader(pipeline, matrix, sortedTests)) {
                        alignment = TextAlignment.MiddleCenter
                    }
                }
            }

            // Second header row with short SHA
            row {
                for (pipeline in matrix.pipelines) {
                    cell(pipeline.sha.shortSha()) {
                        alignment = TextAlignment.MiddleCenter
                    }
                }
            }

            // Third header row with timestamp
            row {
                for (pipeline in matrix.pipelines) {
                    cell(formatTimestamp(pipeline)) {
                        alignment = TextAlignment.MiddleCenter
                    }
                }
            }

            // Fourth header row with author
            row {
                for (pipeline in matrix.pipelines) {
                    cell(formatAuthor(pipeline)) {
                        alignment = TextAlignment.MiddleCenter
                    }
                }
            }

            // Data rows - one per test
            for (testName in sortedTests) {
                row {
                    // Test name cell with failure count
                    val failureCount = getFailureCount(matrix, testName)
                    val testNameWithCount = "($failureCount) ${testName.value}"
                    cell(truncateTestName(testNameWithCount)) {
                        alignment = TextAlignment.MiddleLeft
                    }

                    // Status cell for each pipeline
                    for (pipeline in matrix.pipelines) {
                        val marker = getStatusMarker(matrix, testName, pipeline.id)
                        cell(marker) {
                            alignment = TextAlignment.MiddleCenter
                        }
                    }
                }
            }
        }.toString()
    }

    /**
     * Formats the pipeline header with ID and status.
     * For overloaded pipelines, shows how many tests are hidden.
     */
    private fun formatPipelineHeader(
        pipeline: de.richargh.pipematrix.domain.Pipeline,
        matrix: de.richargh.pipematrix.domain.TestMatrix,
        sortedTests: List<de.richargh.pipematrix.domain.TestName>
    ): String {
        val id = "#${pipeline.id.value}"
        val isOverloaded = matrix.isOverloaded(pipeline.id)

        return if (isOverloaded) {
            val hiddenCount = calculateHiddenTestCount(pipeline.id, matrix, sortedTests)
            if (hiddenCount > 0) {
                "$id (+$hiddenCount)"
            } else {
                "$id (>20)"
            }
        } else {
            id
        }
    }

    /**
     * Calculates how many tests failed in an overloaded pipeline but are not shown in the table.
     */
    private fun calculateHiddenTestCount(
        pipelineId: de.richargh.pipematrix.domain.PipelineId,
        matrix: de.richargh.pipematrix.domain.TestMatrix,
        sortedTests: List<de.richargh.pipematrix.domain.TestName>
    ): Int {
        val allFailuresInPipeline = matrix.overloadedPipelineFailures[pipelineId] ?: emptySet()
        val shownFailuresInPipeline = sortedTests.count { allFailuresInPipeline.contains(it) }
        return allFailuresInPipeline.size - shownFailuresInPipeline
    }

    /**
     * Formats the pipeline timestamp using locale-specific format.
     */
    private fun formatTimestamp(pipeline: de.richargh.pipematrix.domain.Pipeline): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
        return formatter.format(pipeline.createdAt)
    }

    /**
     * Formats the author name (shortened).
     */
    private fun formatAuthor(pipeline: de.richargh.pipematrix.domain.Pipeline): String {
        return pipeline.author?.shortName() ?: "Unknown"
    }

    /**
     * Gets the status marker for a test in a pipeline.
     *
     * @return "✗" if test failed, "" if test passed (even in overloaded pipelines)
     */
    private fun getStatusMarker(
        matrix: de.richargh.pipematrix.domain.TestMatrix,
        testName: de.richargh.pipematrix.domain.TestName,
        pipelineId: de.richargh.pipematrix.domain.PipelineId
    ): String {
        return when {
            // Check if test failed in overloaded pipeline
            matrix.isOverloaded(pipelineId) && matrix.didTestFailInOverloadedPipeline(testName, pipelineId) -> "✗"
            // Check if test failed in normal pipeline
            matrix.didTestFail(testName, pipelineId) -> "✗"
            // Test passed or didn't run
            else -> ""
        }
    }

    /**
     * Gets the total number of times a test failed across all pipelines.
     */
    private fun getFailureCount(matrix: de.richargh.pipematrix.domain.TestMatrix, testName: de.richargh.pipematrix.domain.TestName): Int {
        val regularFailures = matrix.testFailures[testName]?.size ?: 0
        val overloadedFailures = matrix.overloadedPipelineFailures.values.count { it.contains(testName) }
        return regularFailures + overloadedFailures
    }

    /**
     * Truncates test name if it exceeds maximum length.
     */
    private fun truncateTestName(name: String): String {
        return if (name.length > MAX_TEST_NAME_LENGTH) {
            name.take(MAX_TEST_NAME_LENGTH - 3) + "..."
        } else {
            name
        }
    }
}
