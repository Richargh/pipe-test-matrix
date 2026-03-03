# GitLab CI Test Matrix - Specification

## Overview
A Kotlin CLI application that connects to a self-hosted GitLab CI API and displays a matrix of test failures across recent pipeline runs for the main branch.

## Requirements

### Functional Requirements

#### 1. GitLab API Integration
- Connect to a self-hosted GitLab instance (URL provided via configuration)
- Authenticate using a Personal Access Token (PAT)
- Fetch pipeline runs for the `main` branch of a specified project
- Retrieve test failure information for each pipeline run

#### 2. Pipeline Selection
- Fetch a configurable number of recent pipeline runs
- Only consider completed pipelines (success, failed, or canceled)
- Order pipelines chronologically (most recent first)

#### 3. Test Failure Analysis
- Extract individual test failures from each pipeline
- Identify tests by their unique name/identifier
- Count the number of test failures per pipeline
- Mark pipelines with >20 test failures as "X" (data overload indicator)

#### 4. Matrix Display
- **Columns**: Pipeline runs (most recent to oldest, left to right)
- **Rows**: Unique test names that failed across all analyzed pipelines
- **Cells**: Indicate whether a specific test failed in that pipeline run
  - Use a marker (e.g., "✗" or "F") for failures
  - Use empty or "-" for passing tests
  - Use "X" for entire column if pipeline has >20 failures

#### 5. Table Formatting
- ASCII table with borders (using a library like `picnic`)
- Include pipeline metadata in column headers:
  - Pipeline ID
  - Commit SHA (short)
  - Timestamp or age
- Include row headers with test names
- Handle long test names gracefully (truncation or wrapping)

#### 6. Command-Line Interface
- Accept project ID or path as a required argument
- Accept number of pipelines to fetch as an optional parameter (default: 10)
- Display usage help with `-h` or `--help`

### Non-Functional Requirements

#### 1. Configuration
- GitLab instance URL: Environment variable or config file
- Personal Access Token: Environment variable (secure)
- Default pipeline count: Configurable

#### 2. Error Handling
- Handle network errors gracefully
- Provide clear error messages for:
  - Invalid authentication
  - Project not found
  - No pipelines found
  - API rate limiting

#### 3. Performance
- Efficient API calls (minimize requests)
- Reasonable response time for typical datasets

#### 4. Code Quality
- Follow TDD principles
- Strong typing with semantic wrappers
- Clean, maintainable code structure

## Input/Output Examples

### Command Execution
```bash
# Using environment variables
export GITLAB_URL="https://gitlab.example.com"
export GITLAB_TOKEN="glpat-xxxxxxxxxxxx"

# Run with project path
./gitlab-ci-test-matrix mygroup/myproject

# Run with custom pipeline count
./gitlab-ci-test-matrix mygroup/myproject --count 5
```

### Output Example
```
GitLab CI Test Matrix - mygroup/myproject (main branch)

Pipeline | #12345  | #12344  | #12343  | #12342  | #12341  |
         | a3f2b1  | b4c5d6  | c6d7e8  | d8e9f0  | e0f1g2  |
         | 2h ago  | 5h ago  | 8h ago  | 1d ago  | 2d ago  |
---------|---------|---------|---------|---------|---------|
test_auth_login         | ✗       | -       | -       | ✗       | -       |
test_user_create        | ✗       | ✗       | -       | -       | -       |
test_payment_process    | -       | ✗       | ✗       | -       | -       |
test_db_connection      | X       | X       | X       | X       | ✗       |

Note: 'X' indicates pipeline had >20 test failures
```

## Technical Stack

- **Language**: Kotlin (JVM)
- **Build System**: Gradle with Kotlin DSL
- **HTTP Client**: Ktor Client or OkHttp
- **JSON Parsing**: kotlinx.serialization or Jackson
- **CLI Parsing**: kotlinx-cli or Clikt
- **Table Rendering**: Picnic
- **Testing**: JUnit 5 + Kotest or MockK

## Project Structure

```
gitlab-ci-test-matrix/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/gitlab/testmatrix/
│   │           ├── Main.kt
│   │           ├── config/
│   │           │   ├── AppConfig.kt
│   │           │   └── GitLabConfig.kt
│   │           ├── domain/
│   │           │   ├── Pipeline.kt
│   │           │   ├── TestFailure.kt
│   │           │   └── TestMatrix.kt
│   │           ├── api/
│   │           │   ├── GitLabClient.kt
│   │           │   └── models/
│   │           ├── matrix/
│   │           │   └── MatrixBuilder.kt
│   │           └── presentation/
│   │               └── TableRenderer.kt
│   └── test/
│       └── kotlin/
│           └── com/gitlab/testmatrix/
│               ├── api/
│               ├── matrix/
│               └── presentation/
└── README.md
```

## Resolved Decisions
- **Test Failure Source**: Use GitLab's Test Reports API (JUnit XML reports)
- **Branch Support**: Accept branch name as optional CLI parameter (default: `main`)

## Future Enhancements
- Table transposition if there are many pipelines but few tests
- Support for multiple branches comparison
