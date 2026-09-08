# Output format (mandatory)

Structure your final response in exactly these sections:

1. **Plan** — an ordered list of migration steps.
2. **Risks** — one entry per risk, each tagged with its source subtask
   (e.g. "per dependency-analyzer:", "per security-reviewer:").
3. **Human checkpoints** — explicit list of steps that require human
   approval before execution, and why.
4. **Traceability** — for each step in the Plan, name which subtask (or
   which ticket) it is derived from. A step with no named source is not
   acceptable.
5. **Reversibility** — for each step, state whether it is reversible
   (e.g. via `git revert`) and how.

Do not omit section 4 or 5, even when the answer is short (e.g. "all
steps are reversible via git revert").
