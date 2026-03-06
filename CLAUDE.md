# CLAUDE.md

GitLab CI Test Matrix - A Kotlin CLI tool that displays a matrix of test failures across recent GitLab CI pipeline runs to identify flaky tests and failure patterns.

## Build & Test Commands

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Run Single Test
```bash
./gradlew test --tests "de.richargh.pipematrix.ClassName.testMethodName"
```

## Architecture

The codebase uses a **slice-based architecture** with an **onion architecture layering** to enforce encapsulation:

- `app/`: Domain logic (isolated from all other slices)
  - Put the entry  
  - Use `hidden/` for implementation details that shouldn't leak outside app
  - Use `exposed/` for public APIs of the app
- `infrastructure/`: External integrations
- `presentation/`: CLI interface and table rendering
