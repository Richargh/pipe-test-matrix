package de.richargh.pipematrix.app.exposed

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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

/**
 * Represents an ISO-8601 date (YYYY-MM-DD format).
 * Used for date range filtering of pipelines.
 */
@JvmInline
value class IsoDate(val value: LocalDate) {
    companion object {
        /**
         * Parses an ISO-8601 date string (YYYY-MM-DD) into an IsoDate.
         * @throws IllegalArgumentException if the date string is invalid
         */
        fun parse(dateString: String): IsoDate {
            require(dateString.isNotBlank()) { "Date cannot be empty or blank" }
            try {
                val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                return IsoDate(date)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("Invalid date format. Expected ISO-8601 (YYYY-MM-DD): $dateString", e)
            }
        }
    }

    /**
     * Converts this date to GitLab API format (ISO-8601 timestamp at start of day UTC).
     * Use for date range start (--from parameter).
     * Example: "2024-01-15T00:00:00Z"
     */
    fun toGitLabApiFormatStartOfDay(): String {
        return value.atStartOfDay(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
    }

    /**
     * Converts this date to GitLab API format (ISO-8601 timestamp at end of day UTC).
     * Use for date range end (--to parameter).
     * Example: "2024-01-15T23:59:59Z"
     */
    fun toGitLabApiFormatEndOfDay(): String {
        return value.atTime(23, 59, 59).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
    }

    /**
     * Checks if this date is before another date.
     */
    fun isBefore(other: IsoDate): Boolean = value.isBefore(other.value)

    /**
     * Checks if this date is after another date.
     */
    fun isAfter(other: IsoDate): Boolean = value.isAfter(other.value)
}

/**
 * Represents a filter for test class names.
 * Supports case-insensitive partial matching.
 */
@JvmInline
value class TestClassnameFilter(val value: String) {
    init {
        require(value.isNotBlank()) { "Filter cannot be empty or blank" }
    }

    /**
     * Checks if the given classname matches this filter (case-insensitive partial match).
     */
    fun matches(classname: String): Boolean {
        return classname.contains(value, ignoreCase = true)
    }
}
