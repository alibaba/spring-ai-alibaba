# Monodimensional Testing Strategy for LLM Readiness Health Indicator

## Files

### Production Files (2)
1. **LlmReadinessAutoConfiguration.java** - Auto-configuration class
2. **LlmReadinessHealthIndicator.java** - Health indicator logic

### Test Files (4)
1. **LlmReadinessAutoConfigurationNoActuatorTest.java**
2. **LlmReadinessAutoConfigurationTest.java**
3. **LlmReadinessHealthIndicatorLogicTest.java**
4. **LlmReadinessHealthIntegrationTest.java**


---

## Identifying Independent Dimensions (Domain Classes)

### System Analysis

The `LlmReadinessHealthIndicator` system has **4 independent dimensions**:

```
┌─────────────────────────────────────────────────────────────────┐
│                    COMPLETE SYSTEM                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  D1: Actuator Presence      (Spring Boot Actuator available?)  │
│  D2: Bean Lifecycle         (Bean auto-created or custom?)     │
│  D3: Health Logic           (Valid/invalid API key?)           │
│  D4: Integration            (Endpoint exposed correctly?)      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Why are these INDEPENDENT dimensions?

1. **D1 (Actuator Presence)** is orthogonal to D2/D3/D4 because:
   - Can exist or not exist independently of the logic
   - Does not influence HOW the bean works, only IF it gets created

2. **D2 (Bean Lifecycle)** is orthogonal to D3/D4 because:
   - Concerns bean creation (auto vs custom)
   - Does not influence the internal health check logic

3. **D3 (Health Logic)** is orthogonal to D1/D2/D4 because:
   - Pure logic independent of the framework
   - Works the same whether tested in isolation or integration

4. **D4 (Integration)** is orthogonal to D1/D2/D3 because:
   - Tests the system as a whole
   - Verifies that all parts collaborate correctly

---

## Monodimensional Test Matrix

| Dimension | Test Class | Boundaries Tested | Rationale |
|-----------|------------|-------------------|-----------|
| **D1: Actuator** | `NoActuatorTest` | **OUT**: Actuator absent | Verifies that without Actuator the bean is NOT created |
| **D1: Actuator** | `AutoConfigurationTest` | **IN**: Actuator present | Verifies that with Actuator the bean IS created |
| **D2: Lifecycle** | `AutoConfigurationTest` | **EDGE**: Custom bean provided | Verifies `@ConditionalOnMissingBean` |
| **D3: Logic** | `LogicTest` | **IN**: Valid API key | Health status = UP |
| **D3: Logic** | `LogicTest` | **OUT**: Missing API key | Health status = DOWN |
| **D3: Logic** | `LogicTest` | **EDGE**: Blank/empty API key | Health status = DOWN |
| **D4: Integration** | `IntegrationTest` | **IN**: Full flow with key | Endpoint registered, status UP |
| **D4: Integration** | `IntegrationTest` | **OUT**: Full flow without key | Endpoint registered, status DOWN |

---

## Domain Class Identification for Each Dimension

### Dimension 1: Actuator Presence

**Domain:** Presence of Spring Boot Actuator dependency

```
┌────────────────────────────────────────┐
│     ACTUATOR PRESENCE DOMAIN           │
├────────────────────────────────────────┤
│                                        │
│  OUT (boundary):  Actuator absent      │
│                   ↓                    │
│                   Bean NOT created     │
│                                        │
│  ────────────────────────────────────  │
│  BOUNDARY: HealthIndicator.class       │
│  ────────────────────────────────────  │
│                                        │
│  IN (valid):      Actuator present     │
│                   ↓                    │
│                   Bean created         │
│                                        │
└────────────────────────────────────────┘
```

**Test Coverage:**
- **OUT**: `NoActuatorTest.shouldNotAutoConfigureWhenActuatorMissing()`
- **IN**: `AutoConfigurationTest.shouldAutoConfigureHealthIndicatorWhenActuatorPresent()`

**Why don't we test the exact boundary?**
The boundary is binary (class present/absent), there is no intermediate value.

---

### Dimension 2: Bean Lifecycle

**Domain:** Bean creation mode

```
┌─────────────────────────────────────────────┐
│       BEAN LIFECYCLE DOMAIN                 │
├─────────────────────────────────────────────┤
│                                             │
│  OUT (no bean):    No bean defined          │
│                    ↓                        │
│                    Auto-configuration creates│
│                                             │
│  ─────────────────────────────────────────  │
│  BOUNDARY: @ConditionalOnMissingBean        │
│  ─────────────────────────────────────────  │
│                                             │
│  EDGE (custom):    Custom bean provided     │
│                    ↓                        │
│                    Custom bean used         │
│                                             │
└─────────────────────────────────────────────┘
```

**Test Coverage:**
- **OUT**: `AutoConfigurationTest.shouldAutoConfigureHealthIndicatorWhenActuatorPresent()`
- **EDGE**: `AutoConfigurationTest.shouldNotOverrideCustomHealthIndicator()`

**Why do we test only OUT and EDGE?**
There is no "IN" case where the bean is partially defined. Either it exists (custom) or it doesn't (auto).

---

### Dimension 3: Health Logic (API Key Validation)

**Domain:** DashScope API key validity

```
┌──────────────────────────────────────────────────┐
│         API KEY VALIDATION DOMAIN                │
├──────────────────────────────────────────────────┤
│                                                  │
│  OUT (invalid):   Missing API key (null)         │
│                   ↓                              │
│                   Status: DOWN                   │
│                   Details: ["AI_DASHSCOPE_..."]  │
│                                                  │
│  ────────────────────────────────────────────── │
│  BOUNDARY 1: StringUtils.hasText() → false       │
│  ────────────────────────────────────────────── │
│                                                  │
│  EDGE (boundary): Blank ("   ") or               │
│                   empty ("") API key             │
│                   ↓                              │
│                   Status: DOWN                   │
│                                                  │
│  ────────────────────────────────────────────── │
│  BOUNDARY 2: StringUtils.hasText() → true        │
│  ────────────────────────────────────────────── │
│                                                  │
│  IN (valid):      Valid API key ("key-123")     │
│                   ↓                              │
│                   Status: UP                     │
│                   Details: {provider: dashscope} │
│                                                  │
└──────────────────────────────────────────────────┘
```

**Test Coverage:**
- **OUT**: `LogicTest.shouldReportDownWhenApiKeyIsMissing()`
- **EDGE 1**: `LogicTest.shouldReportDownWhenApiKeyIsBlank()`
- **EDGE 2**: `LogicTest.shouldReportDownWhenApiKeyIsEmpty()`
- **IN**: `LogicTest.shouldReportUpWhenApiKeyIsPresent()`

**Boundary Analysis:**

The `StringUtils.hasText()` function defines two boundaries:

1. **Boundary 1 (null → empty/blank)**
   - Input: `null`
   - Output: `false` → DOWN
   
2. **Boundary 2 (empty/blank → valid)**
   - Input: `""` or `"   "`
   - Output: `false` → DOWN
   
3. **Boundary 3 (invalid → valid)**
   - Input: `"any-non-blank-text"`
   - Output: `true` → UP

**Why do we test empty AND blank separately?**
Although both are EDGE cases, they represent two different implementations of "invalid string":
- Empty (`""`) = length 0
- Blank (`"   "`) = length > 0 but only whitespace

---

### Dimension 4: Integration (Endpoint Exposure)

**Domain:** Health endpoint exposure and functionality

```
┌────────────────────────────────────────────────────┐
│        ENDPOINT INTEGRATION DOMAIN                 │
├────────────────────────────────────────────────────┤
│                                                    │
│  Complete flow with valid configuration:           │
│                                                    │
│  AutoConfiguration → Registry → Endpoint           │
│         ↓               ↓          ↓               │
│    Bean created    Contributor  Health UP/DOWN    │
│                   registered                       │
│                                                    │
│  ──────────────────────────────────────────────── │
│  BOUNDARY: Naming Convention                       │
│  ──────────────────────────────────────────────── │
│                                                    │
│  Bean name: "llmReadinessHealthIndicator"         │
│       ↓                                            │
│  Contributor name: "llmReadiness"                  │
│       ↓                                            │
│  Endpoint path: /actuator/health/llmReadiness      │
│                                                    │
└────────────────────────────────────────────────────┘
```

**Test Coverage:**
- **IN (valid config)**: `IntegrationTest.shouldExposeHealthEndpointWithLlmReadinessWhenApiKeyPresent()`
- **OUT (invalid config)**: `IntegrationTest.shouldExposeHealthEndpointWithDownStatusWhenApiKeyMissing()`
- **BOUNDARY (naming)**: `IntegrationTest.shouldRegisterContributorWithCorrectName()`

**Why do we test naming separately?**
It is a critical boundary of the Spring Boot framework: the bean name determines the contributor name. If incorrect, the endpoint will not be accessible at the correct path.

---

## Monodimensional Testing Principle Applied

### Fundamental Rule

> **For each dimension, we test ONLY the boundaries of that dimension, keeping all other dimensions in a valid state (IN).**

### Practical Example

**Dimension 3 (Health Logic)** - Testing blank API key:

```java
@Test
void shouldReportDownWhenApiKeyIsBlank() {
    // D1: Actuator → IN (present via MockEnvironment)
    // D2: Lifecycle → IN (bean created directly)
    // D3: Health Logic → EDGE (blank API key) ← THIS IS THE TESTED DIMENSION
    // D4: Integration → N/A (unit test)
    
    MockEnvironment env = new MockEnvironment();
    env.setProperty("AI_DASHSCOPE_API_KEY", "   ");  // EDGE case

    LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(env);
    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
}
```

### Counter-example: Combinatorial Test (TO AVOID)

```java
// WRONG: Tests 2 dimensions simultaneously
@Test
void shouldFailWhenActuatorMissingAndApiKeyBlank() {
    // D1: Actuator → OUT (absent)
    // D3: Health Logic → EDGE (blank)
    // This test is REDUNDANT and creates combinatorial explosion
}
```

---

## Coverage Analysis

### Total Coverage per Dimension

```
D1 (Actuator):     100% (OUT + IN)
D2 (Lifecycle):    100% (OUT + EDGE)
D3 (Health Logic): 100% (OUT + EDGE×2 + IN)
D4 (Integration):  100% (IN + OUT + BOUNDARY)
```

### Number of Tests

```
Tests required with monodimensional approach:   10
Tests required with combinatorial approach:     2⁴ × 4 = 64
Reduction:                                      84%
```

### Non-Redundancy Matrix

| Test Class | D1 | D2 | D3 | D4 | Unique Coverage |
|------------|----|----|----|----|-----------------|
| NoActuatorTest | ✓ | - | - | - | Actuator OUT |
| AutoConfigurationTest | ✓ | ✓ | - | - | Actuator IN + Lifecycle EDGE |
| LogicTest | - | - | ✓ | - | API key boundaries |
| IntegrationTest | - | - | - | ✓ | End-to-end flow |

**Non-redundancy verification:** Each test covers a UNIQUE combination of dimensions.

---

## Lessons Learned

### 1. How to Identify Dimensions

**Key question:** "Can this aspect change INDEPENDENTLY from the others?"

- YES → It is a dimension
- NO → It is an implementation detail

**Example:**
- "Actuator present/absent" → YES, Dimension (can change independently)
- "Bean name" → NO, Not a dimension (it is a detail of D2)

### 2. How to Identify Boundaries

**Key question:** "Where does the system behavior change?"

For API key validation:
```
null → false (boundary 1)
"" → false (boundary 2)
"   " → false (boundary 2)
"x" → true (boundary 3)
```

### 3. When to Test an EDGE Case Separately

**Rule:** Test separately if:
1. It represents a different implementation (empty vs blank)
2. It could fail independently (null handling vs string parsing)

**Example:**
- Empty and Blank → Separate tests
- Blank with 1 space vs 10 spaces → Same test

---

## Maintainability Over Time

### When to Add New Tests?

**Scenario:** We add support for OpenAI API key

```java
// New logic in LlmReadinessHealthIndicator
String openaiKey = environment.getProperty("OPENAI_API_KEY");
if (!StringUtils.hasText(openaiKey)) {
    missing.add("OPENAI_API_KEY");
}
```

**Required tests:**
- NO new tests needed in D1, D2, D4
- New tests needed ONLY in D3 (LogicTest)

```java
@Test
void shouldReportDownWhenBothKeysAreMissing() {
    MockEnvironment env = new MockEnvironment();
    // Test boundary: no key present
}

