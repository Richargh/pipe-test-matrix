package de.richargh.pipematrix.presentation

import de.richargh.pipematrix.domain.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.time.Instant

class TableRendererTest {

    @Test
    fun `should render empty matrix`() {
        val matrix = TestMatrix(
            pipelines = emptyList(),
            testFailures = emptyMap(),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        output shouldContain "No test failures found"
    }

    @Test
    fun `should render matrix with no failures`() {
        val pipeline = Pipeline(
            id = PipelineId(12345),
            sha = CommitSha("abc123def"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.SUCCESS,
            author = null
        )

        val matrix = TestMatrix(
            pipelines = listOf(pipeline),
            testFailures = emptyMap(),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        output shouldContain "No test failures found"
    }

    @Test
    fun `should render simple matrix with one failure`() {
        val pipeline = Pipeline(
            id = PipelineId(12345),
            sha = CommitSha("abc123def"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        val testName = TestName("test_user_login")

        val matrix = TestMatrix(
            pipelines = listOf(pipeline),
            testFailures = mapOf(testName to setOf(PipelineId(12345))),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Should contain pipeline ID
        output shouldContain "12345"
        // Should contain short SHA
        output shouldContain "abc123d"
        // Should contain test name
        output shouldContain "test_user_login"
        // Should contain failure marker
        output shouldContain "✗"
    }

    @Test
    fun `should render matrix with multiple pipelines and tests`() {
        val pipeline1 = Pipeline(
            id = PipelineId(100),
            sha = CommitSha("aaa111"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(200),
            sha = CommitSha("bbb222"),
            createdAt = Instant.parse("2024-01-15T09:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        val test1 = TestName("test_login")
        val test2 = TestName("test_logout")

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            testFailures = mapOf(
                test1 to setOf(PipelineId(100), PipelineId(200)),
                test2 to setOf(PipelineId(100))
            ),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Should contain both pipeline IDs
        output shouldContain "100"
        output shouldContain "200"
        // Should contain both test names
        output shouldContain "test_login"
        output shouldContain "test_logout"
        // Should have failure markers
        output shouldContain "✗"
    }

    @Test
    fun `should render overloaded pipeline with failure markers`() {
        val pipeline1 = Pipeline(
            id = PipelineId(100),
            sha = CommitSha("abc123"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(200),
            sha = CommitSha("def456"),
            createdAt = Instant.parse("2024-01-15T09:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        // test_login fails in both pipelines (one is overloaded)
        // test_logout only fails in the normal pipeline
        val testFailures = mapOf(
            TestName("test_login") to setOf(PipelineId(200)),  // Only non-overloaded pipeline
            TestName("test_logout") to setOf(PipelineId(200))
        )

        // Pipeline 100 is overloaded with test_login failure
        val overloadedFailures = mapOf(
            PipelineId(100) to setOf(TestName("test_login"))
        )

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            testFailures = testFailures,
            overloadedPipelines = setOf(PipelineId(100)),
            overloadedPipelineFailures = overloadedFailures
        )

        val output = TableRenderer.render(matrix)

        // Should indicate overloaded pipeline in header
        output shouldContain "20"  // Threshold indicator
        // Should show ✗ for tests that actually failed in overloaded pipeline
        output shouldContain "✗"
        // Should NOT show X marker
        output shouldNotContain "X"
    }

    @Test
    fun `should show failure marker only for failed tests`() {
        val pipeline1 = Pipeline(
            id = PipelineId(100),
            sha = CommitSha("aaa111"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(200),
            sha = CommitSha("bbb222"),
            createdAt = Instant.parse("2024-01-15T09:00:00Z"),
            status = PipelineStatus.SUCCESS,
            author = null
        )

        val test1 = TestName("test_login")

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            testFailures = mapOf(
                test1 to setOf(PipelineId(100))  // Only fails in pipeline 100
            ),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Should have failure marker for failed test
        output shouldContain "✗"
        // Should NOT have dash for passed tests
        output shouldNotContain "-"
    }

    @Test
    fun `should truncate long test names`() {
        val pipeline = Pipeline(
            id = PipelineId(12345),
            sha = CommitSha("abc123"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        val longTestName = TestName("a".repeat(100))  // 100 character test name

        val matrix = TestMatrix(
            pipelines = listOf(pipeline),
            testFailures = mapOf(longTestName to setOf(PipelineId(12345))),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Should contain truncated name with ellipsis
        output shouldContain "..."
        // Should not contain the full 100-character name
        output shouldNotContain "a".repeat(100)
    }

    @Test
    fun `should sort tests by failure frequency`() {
        val pipeline1 = Pipeline(PipelineId(1), CommitSha("a"), Instant.now(), PipelineStatus.FAILED, author = null)
        val pipeline2 = Pipeline(PipelineId(2), CommitSha("b"), Instant.now(), PipelineStatus.FAILED, author = null)
        val pipeline3 = Pipeline(PipelineId(3), CommitSha("c"), Instant.now(), PipelineStatus.FAILED, author = null)

        val frequentTest = TestName("frequent_test")  // Fails in 3 pipelines
        val moderateTest = TestName("moderate_test")  // Fails in 2 pipelines
        val rareTest = TestName("rare_test")          // Fails in 1 pipeline

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            testFailures = mapOf(
                frequentTest to setOf(PipelineId(1), PipelineId(2), PipelineId(3)),
                moderateTest to setOf(PipelineId(1), PipelineId(2)),
                rareTest to setOf(PipelineId(1))
            ),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Most frequent test should appear first
        val frequentPos = output.indexOf("frequent_test")
        val moderatePos = output.indexOf("moderate_test")
        val rarePos = output.indexOf("rare_test")

        frequentPos shouldBe frequentPos  // Just to use the variable
        // Most frequent should come before moderate
        (frequentPos < moderatePos) shouldBe true
        // Moderate should come before rare
        (moderatePos < rarePos) shouldBe true
    }

    @Test
    fun `should include pipeline metadata in headers`() {
        val pipeline = Pipeline(
            id = PipelineId(12345),
            sha = CommitSha("abc123def456"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        val matrix = TestMatrix(
            pipelines = listOf(pipeline),
            testFailures = mapOf(TestName("test") to setOf(PipelineId(12345))),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Should contain pipeline ID
        output shouldContain "12345"
        // Should contain short SHA
        output shouldContain "abc123d"
    }

    @Test
    fun `should display failure count before test name`() {
        val pipeline1 = Pipeline(PipelineId(1), CommitSha("a"), Instant.now(), PipelineStatus.FAILED, author = null)
        val pipeline2 = Pipeline(PipelineId(2), CommitSha("b"), Instant.now(), PipelineStatus.FAILED, author = null)
        val pipeline3 = Pipeline(PipelineId(3), CommitSha("c"), Instant.now(), PipelineStatus.FAILED, author = null)

        val test1 = TestName("test_one")  // Fails 3 times
        val test2 = TestName("test_two")  // Fails 2 times
        val test3 = TestName("test_three")  // Fails 1 time

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            testFailures = mapOf(
                test1 to setOf(PipelineId(1), PipelineId(2), PipelineId(3)),
                test2 to setOf(PipelineId(1), PipelineId(2)),
                test3 to setOf(PipelineId(1))
            ),
            overloadedPipelines = emptySet()
        )

        val output = TableRenderer.render(matrix)

        // Should show failure counts in format (N) test_name
        output shouldContain "(3) test_one"
        output shouldContain "(2) test_two"
        output shouldContain "(1) test_three"
    }

    @Test
    fun `should show hidden test count for overloaded pipelines`() {
        val pipeline1 = Pipeline(
            id = PipelineId(100),
            sha = CommitSha("abc123"),
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )
        val pipeline2 = Pipeline(
            id = PipelineId(200),
            sha = CommitSha("def456"),
            createdAt = Instant.parse("2024-01-15T09:00:00Z"),
            status = PipelineStatus.FAILED,
            author = null
        )

        // Pipeline 100 is overloaded with 30 failures total
        // But only test_common fails in both pipelines, so it appears in the table
        // The other 29 tests from pipeline 100 don't appear in the table
        val overloadedFailures = mutableSetOf<TestName>()
        overloadedFailures.add(TestName("test_common"))
        for (i in 1..29) {
            overloadedFailures.add(TestName("hidden_test_$i"))
        }

        val testFailures = mapOf(
            TestName("test_common") to setOf(PipelineId(200)),  // Also fails in normal pipeline
            TestName("test_normal") to setOf(PipelineId(200))
        )

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            testFailures = testFailures,
            overloadedPipelines = setOf(PipelineId(100)),
            overloadedPipelineFailures = mapOf(PipelineId(100) to overloadedFailures)
        )

        val output = TableRenderer.render(matrix)

        // Should show that pipeline 100 is overloaded with +29 hidden tests
        // (30 total failures - 1 shown test_common = 29 hidden)
        output shouldContain "(+29)"
    }
}
