# Code Mode - Non-Obvious Rules

This file contains project-specific coding rules discovered by reading the codebase.

## MBO Lifecycle Enforcement

### Hook Call Order is Mandatory
MBOs MUST call `add() → validate() → save()` in strict order. The base `BfMbo.save()` calls `validate()` automatically, but if you override `save()`, you MUST call `super.save()` first or the validation will be skipped.

**Critical**: Never call `save()` directly without ensuring `validate()` runs. This is NOT enforced by the compiler.

### Attribute Access Pattern
MBOs use generic `getAttribute(String)` / `setAttribute(String, Object)` instead of typed getters/setters. This mimics Maximo's dynamic attribute system. When reading attributes, always cast to the expected type and handle null.

## DAO-to-Service Transformation Pattern

### Records Are Read-Only Projections
DAOs return Java `record` types (e.g., `StationRecord`, `OrderRecord`). These are NEVER converted to MBOs inside the DAO - that transformation happens in the Service layer.

**Why**: Keeps MBOs independent of database schema. If you add a DAO method, return a `record`, not an MBO.

### PreparedStatement is Non-Negotiable
ALL SQL queries MUST use `PreparedStatement` with `?` placeholders. String concatenation like `"WHERE id = '" + id + "'"` is a CRITICAL security violation and will fail code review.

**Pattern to follow**: See `ChargingStationDao.findByExternalId()` for reference implementation.

## Exception Handling Specifics

### SLF4J Exception Parameter Position
When logging exceptions, the exception MUST be the LAST parameter: `log.error("Failed for id={}", id, exception)`. Never concatenate `e.getMessage()` into the log string - this loses the stack trace.

### BorneflowException Error Codes
All error codes use `BF_<DOMAIN>_<REASON>` format:
- `BF_STATION_INVALID` - station validation failed
- `BF_STATION_INVALID_TRANSITION` - illegal status transition
- `BF_MO_INVALID` - maintenance order validation failed
- `BF_TECH_NOT_FOUND` - technician lookup failed
- `BF_DAO_ERROR` - database access error

## Security Patterns

### Authorization Context Required
Every REST Resource method (`@GET`, `@POST`, `@PUT`, `@DELETE`) MUST call `authz.can*()` before executing business logic. This is NOT enforced by framework - you must remember to add it.

### PII Logging Blacklist
NEVER log these fields in clear text:
- `matricule` (technician employee ID)
- Email addresses
- Full person names
- Session tokens
- Auth tokens

Only log technical IDs like `id` or `externalId` for assets.

## Java 17 Patterns Used

### Switch Expressions for State Machines
Status transition logic uses switch expressions (see `ChargingStationMbo.isTransitionAllowed()`). Use this pattern instead of if/else chains for state validation.

### Records for DTOs
All DAO return types are `record` classes. Never create mutable DTO classes with getters/setters for database projections.

### Text Blocks for SQL
Multi-line SQL uses text blocks (`"""..."""`). This is the standard for all SQL in this project.

## Test Patterns

### Test Method Naming
Tests use `should<Behavior>_when<Condition>` pattern:
- `shouldRejectSave_whenExternalIdMissing()`
- `shouldAllowTransitionCommissionedToOperational()`

NOT `testSave()` or `saveTest()`.

### Test Setup Pattern
Use `@BeforeEach` to create and initialize MBOs with `add()` called. This ensures tests start with properly initialized objects.

## Intentional Violations (DO NOT FIX)

### Missing Bf Prefix
`ChargingStationMbo`, `ChargingStationMboSet`, `MaintenanceOrderMbo` deliberately lack the `Bf` prefix. This is for benchmark testing - flag them but don't rename without explicit approval.

### Hardcoded Secrets
`application.properties` lines 14-15 contain hardcoded secrets. This is INTENTIONAL for security scanning benchmarks. Flag but don't remove.