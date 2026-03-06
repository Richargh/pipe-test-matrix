# GitLab CI Test Matrix

A Kotlin CLI tool that displays a matrix of test failures across recent GitLab CI pipeline runs. Helps identify flaky tests and patterns of failures.

## Features

- Fetches recent pipeline runs for a given GitLab project and branch
- Retrieves test reports from each pipeline
- Displays a matrix showing which tests failed in which pipelines
- Shows failure count before each test name for quick identification
- Identifies pipelines with >20 failures (marked as "overloaded")
- For overloaded pipelines, displays count of hidden tests not shown in table

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

## Usage

### Using the run script (recommended)

```bash
./run mygroup/myproject --branch main --count 10
```

### Using Gradle directly

```bash
./gradlew run --args="mygroup/myproject --branch main --count 10"
```

### Common options

- `--branch <name>`: Branch to analyze (default: main)
- `--count <n>`: Number of pipelines (default: 10)
- `--headers <mode>`: Header display: `full` or `none` (default: full)
- `--help`: Show help

## Output Example

```
┌─────────────────────────────┬────────┬────────┬──────────┐
│ Test Class / Variant        │ #12345 │ #12344 │ #12343   │
│                             │ abc123d│ def456a│ 789bcef  │
│                             │ failed │ failed │ success  │
│                             │ 3/6/26 │ 3/6/26 │ 3/5/26   │
│                             │ John   │ Jane   │ Bob      │
├─────────────────────────────┼────────┼────────┼──────────┤
│ UserAuthTest (4)            │   A B  │   A    │   A      │
│   [A] should login          │        │        │          │
│   [B] should logout         │        │        │          │
│ ApiIntegrationTest (2)      │   C    │        │   C      │
│   [C] should call endpoint  │        │        │          │
│       Error: timeout...     │        │        │          │
└─────────────────────────────┴────────┴────────┴──────────┘
```

### Matrix Symbols

- `ClassName (N)` - Test class with total failure count across all pipelines
- `[A] test name` - Failure variant with unique letter identifier
- `A B` - Variant letters that failed in this pipeline
- Empty cell - No failures in this pipeline
- `(+N)` - In overloaded pipeline headers, count of additional hidden failures

## License

This project is licensed under the UNLICENSE and therefore public domain.
