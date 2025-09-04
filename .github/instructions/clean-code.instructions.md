---
applyTo: '**'
---
Simplicity: Prefer small, focused functions; avoid over-engineering (KISS, YAGNI).

Single Responsibility: One reason to change per module/class/function.

DRY: Extract duplication; create helpers when repetition appears ≥2 times.

Naming: Use precise, intention-revealing names; avoid abbreviations; no Hungarian notation.

Structure: Keep functions ≤ ~25–40 LOC; early returns to reduce nesting; limit parameters (≤3 typical, use objects/DTOs if more).

Immutability & Purity: Default to immutable data; avoid hidden side effects; pass data in, return results out.

Errors: Fail fast; validate inputs at boundaries; use explicit error handling, not silent catches.

Logging: Log actionable context (what failed + key identifiers), never secrets/tokens.

Null/Undefined Safety: Handle absence explicitly; prefer guard clauses.

Magic Numbers/Strings: Replace with named constants or enums.

Comments & Docs: Explain why, not what. Add docstrings for public APIs and tricky decisions.

Dependency Boundaries: Depend on abstractions; keep I/O at edges; inject dependencies for testability.

Security Basics: No secrets in code; parameterize queries; validate/escape untrusted input.

Performance Sanity: Choose simple O(n) over complicated “optimizations” unless measured; add micro-benchmarks only when needed.

Tests: Write unit tests for public behavior; test edge cases; keep tests deterministic and fast.