@Test
void shouldReportUpWhenAtLeastOneKeyIsPresent() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("OPENAI_API_KEY", "key");
    // Test boundary: one valid key
}
```

### Refactoring Safety

**When we change internal implementation:**
- Tests for D1, D2, D4 → Continue to pass
- Only tests for D3 → May require modifications

**Example:** We change from `StringUtils.hasText()` to custom validation:
- **Impact:** Only `LogicTest` requires update
- **Protected:** All other tests

---

## Checklist for New Tests

Before adding a new test, verify:

- [ ] Does the test cover a new dimension not yet tested?
- [ ] Does the test cover a new boundary of an existing dimension?
- [ ] Is the test NOT a combination of already tested dimensions?
- [ ] Can the test fail independently from others?

If the answer is NO to all, the test is probably redundant.

---

## Conclusion

The monodimensional approach applied to `LlmReadinessHealthIndicator` guarantees:

- **Complete coverage** (100% of boundaries)  
- **Zero redundancy** (each test has a unique purpose)  
- **Maintainability** (localized changes)  
- **Efficiency** (84% fewer tests vs combinatorial)  
- **Comprehensibility** (each test has a clear purpose)

**Files to maintain: 4 test classes, 10 total test methods.**

---

## Test Design Principles

### Independence Principle
Each test class focuses on a single dimension of the system. This ensures that:
- Changes to one dimension do not cascade to unrelated tests
- Test failures immediately identify the problematic dimension
- New features can be added by extending only the relevant test class

### Boundary Coverage Principle
For each dimension, we identify and test:
- **OUT boundaries**: Invalid states that should be rejected
- **IN boundaries**: Valid states that should be accepted
- **EDGE boundaries**: Corner cases at the transition points

### Non-Redundancy Principle
Before adding a test, we verify it provides unique coverage:
- Does it test a boundary not covered by existing tests?
- Does it test a dimension in isolation?
- Would removing it leave a gap in coverage?

### Maintainability Principle
The test suite should be:
- Easy to understand: Clear naming and organization
- Easy to modify: Changes localized to specific test classes
- Easy to extend: New dimensions can be added without refactoring existing tests

---

## Implementation Guidelines

### Test Naming Convention
Tests follow the pattern: `should[ExpectedBehavior]When[Condition]`

Examples:
- `shouldNotAutoConfigureWhenActuatorMissing()`
- `shouldReportDownWhenApiKeyIsBlank()`
- `shouldExposeHealthEndpointWithLlmReadinessWhenApiKeyPresent()`

### Test Structure
Each test follows the Arrange-Act-Assert pattern:

```java
@Test
void shouldReportDownWhenApiKeyIsBlank() {
    // Arrange: Set up the test state
    MockEnvironment env = new MockEnvironment();
    env.setProperty("AI_DASHSCOPE_API_KEY", "   ");
    
    // Act: Execute the behavior under test
    LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(env);
    Health health = indicator.health();
    
    // Assert: Verify the expected outcome
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
}
```

### Test Class Organization

**LlmReadinessAutoConfigurationNoActuatorTest**
- Purpose: Test D1 OUT boundary (Actuator absent)
- Scope: Auto-configuration behavior without Actuator dependency
- Setup: Minimal Spring context without Actuator on classpath

**LlmReadinessAutoConfigurationTest**
- Purpose: Test D1 IN boundary and D2 EDGE boundary
- Scope: Auto-configuration behavior with Actuator present
- Setup: Full Spring context with Actuator dependency

**LlmReadinessHealthIndicatorLogicTest**
- Purpose: Test D3 boundaries (API key validation logic)
- Scope: Pure logic testing in isolation
- Setup: Direct instantiation with MockEnvironment

**LlmReadinessHealthIntegrationTest**
- Purpose: Test D4 boundaries (end-to-end integration)
- Scope: Full Spring Boot application context
- Setup: `@SpringBootTest` with web environment

---

## Final Test Suite Summary

### Test Distribution by Dimension

| Dimension | Test Class | Test Count | Coverage |
|-----------|-----------|------------|----------|
| D1: Actuator Presence | NoActuatorTest | 1 | OUT boundary |
| D1: Actuator Presence | AutoConfigurationTest | 1 | IN boundary |
| D2: Bean Lifecycle | AutoConfigurationTest | 1 | EDGE boundary |
| D3: Health Logic | LogicTest | 4 | OUT, EDGE×2, IN boundaries |
| D4: Integration | IntegrationTest | 3 | IN, OUT, BOUNDARY |
| **Total** | **4 classes** | **10 tests** | **100% boundary coverage** |

### Verification Matrix

```
Dimension 1 (Actuator):
  OUT: Actuator absent → Bean not created          [COVERED]
  IN:  Actuator present → Bean created             [COVERED]

