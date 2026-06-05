# Ask Mode - Non-Obvious Documentation Context

This file contains project-specific documentation patterns discovered by reading the codebase.

## Project Structure Counterintuitive Aspects

### Synthetic POC Nature
This is NOT production code - it's a deliberately constructed benchmark codebase for evaluating AI coding tools. Contains INTENTIONAL defects for testing detection capabilities.

### Maximo Pattern Simulation
The project mimics IBM Maximo architecture patterns WITHOUT using actual Maximo libraries (which are proprietary). This means:
- MBO classes simulate `psdi.mbo.Mbo` behavior but don't extend it
- No actual Maximo runtime - just the patterns
- H2 in-memory database instead of real enterprise DB

### Documentation Locations

**Primary docs**: `docs/` directory contains:
- `architecture.md` - Detailed layer-by-layer architecture explanation
- `conventions.md` - High-level coding conventions summary

**Enforceable rules**: `.bob/rules/` directory contains:
- `error-handling.md` - Exception patterns, error codes
- `naming-conventions.md` - Class naming by layer
- `mbo-access-patterns.md` - MBO lifecycle, DAO patterns
- `security-rules.md` - Security requirements

### Intentional Defects Are Documented
README.md explicitly states that defects are planted for benchmark evaluation. When finding issues:
- Security violations in `application.properties` are INTENTIONAL
- Missing `Bf` prefix on some classes is INTENTIONAL
- Incomplete test coverage is INTENTIONAL

## Layer Responsibilities (Non-Obvious)

### MBO Layer Owns Business Logic
Unlike typical service-oriented architectures, MBOs contain significant business logic (validations, state transitions). Services orchestrate but don't duplicate MBO logic.

### DAO Returns Records, Not Entities
DAOs return immutable `record` types for read operations. The Service layer transforms these into MBOs when needed. This is NOT standard JPA/Hibernate pattern.

### No Dependency Injection Framework
Despite being Java, there's NO Spring/CDI. Dependencies are passed via constructors. This mimics Maximo's approach.

## Test Organization

### Tests Mirror Source Structure
Test classes are in `src/test/java/` mirroring the package structure of `src/main/java/`. NOT in a separate test module.

### Partial Coverage Is Intentional
Test coverage is deliberately incomplete to allow AI tools to suggest missing tests. Don't assume missing tests are an oversight.

## Automation Scripts

### Scripts Are Documentation Only
Files in `src/main/java/com/borneflow/automation/` are JavaScript/Python files showing WHAT automation would do. They don't actually execute in this POC.

## French Language in Rules
The detailed rules in `.bob/rules/` are written in French (project context: French company CGI). English summaries are in main AGENTS.md files.