# Impact Evidence Minimal Vertical Slice Evaluation

**Status:** preliminary engineering evaluation  
**Verdict:** **VALUE SIGNAL OBSERVED**

## Fixtures and comparison

The automated fixtures in `StructuralPathBaselineTest` execute both
interpretations with the same accepted `VerifiedChangeSet` and frozen manifest:

| Fixture | Structural path/no-path plus warnings baseline | Implemented classifier |
|---|---|---|
| Directly changed subject also reachable by a path | A path can be shown, with no defined precedence | `AFFECTED`; exact declaration and accepted change instance win through `DirectChangeProof` |
| Qualified `BR-BASE -> BR-MID -> BR-S` propagation | Path exists | `AFFECTED`; ordered `RelationshipPathProof` retains both datum IDs and propagation direction |
| No path in the supplied snapshot | No path; a consumer may read this as no impact | `UNKNOWN / NO_QUALIFIED_IMPACT_PROOF` |
| Subject identity has no mapping | No path plus identity warning | `UNKNOWN / UNRESOLVED_SUBJECT_IDENTITY` |
| Apparent path has wrong snapshot and missing provenance | Path exists plus warnings unless the caller interprets them correctly | Edge is excluded; `UNKNOWN` retains both rejection reasons in stable order |

The baseline is deliberately a small test-only function, not a second
production implementation. It traverses structurally resolvable relationships
without qualification gating and reports snapshot/provenance defects as
warnings. Assertions compare its path/direct outcome with the classifier for
qualified path, no path, unresolved subject, wrong snapshot, missing
provenance, and direct-plus-path fixtures.

## Observations

The classifier prevents two false-certainty transitions present in the
baseline: no path cannot be projected as non-impact, and a structural path
cannot prove impact when its evidence fails qualification. Positive results
are more explainable because the selected direct declaration or every path
datum remains in the result. Debugging is more local: rejected datum IDs carry
all applicable stable reasons instead of requiring correlation with a warning
stream.

The semantic advantage costs a compact set of immutable contracts, manifest
fingerprinting, fixed qualification predicates, and an evidence-specific BFS.
The implementation adds no generic graph, rule, proof, provenance, storage, or
integration framework. Deterministic replay tests show that shuffled manifest
construction order does not change the fingerprint or result.

## Preliminary verdict

**VALUE SIGNAL OBSERVED.** The implemented fixtures demonstrate materially
safer uncertainty and auditable positive proof than structural path/no-path
plus warnings. This is engineering evidence only; no user study or production
readiness claim has been made.
