package de.richargh.pipematrix.app.exposed

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class TestMatrixFilterTest {
    private val pipeline1 = Pipeline(
        id = PipelineId(1),
        sha = CommitSha("abc123"),
        createdAt = Instant.parse("2024-01-15T10:00:00Z"),
        status = PipelineStatus.FAILED,
        author = AuthorName("Alice")
    )

    private val pipeline2 = Pipeline(
        id = PipelineId(2),
        sha = CommitSha("def456"),
        createdAt = Instant.parse("2024-01-16T10:00:00Z"),
        status = PipelineStatus.FAILED,
        author = AuthorName("Bob")
    )

    private val pipeline3 = Pipeline(
        id = PipelineId(3),
        sha = CommitSha("ghi789"),
        createdAt = Instant.parse("2024-01-17T10:00:00Z"),
        status = PipelineStatus.FAILED,
        author = AuthorName("Charlie")
    )

    private val testClassA = "AbrechnungsstatusEmpfaengerAnzeigeTest"
    private val testClassB = "OtherServiceTest"
    private val testClassC = "AnotherEmpfaengerTest"

    @Test
    fun `should filter to single matching classname group`() {
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1), PipelineId(2)))
                    )
                ),
                ClassnameGroup(
                    classname = testClassB,
                    variants = listOf(
                        FailureVariant("A", "test2", null, null, setOf(PipelineId(3)))
                    )
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("Empfaenger")
        val filtered = matrix.filterByClassname(filter)

        filtered.classnameGroups shouldHaveSize 1
        filtered.classnameGroups.first().classname shouldBe testClassA
    }

    @Test
    fun `should filter pipelines to only those with matching test failures`() {
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1), PipelineId(2)))
                    )
                ),
                ClassnameGroup(
                    classname = testClassB,
                    variants = listOf(
                        FailureVariant("A", "test2", null, null, setOf(PipelineId(3)))
                    )
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("Empfaenger")
        val filtered = matrix.filterByClassname(filter)

        filtered.pipelines shouldHaveSize 2
        filtered.pipelines.map { it.id } shouldContainExactlyInAnyOrder listOf(PipelineId(1), PipelineId(2))
    }

    @Test
    fun `should match multiple classnames with partial filter`() {
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1)))
                    )
                ),
                ClassnameGroup(
                    classname = testClassB,
                    variants = listOf(
                        FailureVariant("A", "test2", null, null, setOf(PipelineId(2)))
                    )
                ),
                ClassnameGroup(
                    classname = testClassC,
                    variants = listOf(
                        FailureVariant("A", "test3", null, null, setOf(PipelineId(3)))
                    )
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("Empfaenger")
        val filtered = matrix.filterByClassname(filter)

        filtered.classnameGroups shouldHaveSize 2
        filtered.classnameGroups.map { it.classname } shouldContainExactlyInAnyOrder listOf(testClassA, testClassC)
        filtered.pipelines shouldHaveSize 2
        filtered.pipelines.map { it.id } shouldContainExactlyInAnyOrder listOf(PipelineId(1), PipelineId(3))
    }

    @Test
    fun `should return empty classnameGroups when no match found`() {
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassB,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1)))
                    )
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("NonExistent")
        val filtered = matrix.filterByClassname(filter)

        filtered.classnameGroups.shouldBeEmpty()
        filtered.pipelines.shouldBeEmpty()
    }

    @Test
    fun `should be case-insensitive when matching`() {
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1)))
                    )
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("empfaenger")  // lowercase
        val filtered = matrix.filterByClassname(filter)

        filtered.classnameGroups shouldHaveSize 1
        filtered.classnameGroups.first().classname shouldBe testClassA
    }

    @Test
    fun `should exclude overloaded pipelines when filtering`() {
        val overloadedPipelineId = PipelineId(2)
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1)))
                    )
                )
            ),
            overloadedPipelines = setOf(overloadedPipelineId, PipelineId(3)),
            overloadedPipelineFailures = mapOf(
                overloadedPipelineId to listOf(
                    FailureVariant("A", "test1", null, null, setOf(overloadedPipelineId))
                ),
                PipelineId(3) to listOf(
                    FailureVariant("A", "otherTest", null, null, setOf(PipelineId(3)))
                )
            )
        )

        val filter = TestClassnameFilter("Empfaenger")
        val filtered = matrix.filterByClassname(filter)

        // Should only include non-overloaded pipelines with the matching test
        filtered.pipelines shouldHaveSize 1
        filtered.pipelines.map { it.id } shouldContainExactly listOf(PipelineId(1))

        // Overloaded pipelines should be excluded from filtered results
        filtered.overloadedPipelines.shouldBeEmpty()
        filtered.overloadedPipelineFailures shouldBe emptyMap()
    }

    @Test
    fun `should collect all pipeline IDs from all variants in matching groups`() {
        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2, pipeline3),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(
                        FailureVariant("A", "test1", null, null, setOf(PipelineId(1))),
                        FailureVariant("B", "test2", null, null, setOf(PipelineId(2))),
                        FailureVariant("C", "test3", null, null, setOf(PipelineId(3)))
                    )
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("Empfaenger")
        val filtered = matrix.filterByClassname(filter)

        filtered.pipelines shouldHaveSize 3
        filtered.pipelines.map { it.id } shouldContainExactlyInAnyOrder listOf(
            PipelineId(1),
            PipelineId(2),
            PipelineId(3)
        )
    }

    @Test
    fun `should preserve failure variants in filtered classname groups`() {
        val variant1 = FailureVariant("A", "test1", "output1", "trace1", setOf(PipelineId(1)))
        val variant2 = FailureVariant("B", "test2", "output2", "trace2", setOf(PipelineId(2)))

        val matrix = TestMatrix(
            pipelines = listOf(pipeline1, pipeline2),
            classnameGroups = listOf(
                ClassnameGroup(
                    classname = testClassA,
                    variants = listOf(variant1, variant2)
                )
            ),
            overloadedPipelines = emptySet()
        )

        val filter = TestClassnameFilter("Empfaenger")
        val filtered = matrix.filterByClassname(filter)

        filtered.classnameGroups.first().variants shouldContainExactly listOf(variant1, variant2)
    }
}
