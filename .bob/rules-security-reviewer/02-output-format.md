# Output format (mandatory)

For every issue found, use exactly this format:

```
[CRITICALITY] Short title — file:line
Finding: what the vulnerable pattern is and why it is exploitable.
Impact: what an attacker action or compliance failure would cause.
Recommendation: a concrete, specific fix (not "add validation").
```

Classify CRITICALITY as CRITICAL / HIGH / MEDIUM / LOW. List issues most
critical first. If you find nothing in a given category, do not invent a
finding to fill space — report only what you actually observe.
