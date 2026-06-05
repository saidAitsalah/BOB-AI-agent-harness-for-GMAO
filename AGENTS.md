# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build & Test Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=ChargingStationMboTest

# Run single test method
mvn test -Dtest=ChargingStationMboTest#shouldRejectSave_whenExternalIdMissing
```

## Critical Non-Obvious Patterns

### MBO Lifecycle Hook Chain
MBOs MUST call `add() → validate() → save()` in order. `save()` automatically calls `validate()`, but if overriding `save()`, MUST call `super.save()` first. Never call `save()` without prior `validate()`.

### DAO Returns Records, Not MBOs
DAOs return Java `record` types (read projections). The `record → MBO` transformation happens in Service layer, NOT in DAO. This keeps MBOs independent of DB schema.

### Naming Convention Violations Are Intentional
Some classes violate the `Bf` prefix rule (e.g., `ChargingStationMbo` instead of `BfChargingStationMbo`). This is DELIBERATE for benchmark testing - do not "fix" without explicit request.

### Security Violations Are Planted
`application.properties` contains hardcoded secrets (lines 14-15). This is INTENTIONAL for security scanning benchmarks. Flag them but don't remove without confirmation.

### Exception Logging Pattern
Always log exceptions as: `log.error("Operation failed for entityId={}", id, exception)` - exception as LAST parameter (SLF4J pattern), never concatenate `e.getMessage()`.

### SQL Injection Prevention
ALL SQL queries MUST use `PreparedStatement` with `?` placeholders. String concatenation in SQL (`"... '" + var + "'"`) is FORBIDDEN and will be flagged as critical security issue.

### Authorization Check Pattern
Every REST Resource method (`@GET`, `@POST`, `@PUT`, `@DELETE`) MUST call `authz.can*()` BEFORE executing business logic. Missing checks are critical security findings.

### Personal Data Logging Ban
NEVER log: `matricule` (technician ID), email, full names, session tokens, auth tokens. Only log technical IDs (`id`, `externalId` for assets).

### Test Naming Convention
Tests use `should<Behavior>_when<Condition>` pattern (e.g., `shouldRejectOrder_whenStationIsOffline`), not `test*` or `*Test` suffixes on method names.

## Project Context

- **Synthetic POC**: No real client code, simulates Maximo patterns for AI tool benchmarking
- **Intentional defects**: Security issues, naming violations, incomplete tests are PLANTED for evaluation
- **Java 17 features**: Use records, switch expressions, text blocks, var - avoid Java 8 style
- **No Spring**: Deliberately minimal, no DI framework (Maximo doesn't use Spring)
- **H2 in-memory**: Database resets between runs, no persistence

## Custom Rules Location

Detailed enforceable rules are in `.bob/rules/`:
- `error-handling.md` - Exception patterns, error codes (`BF_*` prefix)
- `naming-conventions.md` - Class prefixes/suffixes by layer
- `mbo-access-patterns.md` - MBO hooks, DAO patterns, SQL rules
- `security-rules.md` - Injection prevention, secrets, authorization, PII logging