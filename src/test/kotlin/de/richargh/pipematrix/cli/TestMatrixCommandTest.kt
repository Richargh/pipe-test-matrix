package de.richargh.pipematrix.cli

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.io.PrintWriter
import java.io.StringWriter

class TestMatrixCommandTest {

    private fun parseCommand(args: String): Pair<TestMatrixCommand, Int> {
        val command = TestMatrixCommand()
        val commandLine = CommandLine(command)
        val exitCode = try {
            commandLine.parseArgs(*args.split(" ").toTypedArray())
            0
        } catch (e: Exception) {
            2
        }
        return command to exitCode
    }

    private fun parseCommandWithOutput(args: String): Triple<TestMatrixCommand, Int, String> {
        val command = TestMatrixCommand()
        val commandLine = CommandLine(command)
        val sw = StringWriter()
        commandLine.out = PrintWriter(sw)
        commandLine.err = PrintWriter(sw)

        val exitCode = commandLine.execute(*args.split(" ").filter { it.isNotBlank() }.toTypedArray())
        return Triple(command, exitCode, sw.toString())
    }

    @Test
    fun `should parse required project argument url and token`() {
        val (command, exitCode) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx")

        exitCode shouldBe 0
        command.projectPath shouldBe "mygroup/myproject"
        command.gitlabUrl shouldBe "https://gitlab.com"
        command.gitlabToken shouldBe "glpat-xxx"
    }

    @Test
    fun `should use default values when optional parameters not provided`() {
        val (command, _) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx")

        command.projectPath shouldBe "mygroup/myproject"
        command.gitlabUrl shouldBe "https://gitlab.com"
        command.gitlabToken shouldBe "glpat-xxx"
        command.branch shouldBe "main"  // Default value
        command.count shouldBe 10   // Default value
    }

    @Test
    fun `should parse optional count parameter with short flag`() {
        val (command, _) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx -c 20")

        command.projectPath shouldBe "mygroup/myproject"
        command.count shouldBe 20
    }

    @Test
    fun `should parse optional count parameter with long flag`() {
        val (command, _) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx --count 15")

        command.projectPath shouldBe "mygroup/myproject"
        command.count shouldBe 15
    }

    @Test
    fun `should parse optional branch parameter with short flag`() {
        val (command, _) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx -b develop")

        command.projectPath shouldBe "mygroup/myproject"
        command.branch shouldBe "develop"
    }

    @Test
    fun `should parse optional branch parameter with long flag`() {
        val (command, _) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx --branch develop")

        command.projectPath shouldBe "mygroup/myproject"
        command.branch shouldBe "develop"
    }

    @Test
    fun `should parse all parameters together`() {
        val (command, _) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx --branch develop --count 25")

        command.projectPath shouldBe "mygroup/myproject"
        command.gitlabUrl shouldBe "https://gitlab.com"
        command.gitlabToken shouldBe "glpat-xxx"
        command.branch shouldBe "develop"
        command.count shouldBe 25
    }

    @Test
    fun `should fail when project argument is missing`() {
        val (_, exitCode, output) = parseCommandWithOutput("--url https://gitlab.com --token glpat-xxx")

        exitCode shouldBe 2
        output shouldContain "Missing required parameter"
    }

    @Test
    fun `should fail when url is missing`() {
        val (_, exitCode, output) = parseCommandWithOutput("mygroup/myproject --token glpat-xxx")

        exitCode shouldBe 2
        output shouldContain "Missing required option"
    }

    @Test
    fun `should fail when token is missing`() {
        val (_, exitCode, output) = parseCommandWithOutput("mygroup/myproject --url https://gitlab.com")

        exitCode shouldBe 2
        output shouldContain "Missing required option"
    }

    @Test
    fun `should display help text with --help flag`() {
        val (_, exitCode, output) = parseCommandWithOutput("--help")

        exitCode shouldBe 0
        output shouldContain "Usage:"
        output shouldContain "PROJECT_PATH"
        output shouldContain "--branch"
        output shouldContain "--count"
        output shouldContain "--url"
        output shouldContain "--token"
    }

    @Test
    fun `should display help text with -h flag`() {
        val (_, exitCode, output) = parseCommandWithOutput("-h")

        exitCode shouldBe 0
        output shouldContain "Usage:"
    }

    @Test
    fun `should accept project path with slashes`() {
        val (command, exitCode) = parseCommand("group/subgroup/project --url https://gitlab.com --token glpat-xxx")

        exitCode shouldBe 0
        command.projectPath shouldBe "group/subgroup/project"
    }

    @Test
    fun `should accept project path with hyphens and underscores`() {
        val (command, exitCode) = parseCommand("my-group/my_project --url https://gitlab.com --token glpat-xxx")

        exitCode shouldBe 0
        command.projectPath shouldBe "my-group/my_project"
    }

    @Test
    fun `should parse headers flag with full value`() {
        val (command, exitCode) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx --headers full")

        exitCode shouldBe 0
        command.headerMode shouldBe "full"
    }

    @Test
    fun `should parse headers flag with none value`() {
        val (command, exitCode) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx --headers none")

        exitCode shouldBe 0
        command.headerMode shouldBe "none"
    }

    @Test
    fun `should default to full header mode when not specified`() {
        val (command, exitCode) = parseCommand("mygroup/myproject --url https://gitlab.com --token glpat-xxx")

        exitCode shouldBe 0
        command.headerMode shouldBe "full"
    }
}
