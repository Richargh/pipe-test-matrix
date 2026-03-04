package de.richargh.pipematrix.presentation

import de.richargh.pipematrix.domain.*
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.time.Instant

class TableRendererTest {

    private fun createTestMatrix(): TestMatrix {
        val pipeline = Pipeline(
            id = PipelineId(12345),
            sha = CommitSha("abc123def456"),
            status = PipelineStatus.SUCCESS,
            createdAt = Instant.parse("2024-01-15T10:30:00Z"),
            author = AuthorName("John Doe")
        )

        val classnameGroup = ClassnameGroup(
            classname = "com.example.TestClass",
            variants = listOf(
                FailureVariant(
                    letter = "A",
                    testName = "testMethod",
                    systemOutput = null,
                    stackTrace = null,
                    pipelineIds = setOf(PipelineId(12345))
                )
            )
        )

        return TestMatrix(
            pipelines = listOf(pipeline),
            classnameGroups = listOf(classnameGroup),
            overloadedPipelines = emptySet()
        )
    }

    @Test
    fun `should render full headers with all information`() {
        val matrix = createTestMatrix()
        val result = TableRenderer.render(matrix, SortMode.COUNT, HeaderMode.FULL)

        // Should contain pipeline ID
        result shouldContain "#12345"
        // Should contain short SHA
        result shouldContain "abc123d"
        // Should contain status
        result shouldContain "success"
        // Should contain author
        result shouldContain "John"
        // Should contain header label
        result shouldContain "Test Class / Variant"
    }

    @Test
    fun `should render no headers when mode is NONE`() {
        val matrix = createTestMatrix()
        val result = TableRenderer.render(matrix, SortMode.COUNT, HeaderMode.NONE)

        // Should NOT contain header label
        result shouldNotContain "Test Class / Variant"
        // Should still contain test class name in data rows (simple name only)
        result shouldContain "TestClass"
    }
}
