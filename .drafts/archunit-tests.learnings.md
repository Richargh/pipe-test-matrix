# ArchUnit Tests Implementation

## Overview
Added comprehensive ArchUnit tests to enforce architectural boundaries and prevent dependency violations.

## Package Structure
The codebase uses a clean architecture with these slices:
- **app** - Application core layer
  - `exposed` - Public API (domain entities, value types, interfaces)
  - `hidden` - Internal implementation details
  - `TestMatrixFacade` - Application facade
- **config** - Configuration management
- **infrastructure** - External integrations (GitLab API client)
- **presentation** - UI/CLI layer (TableRenderer, CLI command)

## Architecture Rules Enforced

### 1. Slice Cycle Detection (1 test)
- ✅ All slices (app, config, infrastructure, presentation) are free of circular dependencies

**Rationale**: Using ArchUnit's built-in slicing API to detect cycles between major architectural slices.

### 2. App Slice Isolation (1 test)
- ✅ App slice cannot depend on any external slices (config, infrastructure, presentation)
- ✅ Allows: standard libraries (java, kotlin, kotlinx, org.jetbrains), com.gitlab.api (DTOs)

**Rationale**: Single comprehensive rule ensures app slice is isolated. Filters out compiler-generated dependencies to focus on real architectural violations.

### 3. Hidden Package Protection (2 tests)
- ✅ Only `app` package can access `app.hidden` (legacy specific test)
- ✅ **Generalized rule**: Hidden packages in ANY slice are only accessible from that slice
  - `app.hidden` only accessible from `app`
  - `config.hidden` only accessible from `config`
  - `infrastructure.hidden` only accessible from `infrastructure`
  - `presentation.hidden` only accessible from `presentation`

**Rationale**: Enforces information hiding across all slices - internal implementation details cannot leak outside their owning slice.

### 4. No Upward Dependencies (2 tests)
- ✅ `app.exposed` cannot depend on `TestMatrixFacade`
- ✅ `app.hidden` cannot depend on `TestMatrixFacade`

**Rationale**: Lower-level domain types and implementations should not know about higher-level facade. Prevents circular dependencies.

### 5. Hidden Isolation (1 test)
- ✅ Hidden packages in ANY slice can only depend on:
  - Their own hidden package
  - Their corresponding exposed package (within same slice)
  - Standard libraries (java, kotlin, kotlinx, org.jetbrains)
- Uses `allowEmptyShould(true)` to handle slices without hidden packages yet

**Rationale**: Hidden implementation should only use domain types and other internal implementation within the same slice, not external dependencies. This ensures implementation details don't leak across slice boundaries.

## Technical Details

### Dependency Added
```kotlin
testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
```

### Test Files
- `/workspace/src/test/kotlin/de/richargh/pipematrix/ArchitectureTest.kt` - Architecture rules
- `/workspace/src/test/kotlin/de/richargh/pipematrix/ArchUnitExtension.kt` - JUnit 5 extension

### Key Implementation Notes
1. **JUnit 5 Extension**: Created `ArchUnitExtension` that implements:
   - `BeforeAllCallback` - loads JavaClasses once before all tests
   - `ParameterResolver` - injects JavaClasses into test methods via parameters
   - Stores classes in ExtensionContext.Store for reuse across tests
2. **Parameter Injection**: Tests receive `JavaClasses` as method parameters instead of static fields
3. **Slice definitions centralized**: All slice/package patterns defined once as constants in companion object
4. **ArchUnit Slicing API properly used**:
   - `slices().matching(SLICES).should().beFreeOfCycles()` for cycle detection
   - Package-based `noClasses()` rules for unidirectional dependency constraints
5. **Why not use slices for all rules?**
   - Slicing API is designed for mutual dependencies and cycles
   - Unidirectional rules ("A should not depend on B") work better with package-based rules
   - `noClasses()` provides clearer, more specific constraints
6. **Slice pattern**: `"de.richargh.pipematrix.(*).."` captures top-level packages (app, config, infrastructure, presentation)
7. Used `ImportOption.DoNotIncludeTests()` to analyze only production code
8. Excluded JetBrains annotations (compiler-generated, not real dependencies)
9. Used `allowEmptyShould(true)` for rules checking packages that might not exist yet (e.g., config.hidden, infrastructure.hidden)
10. All 4 tests passing
11. Tests run automatically with `./gradlew test`

### Benefits of JUnit Extension
- **Reusability**: Extension can be used by multiple test classes
- **Cleaner tests**: No @BeforeAll boilerplate in each test class
- **Parameter injection**: Type-safe JavaClasses injection via method parameters
- **Single responsibility**: Extension handles setup, tests focus on rules

## Benefits
1. **Enforces boundaries**: Prevents accidental coupling between layers
2. **Documents architecture**: Tests serve as executable documentation
3. **Prevents regression**: Architecture violations caught in CI
4. **Guides refactoring**: Clear rules help developers maintain structure

## Verification
Current test status (4 tests):
```bash
./gradlew test --tests "ArchitectureTest"
# 4 tests completed, all passing
# 1. Slice cycle detection
# 2. App slice isolation
# 3. Hidden package protection (generalized for all slices)
# 4. Hidden package dependency isolation (generalized for all slices)
```

All tests are passing. The codebase complies with all defined architectural rules.

## Next Steps
Architecture rules are in place and enforced. The codebase currently complies with all defined rules.
