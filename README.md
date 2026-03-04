# GitLab CI Test Matrix

A Kotlin CLI tool that displays a matrix of test failures across recent GitLab CI pipeline runs. Helps identify flaky tests and patterns of failures.

## Features

- Fetches recent pipeline runs for a given GitLab project and branch
- Retrieves test reports from each pipeline
- Displays a matrix showing which tests failed in which pipelines
- Shows failure count before each test name for quick identification
- Identifies pipelines with >20 failures (marked as "overloaded")
- For overloaded pipelines, displays count of hidden tests not shown in table
- Sorts tests by failure frequency (most frequent first)
- Clean ASCII table output

## Requirements

- Java 21 or higher
- GitLab instance with API access
- GitLab Personal Access Token with `read_api` scope

## Installation

### Build from Source

```bash
./gradlew build
```

This creates an executable distribution in `build/distributions/`.

### Extract and Run

```bash
cd build/distributions
unzip gitlab-ci-test-matrix-1.0.0.zip
cd gitlab-ci-test-matrix-1.0.0/bin
```

## Configuration

The tool requires two environment variables:

```bash
export GITLAB_DEBUG_URL="https://gitlab.example.com"
export GITLAB_DEBUG_TOKEN_URL="your-personal-access-token"
```

### Optional Environment Variables

- `DEFAULT_BRANCH`: Default branch to analyze (default: `main`)
- `DEFAULT_PIPELINE_COUNT`: Default number of pipelines to fetch (default: `10`)
- `FAILURE_THRESHOLD`: Threshold for marking pipelines as overloaded (default: `20`)

## Usage

### Basic Usage

```bash
gitlab-test-matrix mygroup/myproject
```

This analyzes the last 10 pipelines on the default branch.

### Specify Branch

```bash
gitlab-test-matrix mygroup/myproject --branch develop
```

### Specify Pipeline Count

```bash
gitlab-test-matrix mygroup/myproject --count 20
```

### Combine Options

```bash
gitlab-test-matrix mygroup/myproject --branch main --count 15
```

### Control Header Display

You can control how table headers are displayed using the `--headers` flag:

```bash
# Full headers with all information (default)
gitlab-test-matrix mygroup/myproject --headers full

# No headers at all
gitlab-test-matrix mygroup/myproject --headers none
```

### Help

```bash
gitlab-test-matrix --help
```

## Output Example

```
Fetching test matrix for mygroup/myproject on branch main...
Analyzing last 10 pipelines...

┌───────────────────────────────┬────────┬────────┬──────────┐
│ Test Name                     │ #12345 │ #12344 │ #12343   │
│                               │ abc123d│ def456a│ 789bcef  │
├───────────────────────────────┼────────┼────────┼──────────┤
│ (3) test_user_authentication  │   ✗    │   ✗    │   ✗      │
│ (2) test_api_integration      │   ✗    │        │   ✗      │
│ (1) test_database_connection  │        │   ✗    │          │
└───────────────────────────────┴────────┴────────┴──────────┘

Summary:
  Pipelines analyzed: 3
  Unique test failures: 3
  Overloaded pipelines (>20 failures): 0
```

### Matrix Symbols

- `(N)` - Number before test name shows how many times the test failed
- `✗` - Test failed in this pipeline
- Empty cell - Test passed or didn't run in this pipeline
- `(+N)` - In overloaded pipeline headers, shows how many failing tests are not displayed

## Development

### Run Tests

```bash
./gradlew test
```

### Build

```bash
./gradlew build
```

### Run without Building Distribution

```bash
./gradlew run --args="mygroup/myproject --branch main"
```

Note: You'll need to set GITLAB_DEBUG_URL and GITLAB_DEBUG_TOKEN_URL environment variables before running.

## Architecture

The application follows a clean architecture with TDD:

- **Domain Layer**: Value types, entities, and business logic
- **API Layer**: GitLab API client and models
- **Repository Layer**: Data fetching and transformation
- **Presentation Layer**: Table rendering
- **CLI Layer**: Command-line interface
- **Configuration**: Environment variable management

### Key Components

- `ValueTypes.kt`: Semantic wrappers for primitives (ProjectPath, BranchName, etc.)
- `DomainEntities.kt`: Core domain models (Pipeline, TestFailure, TestMatrix)
- `MatrixBuilder.kt`: Constructs test matrix from API data
- `GitLabClient.kt`: HTTP client for GitLab API
- `TestMatrixRepository.kt`: Orchestrates API calls
- `TableRenderer.kt`: Renders ASCII tables using Picnic
- `TestMatrixCommand.kt`: CLI parsing using Clikt
- `Config.kt`: Configuration management
- `Main.kt`: Application entry point

## Testing

The project has comprehensive test coverage:

- Unit tests for all domain logic
- Integration tests with fake implementations
- Tests for API models and serialization
- Tests for CLI argument parsing
- Tests for table rendering

All tests use TDD approach with Kotest assertions and JUnit 5.

## Technology Stack

- **Kotlin 1.9.22**: Programming language
- **Gradle Kotlin DSL**: Build system
- **Ktor Client**: HTTP client
- **kotlinx.serialization**: JSON parsing
- **kotlinx.coroutines**: Async operations
- **Clikt**: CLI argument parsing
- **Picnic**: ASCII table rendering
- **JUnit 5 + Kotest**: Testing framework

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please ensure:

1. All tests pass (`./gradlew test`)
2. Code follows TDD practices
3. New features include tests
4. Documentation is updated

## Troubleshooting

### "GITLAB_DEBUG_URL environment variable is required"

Ensure you've set the required environment variables:

```bash
export GITLAB_DEBUG_URL="https://gitlab.example.com"
export GITLAB_DEBUG_TOKEN_URL="your-token"
```

### "GitLab API authentication failed"

Check that your GITLAB_DEBUG_TOKEN_URL is valid and has the `read_api` scope.

### "GitLab resource not found"

Verify the project path is correct and your token has access to the project.

### "GitLab API rate limit exceeded"

Wait a few minutes and try again. Consider reducing the number of pipelines analyzed with `--count`.
