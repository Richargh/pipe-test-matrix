package de.richargh.pipematrix.domain

/**
 * Represents a GitLab project path (e.g., "mygroup/myproject").
 */
@JvmInline
value class ProjectPath(val value: String) {
    init {
        require(value.isNotBlank()) { "Project path cannot be empty or blank" }
    }
}

/**
 * Represents a Git branch name.
 */
@JvmInline
value class BranchName(val value: String) {
    init {
        require(value.isNotEmpty()) { "Branch name cannot be empty" }
    }
}

/**
 * Represents a GitLab pipeline ID.
 */
@JvmInline
value class PipelineId(val value: Long) {
    init {
        require(value > 0) { "Pipeline ID must be positive" }
    }
}

/**
 * Represents a Git commit SHA.
 */
@JvmInline
value class CommitSha(val value: String) {
    init {
        require(value.isNotEmpty()) { "Commit SHA cannot be empty" }
    }

    /**
     * Returns the short form of the SHA (first 7 characters).
     */
    fun shortSha(): String = if (value.length > 7) value.take(7) else value
}

/**
 * Represents a test name/identifier.
 */
@JvmInline
value class TestName(val value: String) {
    init {
        require(value.isNotEmpty()) { "Test name cannot be empty" }
    }
}

/**
 * Represents the threshold for marking a pipeline as having too many failures.
 */
@JvmInline
value class FailureThreshold(val value: Int) {
    init {
        require(value >= 0) { "Failure threshold cannot be negative" }
    }

    /**
     * Checks if the given count exceeds this threshold.
     */
    fun isExceeded(count: Int): Boolean = count > value
}

/**
 * Represents the name of a user who triggered a pipeline.
 */
@JvmInline
value class AuthorName(val value: String) {
    init {
        require(value.isNotEmpty()) { "Author name cannot be empty" }
    }

    /**
     * Returns a shortened version of the author name (first name or username).
     */
    fun shortName(): String {
        // If it contains a space, return the first word (first name)
        // Otherwise return the first 15 characters
        val parts = value.split(" ")
        return if (parts.size > 1) {
            parts[0]
        } else {
            if (value.length > 15) value.take(15) else value
        }
    }
}

/**
 * Represents the display mode for table headers.
 */
enum class HeaderMode {
    /** Display full headers with all information (pipeline ID, SHA, status, timestamp, author) */
    FULL,
    /** Display no headers at all */
    NONE
}
