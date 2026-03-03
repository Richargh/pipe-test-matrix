# GitLab CI Test Matrix - Implementation Plan

## Current State
- Empty workspace with basic directory structure
- Target: Kotlin CLI app with Gradle Kotlin DSL

## Implementation Steps

### Phase 1: Project Setup
1. **Initialize Gradle Kotlin project**
   - Create `build.gradle.kts` with Kotlin JVM plugin
   - Create `settings.gradle.kts`
   - Configure Kotlin version and JVM target
   - Add application plugin for CLI execution

2. **Add dependencies**
   - Ktor Client for HTTP requests
   - kotlinx.serialization for JSON parsing
   - Clikt for CLI argument parsing
   - Picnic for ASCII table rendering
   - JUnit 5 + Kotest for testing
   - MockK for mocking

3. **Create project structure**
   - Set up package structure: `com.gitlab.testmatrix`
   - Create subdirectories: config, domain, api, matrix, presentation
   - Create test directory structure

### Phase 2: Domain Model (TDD)
1. **Create value types (semantic wrappers)**
   - `ProjectPath` - wraps project path/ID
   - `BranchName` - wraps branch name
   - `PipelineId` - wraps pipeline ID
   - `CommitSha` - wraps commit SHA
   - `TestName` - wraps test name
   - `FailureThreshold` - wraps the >20 threshold

2. **Create domain entities**
   - `Pipeline`: Represents a pipeline run with ID, SHA, timestamp, status
   - `TestFailure`: Represents a single test failure
   - `TestMatrix`: Represents the complete matrix data structure

3. **Create matrix builder**
   - `MatrixBuilder`: Transforms list of pipelines + failures into matrix structure
   - Handle the >20 failures logic
   - Sort tests by frequency of failure

### Phase 3: GitLab API Client (TDD)
1. **Create API models**
   - Data classes for GitLab API responses
   - Pipeline list response
   - Test report response
   - Use kotlinx.serialization annotations

2. **Implement GitLabClient**
   - Test: Fetch pipelines for a branch (mocked)
   - Impl: GET `/api/v4/projects/:id/pipelines?ref=:branch`
   - Test: Fetch test report for a pipeline (mocked)
   - Impl: GET `/api/v4/projects/:id/pipelines/:pipeline_id/test_report`
   - Test: Handle authentication with PAT
   - Impl: Add "PRIVATE-TOKEN" header
   - Test: Handle errors (404, 401, network errors)
   - Impl: Error handling with meaningful messages

3. **Create repository/service layer**
   - `TestMatrixRepository`: Orchestrates API calls
   - Fetches N pipelines for a branch
   - Fetches test reports for each pipeline
   - Transforms API data to domain models

### Phase 4: Configuration (TDD)
1. **Test: Load GitLab URL from environment**
   - Impl: Read `GITLAB_URL` env var

2. **Test: Load PAT from environment**
   - Impl: Read `GITLAB_TOKEN` env var
   - Validate token format

3. **Test: Provide sensible defaults**
   - Impl: Default pipeline count = 10
   - Default branch = "main"

4. **Test: Validate required configuration**
   - Impl: Fail fast if URL or token missing

### Phase 5: CLI Interface (TDD)
1. **Test: Parse project argument**
   - Impl: Required positional argument for project path

2. **Test: Parse optional count parameter**
   - Impl: `--count` or `-c` flag with default value

3. **Test: Parse optional branch parameter**
   - Impl: `--branch` or `-b` flag with default "main"

4. **Test: Display help text**
   - Impl: Generate help with usage examples

5. **Test: Validate arguments**
   - Impl: Ensure count > 0, project not empty

### Phase 6: Table Rendering (TDD)
1. **Test: Render empty matrix**
   - Impl: Handle case with no failures

2. **Test: Render simple matrix (2x2)**
   - Impl: Use Picnic library to create ASCII table
   - Pipeline IDs as column headers
   - Test names as row headers
   - "✗" for failures, "-" for passes

3. **Test: Render with >20 failures marker**
   - Impl: Show "X" for entire column when threshold exceeded

4. **Test: Format pipeline metadata in headers**
   - Impl: Multi-line headers with ID, SHA, timestamp

5. **Test: Truncate long test names**
   - Impl: Limit test names to reasonable width (e.g., 50 chars)

6. **Test: Handle large matrices**
   - Impl: Consider pagination or limiting displayed rows

### Phase 7: Integration & Main Function
1. **Wire up all components**
   - Create main function
   - Parse CLI arguments
   - Load configuration
   - Initialize GitLabClient
   - Fetch data via repository
   - Build matrix
   - Render table
   - Handle errors with exit codes

2. **Create executable JAR**
   - Configure Gradle application plugin
   - Set main class
   - Create distribution

### Phase 8: Testing & Documentation
1. **Integration tests**
   - Test with real GitLab API (optional, if test instance available)
   - Test full flow with mocked API

2. **Create README**
   - Installation instructions
   - Configuration guide
   - Usage examples
   - Requirements

3. **Manual testing**
   - Test with real GitLab instance
   - Verify edge cases

## Technical Decisions

### GitLab API Endpoints
- **List Pipelines**: `GET /api/v4/projects/:id/pipelines`
  - Query params: `ref` (branch name), `per_page`, `page`
- **Get Test Report**: `GET /api/v4/projects/:id/pipelines/:pipeline_id/test_report`
  - Returns JUnit test results in structured format

### Error Handling Strategy
- Network errors: Retry with exponential backoff (optional for v1)
- 401 Unauthorized: Clear message about token
- 404 Not Found: Clear message about project/pipeline
- 429 Rate Limited: Show message and exit gracefully

### Data Flow
```
CLI Args → Config → GitLabClient → TestMatrixRepository → MatrixBuilder → TableRenderer → Console
```

### Testing Strategy
- Unit tests for domain logic (matrix building, formatting)
- Unit tests with mocked HTTP for API client
- Integration test with in-memory HTTP server
- Manual testing with real API

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // CLI
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // Table rendering
    implementation("com.jakewharton.picnic:picnic:0.7.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.ktor:ktor-client-mock:2.3.7")
}
```

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| GitLab API rate limiting | High | Cache results, add delays between requests |
| Large test reports | Medium | Implement pagination, streaming |
| API version compatibility | Medium | Target common API version (v4), document requirements |
| Complex table rendering | Low | Use Picnic library, keep formatting simple |

## Next Steps
Start with Phase 1: Project Setup
