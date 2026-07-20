# MERGE CHECKLIST

## Purpose

This checklist defines the minimum conditions that must be satisfied before merging any engineering phase into the `main` branch.

The goal is to ensure that every completed phase is stable, maintainable, fully documented, and ready to become the foundation for subsequent work.

Every checklist item should be answered with one of:

* ✅ YES
* ❌ NO
* N/A (only when explicitly justified)

If any mandatory item is marked ❌, the phase must not be merged.

---

# 1. Scope

## Scope Completed

* [ ] All objectives defined in the current Phase specification have been completed.
* [ ] No objectives from future phases have been implemented.
* [ ] No unrelated functionality has been added.
* [ ] The implementation matches the approved scope.

---

# 2. Architecture

## Architecture Review

* [ ] Architecture Review has been completed.
* [ ] No Blocking Issues remain.
* [ ] Any Recommended Improvements have been documented.
* [ ] The implementation follows `MASTER_PROMPT.md`.
* [ ] Responsibility boundaries remain clear.
* [ ] No unnecessary abstractions have been introduced.
* [ ] No architectural regressions were identified.

---

# 3. Code Quality

## General

* [ ] Code is readable.
* [ ] Naming is consistent.
* [ ] No duplicated business logic.
* [ ] No dead code.
* [ ] No commented-out code.
* [ ] No temporary code remains.
* [ ] No debugging statements remain.
* [ ] No TODO/FIXME comments remain without justification.

---

# 4. Dependency Review

* [ ] Dependency direction is correct.
* [ ] No circular dependencies.
* [ ] No hidden framework coupling.
* [ ] Public APIs remain stable.
* [ ] No unnecessary dependencies were introduced.

---

# 5. Backward Compatibility

* [ ] Public REST API preserved.
* [ ] JSON contracts preserved.
* [ ] Validation behavior preserved.
* [ ] Existing finding codes preserved.
* [ ] Existing finding messages preserved.
* [ ] Existing endpoint behavior preserved.
* [ ] Existing consumers remain compatible.

---

# 6. Tests

* [ ] Existing tests pass.
* [ ] New tests added where appropriate.
* [ ] Regression tests pass.
* [ ] Integration tests pass.
* [ ] Rule-level tests pass (if applicable).
* [ ] No failing tests.
* [ ] No skipped tests without justification.

---

# 7. Build

* [ ] Clean build succeeds.
* [ ] Full test suite succeeds.
* [ ] `git diff --check` reports no whitespace issues.
* [ ] No generated artifacts are accidentally committed.

---

# 8. Documentation

* [ ] Relevant documentation updated.
* [ ] Architecture documentation updated.
* [ ] README updated if required.
* [ ] Phase documentation reflects the implementation.
* [ ] Documentation contains no planned-but-unimplemented behavior.

---

# 9. Repository Hygiene

* [ ] No unrelated files modified.
* [ ] No accidental file deletions.
* [ ] No temporary resources committed.
* [ ] No IDE-specific files committed.
* [ ] Project structure remains consistent.

---

# 10. Technical Debt

* [ ] Remaining Technical Debt documented.
* [ ] Deferred improvements explicitly identified.
* [ ] Technical debt does not block future phases.

---

# 11. Final Verification

Confirm the following statements.

* [ ] This implementation is production-quality.
* [ ] The architecture is suitable for future phases.
* [ ] No known Blocking Issues remain.
* [ ] The repository is ready to become the new baseline.

---

# Merge Decision

Choose exactly one.

* ☐ APPROVED FOR MERGE
* ☐ APPROVED WITH DOCUMENTED DEBT
* ☐ MERGE BLOCKED

If merge is blocked, list every blocking issue.

If merge is approved with debt, document every deferred improvement.

---

# Post-Merge Actions

After a successful merge:

* Create a merge commit (or follow the repository merge strategy).
* Update the project baseline.
* Delete the feature branch when appropriate.
* Create the next feature branch from the updated `main`.
* Begin the next engineering phase only after the new baseline is confirmed.