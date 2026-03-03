package de.richargh.pipematrix.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class PipelineTest {
    @Test
    fun `should create pipeline with all fields`() {
        val instant = Instant.parse("2024-01-15T10:30:00Z")
        val pipeline = Pipeline(
            id = PipelineId(12345),
            sha = CommitSha("a1b2c3d4"),
            createdAt = instant,
            status = PipelineStatus.FAILED,
            author = AuthorName("John Doe")
        )

        pipeline.id.value shouldBe 12345
        pipeline.sha.value shouldBe "a1b2c3d4"
        pipeline.createdAt shouldBe instant
        pipeline.status shouldBe PipelineStatus.FAILED
        pipeline.author?.value shouldBe "John Doe"
    }
}

class TestFailureTest {
    @Test
    fun `should create test failure`() {
        val failure = TestFailure(
            testName = TestName("test_user_login"),
            pipelineId = PipelineId(12345),
            message = "AssertionError: Expected 200 but got 500"
        )

        failure.testName.value shouldBe "test_user_login"
        failure.pipelineId.value shouldBe 12345
        failure.message shouldBe "AssertionError: Expected 200 but got 500"
    }
}

class TestMatrixTest {
    @Test
    fun `should create empty test matrix`() {
        val matrix = TestMatrix(
            pipelines = emptyList(),
            testFailures = emptyMap(),
            overloadedPipelines = emptySet()
        )

        matrix.pipelines shouldBe emptyList()
        matrix.testFailures shouldBe emptyMap()
        matrix.overloadedPipelines shouldBe emptySet()
    }

    @Test
    fun `should create test matrix with data`() {
        val pipeline1 = Pipeline(
            PipelineId(1),
            CommitSha("abc"),
            Instant.now(),
            PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            PipelineId(2),
            CommitSha("def"),
            Instant.now(),
            PipelineStatus.SUCCESS,
            author = null
        )
        val test1 = TestName("test1")
        val test2 = TestName("test2")

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            testFailures = mapOf(
                test1 to setOf(PipelineId(1)),
                test2 to setOf(PipelineId(1), PipelineId(2))
            ),
            overloadedPipelines = setOf(PipelineId(1))
        )

        matrix.pipelines.size shouldBe 2
        matrix.testFailures.size shouldBe 2
        matrix.overloadedPipelines.size shouldBe 1
    }

    @Test
    fun `should check if pipeline is overloaded`() {
        val matrix = TestMatrix(
            pipelines = emptyList(),
            testFailures = emptyMap(),
            overloadedPipelines = setOf(PipelineId(123))
        )

        matrix.isOverloaded(PipelineId(123)) shouldBe true
        matrix.isOverloaded(PipelineId(456)) shouldBe false
    }

    @Test
    fun `should check if test failed in pipeline`() {
        val test = TestName("test1")
        val matrix = TestMatrix(
            pipelines = emptyList(),
            testFailures = mapOf(
                test to setOf(PipelineId(1), PipelineId(2))
            ),
            overloadedPipelines = emptySet()
        )

        matrix.didTestFail(test, PipelineId(1)) shouldBe true
        matrix.didTestFail(test, PipelineId(2)) shouldBe true
        matrix.didTestFail(test, PipelineId(3)) shouldBe false
        matrix.didTestFail(TestName("unknown"), PipelineId(1)) shouldBe false
    }
}
