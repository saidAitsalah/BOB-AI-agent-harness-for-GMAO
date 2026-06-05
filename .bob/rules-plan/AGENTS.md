# Plan Mode - Non-Obvious Architectural Constraints

This file contains project-specific architectural patterns discovered by reading the codebase.

## Layered Architecture Constraints

### MBO-Centric Design (Not Service-Centric)
Unlike modern microservices, business logic lives primarily in MBOs, not Services. Services orchestrate MBO interactions but don't duplicate validation logic.

**Implication**: When planning new features, start with MBO design, not service design.

### Strict Layer Separation
- **API Layer** → calls Services only, never DAOs or MBOs directly
- **Service Layer** → orchestrates MBOs, calls DAOs for reads
- **MBO Layer** → contains business rules, never calls DAOs
- **DAO Layer** → pure data access, returns records not MBOs

**Violation example**: Service calling DAO to get a record, then manually populating an MBO. Instead, Service should use MBO factory methods.

## State Machine Patterns

### Status Transitions Are Enforced in MBOs
`ChargingStationMbo.changeStatus()` enforces valid transitions via switch expression. This pattern MUST be used for all stateful entities.

**Don't**: Validate transitions in Service layer
**Do**: Implement `changeStatus()` method in MBO with transition rules

### Status Transition Matrix
```
COMMISSIONED → OPERATIONAL, DECOMMISSIONED
OPERATIONAL → MAINTENANCE, DECOMMISSIONED  
MAINTENANCE → OPERATIONAL, DECOMMISSIONED
DECOMMISSIONED → (terminal state)
```

## Data Access Patterns

### Record-to-MBO Transformation Happens in Service
DAOs return immutable `record` types. Services transform these to MBOs when mutation is needed. This keeps:
- DAOs independent of MBO lifecycle
- MBOs independent of database schema
- Clear separation between read projections and mutable entities

### No ORM Framework
This is deliberate - Maximo doesn't use JPA/Hibernate. All SQL is hand-written with PreparedStatements.

**Implication**: When planning data access, budget time for manual SQL and result mapping.

## Testing Strategy

### Test Coverage Gaps Are Intentional
Missing tests are planted defects for AI tool evaluation. When planning test additions:
- Focus on critical paths (MBO validation, state transitions)
- Use `should<Behavior>_when<Condition>` naming
- Mock dependencies with Mockito

### No Integration Tests
Only unit tests exist. H2 in-memory database is used within unit tests, not separate integration test suite.

## Security Architecture

### Authorization at API Boundary
`AuthorizationContext.can*()` checks MUST happen in Resource classes before delegating to Services. Services assume authorization already checked.

**Critical**: Never add a new REST endpoint without authorization check.

### Secrets Management Pattern
Environment variables (`${VAR_NAME}`) should be used for secrets, but `application.properties` contains hardcoded secrets INTENTIONALLY for benchmark testing.

## Extension Points

### Automation Scripts Are Placeholders
JavaScript/Python files in `automation/` directory show WHERE automation would hook in, but don't execute. When planning automation:
- Document trigger points (status change, save, delete)
- Specify which MBO attributes are accessed
- Note external system integrations

## Performance Considerations

### H2 In-Memory Resets Between Runs
No data persistence between application restarts. This is acceptable for POC but would need addressing for production planning.

### No Caching Layer
Services call DAOs on every request. For production planning, consider caching strategy for frequently accessed reference data.

## Intentional Technical Debt

### Missing Bf Prefix on Some Classes
`ChargingStationMbo`, `MaintenanceOrderMbo` lack `Bf` prefix deliberately. When planning refactoring, these are KNOWN issues for benchmark testing.

### Incomplete Validation
Some MBOs have partial validation. This is intentional - allows AI tools to suggest missing validations.