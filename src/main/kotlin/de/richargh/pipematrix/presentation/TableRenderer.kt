package de.richargh.pipematrix.presentation

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import de.richargh.pipematrix.app.exposed.ClassnameGroup
import de.richargh.pipematrix.app.exposed.HeaderMode
import de.richargh.pipematrix.app.exposed.Pipeline
import de.richargh.pipematrix.app.exposed.PipelineStatus
import de.richargh.pipematrix.app.exposed.SortMode
import de.richargh.pipematrix.app.exposed.TestMatrix
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Renders a test matrix as an ASCII table.
 */
object TableRenderer {

    /**
     * Renders a test matrix as a formatted ASCII table string.
     *
     * @param matrix The test matrix to render
     * @param sortMode The sort mode to apply to the classname groups
     * @param headerMode The header display mode (full, min, or none)
     * @return A formatted string representation of the matrix
     */
    fun render(
        matrix: TestMatrix,
        sortMode: SortMode = SortMode.COUNT,
        headerMode: HeaderMode = HeaderMode.FULL
    ): String {
        // Handle empty matrix
        if (matrix.classnameGroups.isEmpty()) {
            return "No test failures found across ${matrix.pipelines.size} pipeline(s)."
        }

        // Get sorted classname groups
        val sortedGroups = matrix.getSortedClassnameGroups(sortMode)

        return table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            // Render headers based on mode
            when (headerMode) {
                HeaderMode.FULL -> {
                    // Header row with pipeline information
                    row {
                        cell("Test Class / Variant") {
                            rowSpan = 5
                            alignment = TextAlignment.MiddleLeft
                        }

                        // Add column for each pipeline
                        for (pipeline in matrix.pipelines) {
                            cell(formatPipelineHeader(pipeline, matrix)) {
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

                    // Third header row with status
                    row {
                        for (pipeline in matrix.pipelines) {
                            cell(formatStatus(pipeline)) {
                                alignment = TextAlignment.MiddleCenter
                            }
                        }
                    }

                    // Fourth header row with timestamp
                    row {
                        for (pipeline in matrix.pipelines) {
                            cell(formatTimestamp(pipeline)) {
                                alignment = TextAlignment.MiddleCenter
                            }
                        }
                    }

                    // Fifth header row with author
                    row {
                        for (pipeline in matrix.pipelines) {
                            cell(formatAuthor(pipeline)) {
                                alignment = TextAlignment.MiddleCenter
                            }
                        }
                    }
                }
                HeaderMode.NONE -> {
                    // No headers at all
                }
            }

            // Data rows - one per classname group (sorted)
            for (group in sortedGroups) {
                row {
                    // Classname cell with all variants listed
                    cell(formatClassnameGroup(group)) {
                        alignment = TextAlignment.TopLeft
                    }

                    // Status cell for each pipeline
                    for (pipeline in matrix.pipelines) {
                        val letters = group.getLetterForPipeline(pipeline.id)
                        cell(if (letters.isNotEmpty()) letters.joinToString(" ") else "") {
                            alignment = TextAlignment.MiddleCenter
                        }
                    }
                }
            }
        }.toString()
    }

    /**
     * Formats a classname group with all its failure variants.
     */
    private fun formatClassnameGroup(group: ClassnameGroup): String {
        val lines = mutableListOf<String>()

        // Add simple classname as header with total count (just the part after the last dot)
        val simpleClassName = group.classname.substringAfterLast('.')
        val totalCount = group.variants.sumOf { it.pipelineIds.size }
        lines.add("$simpleClassName ($totalCount)")

        // Add each variant
        for (variant in group.variants) {
            lines.add("  [${variant.letter}] ${variant.testName}")
            lines.add("      (${variant.jobName})")

            // Add system output if present (truncated)
            if (!variant.systemOutput.isNullOrBlank()) {
                val truncatedOutput = truncateOutput(variant.systemOutput)
                lines.add("      $truncatedOutput")
            }

            // Add stack trace (error) if present (truncated)
            if (!variant.stackTrace.isNullOrBlank()) {
                val truncatedTrace = truncateOutput(variant.stackTrace)
                lines.add("      Error: $truncatedTrace")
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Truncates output to a reasonable length.
     */
    private fun truncateOutput(text: String, maxLength: Int = 80): String {
        val singleLine = text.replace("\n", " ").trim()
        return if (singleLine.length > maxLength) {
            singleLine.take(maxLength - 3) + "..."
        } else {
            singleLine
        }
    }

    /**
     * Formats the pipeline header with ID and status.
     * For overloaded pipelines, shows indication.
     */
    private fun formatPipelineHeader(
        pipeline: Pipeline,
        matrix: TestMatrix
    ): String {
        val id = "#${pipeline.id.value}"
        val isOverloaded = matrix.isOverloaded(pipeline.id)

        return if (isOverloaded) {
            val failureCount = matrix.overloadedPipelineFailures[pipeline.id]?.size ?: 0
            "$id (+$failureCount)"
        } else {
            id
        }
    }

    /**
     * Formats the pipeline status.
     */
    private fun formatStatus(pipeline: Pipeline): String {
        return when (pipeline.status) {
            PipelineStatus.SUCCESS -> "success"
            PipelineStatus.FAILED -> "failed"
            PipelineStatus.RUNNING -> "running"
            PipelineStatus.PENDING -> "pending"
            PipelineStatus.CANCELED -> "canceled"
            PipelineStatus.SKIPPED -> "skipped"
        }
    }

    /**
     * Formats the pipeline timestamp using locale-specific format.
     */
    private fun formatTimestamp(pipeline: Pipeline): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
        return formatter.format(pipeline.createdAt)
    }

    /**
     * Formats the author name (shortened).
     */
    private fun formatAuthor(pipeline: Pipeline): String {
        return pipeline.author?.shortName() ?: "Unknown"
    }

}