Dimension 2 (Lifecycle):
  OUT: No custom bean → Auto-configuration active  [COVERED]
  EDGE: Custom bean → Auto-configuration skipped   [COVERED]

Dimension 3 (Health Logic):
  OUT:  API key null → Status DOWN                 [COVERED]
  EDGE: API key empty → Status DOWN                [COVERED]
  EDGE: API key blank → Status DOWN                [COVERED]
  IN:   API key valid → Status UP                  [COVERED]

Dimension 4 (Integration):
  IN:       Valid config → Endpoint UP             [COVERED]
  OUT:      Invalid config → Endpoint DOWN         [COVERED]
  BOUNDARY: Naming convention → Correct path       [COVERED]
```

### Redundancy Check

All eliminated test files were verified to be redundant:
- `LlmReadinessHealthEndpointIdTest` → Naming verification covered in Integration Test
- `LlmReadinessHealthIndicatorBehaviorTest` → Bean behavior covered in AutoConfiguration Test
- `LlmReadinessHealthIndicatorPresenceTest` → Bean presence covered in AutoConfiguration Test

No unique coverage was lost by removing these files.

---

## Conclusion: Professional Testing Strategy

This monodimensional testing strategy represents a rigorous, systematic approach to test design that balances comprehensive coverage with maintainability. By identifying the four independent dimensions of the system and testing each dimension's boundaries in isolation, we achieve:

**Quantitative Benefits:**
- 100% boundary coverage across all dimensions
- 84% reduction in test count compared to combinatorial approach
- Zero redundant test cases

**Qualitative Benefits:**
- Clear mapping between tests and system dimensions
- Predictable impact analysis when requirements change
- Self-documenting test suite through systematic organization

**Engineering Excellence:**
- Adherence to SOLID principles in test design
- Scalable approach as system complexity grows
- Foundation for continuous integration and delivery

This approach demonstrates that effective testing is not about maximizing test count, but about strategic coverage of critical system boundaries through independent, focused test cases.
