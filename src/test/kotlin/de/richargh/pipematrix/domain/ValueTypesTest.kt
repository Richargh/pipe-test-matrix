package de.richargh.pipematrix.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProjectPathTest {
    @Test
    fun `should create valid project path`() {
        val path = ProjectPath("mygroup/myproject")
        path.value shouldBe "mygroup/myproject"
    }

    @Test
    fun `should reject empty project path`() {
        shouldThrow<IllegalArgumentException> {
            ProjectPath("")
        }
    }

    @Test
    fun `should reject blank project path`() {
        shouldThrow<IllegalArgumentException> {
            ProjectPath("   ")
        }
    }
}

class BranchNameTest {
    @Test
    fun `should create valid branch name`() {
        val branch = BranchName("main")
        branch.value shouldBe "main"
    }

    @Test
    fun `should reject empty branch name`() {
        shouldThrow<IllegalArgumentException> {
            BranchName("")
        }
    }
}

class PipelineIdTest {
    @Test
    fun `should create valid pipeline ID`() {
        val id = PipelineId(12345)
        id.value shouldBe 12345
    }

    @Test
    fun `should reject negative pipeline ID`() {
        shouldThrow<IllegalArgumentException> {
            PipelineId(-1)
        }
    }

    @Test
    fun `should reject zero pipeline ID`() {
        shouldThrow<IllegalArgumentException> {
            PipelineId(0)
        }
    }
}

class CommitShaTest {
    @Test
    fun `should create valid commit SHA`() {
        val sha = CommitSha("a1b2c3d4e5f6")
        sha.value shouldBe "a1b2c3d4e5f6"
    }

    @Test
    fun `should create short SHA`() {
        val sha = CommitSha("a1b2c3d4e5f6789012345678901234567890")
        sha.shortSha() shouldBe "a1b2c3d"
    }

    @Test
    fun `should return full SHA if shorter than 7 chars`() {
        val sha = CommitSha("abc")
        sha.shortSha() shouldBe "abc"
    }

    @Test
    fun `should reject empty SHA`() {
        shouldThrow<IllegalArgumentException> {
            CommitSha("")
        }
    }
}

class TestNameTest {
    @Test
    fun `should create valid test name`() {
        val name = TestName("test_authentication")
        name.value shouldBe "test_authentication"
    }

    @Test
    fun `should reject empty test name`() {
        shouldThrow<IllegalArgumentException> {
            TestName("")
        }
    }
}

class FailureThresholdTest {
    @Test
    fun `should create valid threshold`() {
        val threshold = FailureThreshold(20)
        threshold.value shouldBe 20
    }

    @Test
    fun `should check if count exceeds threshold`() {
        val threshold = FailureThreshold(20)
        threshold.isExceeded(25) shouldBe true
        threshold.isExceeded(20) shouldBe false
        threshold.isExceeded(15) shouldBe false
    }

    @Test
    fun `should reject negative threshold`() {
        shouldThrow<IllegalArgumentException> {
            FailureThreshold(-1)
        }
    }
}
