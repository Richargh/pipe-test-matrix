package de.richargh.pipematrix.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class MatrixBuilderTest {

    @Test
    fun `should build empty matrix when no pipelines provided`() {
        val matrix = MatrixBuilder.build(
            pipelines = emptyList(),
            failuresByPipeline = emptyMap(),
            threshold = FailureThreshold(20)
        )

        matrix.pipelines shouldBe emptyList()
        matrix.testFailures shouldBe emptyMap()
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should build matrix with no failures`() {
        val pipeline1 = Pipeline(
            id = PipelineId(1),
            sha = CommitSha("abc123"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.SUCCESS,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(2),
            sha = CommitSha("def456"),
            createdAt = Instant.parse("2024-01-15T11:00:00Z"),
            status = PipelineStatus.SUCCESS,
            author = null
        )

        val matrix = MatrixBuilder.build(
            pipelines = listOf(pipeline1, pipeline2),
            failuresByPipeline = emptyMap(),
            threshold = FailureThreshold(20)
        )

        matrix.pipelines shouldBe listOf(pipeline1, pipeline2)
        matrix.testFailures shouldBe emptyMap()
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should build matrix with test failures`() {
        val pipeline1 = Pipeline(
            id = PipelineId(1),
            sha = CommitSha("abc123"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(2),
            sha = CommitSha("def456"),
            createdAt = Instant.parse("2024-01-15T11:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        val test1 = TestName("test_login")
        val test2 = TestName("test_logout")
        val test3 = TestName("test_profile")

        val failuresByPipeline = mapOf(
            PipelineId(1) to listOf(
                TestFailure(test1, PipelineId(1), "login failed"),
                TestFailure(test2, PipelineId(1), "logout failed")
            ),
            PipelineId(2) to listOf(
                TestFailure(test1, PipelineId(2), "login failed again"),
                TestFailure(test3, PipelineId(2), "profile failed")
            )
        )

        val matrix = MatrixBuilder.build(
            pipelines = listOf(pipeline1, pipeline2),
            failuresByPipeline = failuresByPipeline,
            threshold = FailureThreshold(20)
        )

        matrix.pipelines shouldBe listOf(pipeline1, pipeline2)
        matrix.testFailures shouldBe mapOf(
            test1 to setOf(PipelineId(1), PipelineId(2)),
            test2 to setOf(PipelineId(1)),
            test3 to setOf(PipelineId(2))
        )
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should mark pipeline as overloaded when failures exceed threshold`() {
        val pipeline1 = Pipeline(
            id = PipelineId(1),
            sha = CommitSha("abc123"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(2),
            sha = CommitSha("def456"),
            createdAt = Instant.parse("2024-01-15T11:00:00Z"),
            status = PipelineStatus.SUCCESS,
            author = null
        )

        // Create 25 test failures for pipeline 1 (exceeds threshold of 20)
        val failuresForPipeline1 = (1..25).map { i ->
            TestFailure(TestName("test_$i"), PipelineId(1), "failed")
        }

        val failuresByPipeline = mapOf(
            PipelineId(1) to failuresForPipeline1,
            PipelineId(2) to emptyList()
        )

        val matrix = MatrixBuilder.build(
            pipelines = listOf(pipeline1, pipeline2),
            failuresByPipeline = failuresByPipeline,
            threshold = FailureThreshold(20)
        )

        matrix.pipelines shouldBe listOf(pipeline1, pipeline2)
        matrix.overloadedPipelines shouldBe setOf(PipelineId(1))
        // When overloaded, the individual test failures should NOT be recorded to keep the table manageable
        matrix.testFailures.size shouldBe 0
    }

    @Test
    fun `should sort test failures by frequency descending`() {
        val pipeline1 = Pipeline(PipelineId(1), CommitSha("a"), Instant.now(), PipelineStatus.FAILED, author = null)
        val pipeline2 = Pipeline(PipelineId(2), CommitSha("b"), Instant.now(), PipelineStatus.FAILED, author = null)
        val pipeline3 = Pipeline(PipelineId(3), CommitSha("c"), Instant.now(), PipelineStatus.FAILED, author = null)

        val test1 = TestName("test_frequent")     // fails in 3 pipelines
        val test2 = TestName("test_moderate")     // fails in 2 pipelines
        val test3 = TestName("test_rare")         // fails in 1 pipeline

        val failuresByPipeline = mapOf(
            PipelineId(1) to listOf(
                TestFailure(test1, PipelineId(1), null),
                TestFailure(test2, PipelineId(1), null),
                TestFailure(test3, PipelineId(1), null)
            ),
            PipelineId(2) to listOf(
                TestFailure(test1, PipelineId(2), null),
                TestFailure(test2, PipelineId(2), null)
            ),
            PipelineId(3) to listOf(
                TestFailure(test1, PipelineId(3), null)
            )
        )

        val matrix = MatrixBuilder.build(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            failuresByPipeline = failuresByPipeline,
            threshold = FailureThreshold(20)
        )

        // Get sorted tests
        val sortedTests = matrix.getAllFailedTestsSortedByFrequency()

        // Most frequent test should be first
        sortedTests[0] shouldBe test1  // 3 failures
        sortedTests[1] shouldBe test2  // 2 failures
        sortedTests[2] shouldBe test3  // 1 failure
    }

    @Test
    fun `should handle threshold at boundary`() {
        val pipeline = Pipeline(
            id = PipelineId(1),
            sha = CommitSha("abc"),
            createdAt = Instant.now(),
            status = PipelineStatus.FAILED,
            author = null
        )

        // Exactly 20 failures (threshold is >20, so this should NOT be overloaded)
        val failures = (1..20).map { i ->
            TestFailure(TestName("test_$i"), PipelineId(1), null)
        }

        val matrix = MatrixBuilder.build(
            pipelines = listOf(pipeline),
            failuresByPipeline = mapOf(PipelineId(1) to failures),
            threshold = FailureThreshold(20)
        )

        matrix.overloadedPipelines shouldBe emptySet()

        // 21 failures (should be overloaded)
        val failures21 = failures + TestFailure(TestName("test_21"), PipelineId(1), null)
        val matrix21 = MatrixBuilder.build(
            pipelines = listOf(pipeline),
            failuresByPipeline = mapOf(PipelineId(1) to failures21),
            threshold = FailureThreshold(20)
        )

        matrix21.overloadedPipelines shouldBe setOf(PipelineId(1))
    }
}
