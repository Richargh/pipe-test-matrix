package de.richargh.pipematrix.app.exposed

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

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

class HeaderModeTest {
    @Test
    fun `should have FULL mode`() {
        val mode = HeaderMode.FULL
        mode shouldBe HeaderMode.FULL
    }

    @Test
    fun `should have NONE mode`() {
        val mode = HeaderMode.NONE
        mode shouldBe HeaderMode.NONE
    }

    @Test
    fun `should have exactly two modes`() {
        HeaderMode.entries.size shouldBe 2
    }
}

class IsoDateTest {
    @Test
    fun `should parse valid ISO date`() {
        val date = IsoDate.parse("2024-01-15")
        date.value shouldBe LocalDate.of(2024, 1, 15)
    }

    @Test
    fun `should parse valid ISO date at year boundaries`() {
        val date = IsoDate.parse("2024-12-31")
        date.value shouldBe LocalDate.of(2024, 12, 31)
    }

    @Test
    fun `should reject empty date string`() {
        shouldThrow<IllegalArgumentException> {
            IsoDate.parse("")
        }
    }

    @Test
    fun `should reject blank date string`() {
        shouldThrow<IllegalArgumentException> {
            IsoDate.parse("   ")
        }
    }

    @Test
    fun `should reject invalid date format`() {
        shouldThrow<IllegalArgumentException> {
            IsoDate.parse("2024/01/15")
        }
    }

    @Test
    fun `should reject invalid date format with text`() {
        shouldThrow<IllegalArgumentException> {
            IsoDate.parse("January 15, 2024")
        }
    }

    @Test
    fun `should reject invalid date values`() {
        shouldThrow<IllegalArgumentException> {
            IsoDate.parse("2024-13-01")  // Invalid month
        }
    }

    @Test
    fun `should convert to GitLab API format for start of day`() {
        val date = IsoDate.parse("2024-01-15")
        val apiFormat = date.toGitLabApiFormatStartOfDay()
        apiFormat shouldBe "2024-01-15T00:00:00Z"
    }

    @Test
    fun `should convert to GitLab API format for end of day`() {
        val date = IsoDate.parse("2024-01-15")
        val apiFormat = date.toGitLabApiFormatEndOfDay()
        apiFormat shouldBe "2024-01-15T23:59:59Z"
    }

    @Test
    fun `should compare dates correctly with isBefore`() {
        val earlier = IsoDate.parse("2024-01-15")
        val later = IsoDate.parse("2024-01-20")
        earlier.isBefore(later) shouldBe true
        later.isBefore(earlier) shouldBe false
    }

    @Test
    fun `should compare dates correctly with isAfter`() {
        val earlier = IsoDate.parse("2024-01-15")
        val later = IsoDate.parse("2024-01-20")
        later.isAfter(earlier) shouldBe true
        earlier.isAfter(later) shouldBe false
    }

    @Test
    fun `should return false for isBefore when dates are equal`() {
        val date1 = IsoDate.parse("2024-01-15")
        val date2 = IsoDate.parse("2024-01-15")
        date1.isBefore(date2) shouldBe false
    }

    @Test
    fun `should return false for isAfter when dates are equal`() {
        val date1 = IsoDate.parse("2024-01-15")
        val date2 = IsoDate.parse("2024-01-15")
        date1.isAfter(date2) shouldBe false
    }
}

class TestClassnameFilterTest {
    @Test
    fun `should create valid filter`() {
        val filter = TestClassnameFilter("MyTestClass")
        filter.value shouldBe "MyTestClass"
    }

    @Test
    fun `should reject empty filter`() {
        shouldThrow<IllegalArgumentException> {
            TestClassnameFilter("")
        }
    }

    @Test
    fun `should reject blank filter`() {
        shouldThrow<IllegalArgumentException> {
            TestClassnameFilter("   ")
        }
    }

    @Test
    fun `should match exact classname`() {
        val filter = TestClassnameFilter("AbrechnungsstatusEmpfaengerAnzeigeTest")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe true
    }

    @Test
    fun `should match partial classname case-insensitive`() {
        val filter = TestClassnameFilter("Empfaenger")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe true
    }

    @Test
    fun `should match with different case`() {
        val filter = TestClassnameFilter("empfaenger")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe true
    }

    @Test
    fun `should match with filter in different case`() {
        val filter = TestClassnameFilter("EMPFAENGER")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe true
    }

    @Test
    fun `should not match unrelated classname`() {
        val filter = TestClassnameFilter("SomethingElse")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe false
    }

    @Test
    fun `should match beginning of classname`() {
        val filter = TestClassnameFilter("Abrechnungs")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe true
    }

    @Test
    fun `should match end of classname`() {
        val filter = TestClassnameFilter("AnzeigeTest")
        filter.matches("AbrechnungsstatusEmpfaengerAnzeigeTest") shouldBe true
    }
}
