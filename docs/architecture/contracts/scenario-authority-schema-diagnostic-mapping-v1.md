# Scenario Authority Schema Diagnostic Mapping V1

## Status

Normative subordinate contract for ADR-015.

## Contract identity and scope

The contract identifier is:

```text
scenario-authority-schema-diagnostic-mapping-v1
```

This contract freezes the QAIP-owned mapping from validation failures of the
immutable `qaip-scenario-authority-manifest-v1` JSON Schema, identified by
`qaip-scenario-authority-manifest-schema-v1`, to canonical Scenario Authority
schema diagnostics. It specializes the schema-diagnostic canonicalization
requirements in ADR-015. It does not define JSON parsing, authority
attribution, schema admission, attributed-member fingerprinting, or validator
presentation diagnostics.

An implementation conforms only when it maps every observed validator result
completely through this finite contract before producing canonical rejected
evidence. Validator output is adapter input, never canonical evidence.

## Stable diagnostic record

Each mapped violation produces this logical record:

```text
StableScenarioSchemaDiagnosticV1 {
  diagnosticContractVersion
  stableDiagnosticCode
  instanceLocation
  normativeSchemaKeyword
  schemaRuleId
  typedParameters[]
}
```

The fields are frozen as follows:

- `diagnosticContractVersion` is
  `scenario-authority-schema-diagnostic-v1`.
- `stableDiagnosticCode` is `SCHEMA_VIOLATION`.
- `instanceLocation` is an RFC 6901 JSON Pointer under the rules below.
- `normativeSchemaKeyword` is the exact JSON Schema 2020-12 keyword in the
  mapping table.
- `schemaRuleId` is the exact QAIP-owned identifier in the mapping table.
- `typedParameters` contains exactly the parameter names, type codes, and
  values prescribed for the keyword. No additional parameter is permitted.

The mapping contract identifier is not substituted for
`diagnosticContractVersion`. The former versions the schema-to-diagnostic
adapter table; the latter versions the canonical diagnostic record consumed by
ADR-015 fingerprint domains.

## Rule identifier form

Every V1 rule identifier has this form:

```text
qaip-scenario-authority-manifest-schema-v1#<schema JSON Pointer>
```

The fragment is the canonical JSON Pointer to the exact keyword-bearing node
in `qaip-scenario-authority-manifest-v1.schema.json`. The schema `$id`, a
retrieval URI, a validator-rendered schema location, or a resolved `$ref` URI
is never a rule identifier.

## Complete V1 rule mapping

The V1 vocabulary contains exactly 36 rules. A rule's parameter binding names
the fixed schema value or the deterministic value derived from the validated
instance. `property` means one property name per expanded diagnostic. Indexes
are unsigned, zero-based array indexes.

| # | QAIP schema-rule identifier | Keyword | Canonical parameter binding |
| ---: | --- | --- | --- |
| 1 | `qaip-scenario-authority-manifest-schema-v1#/type` | `type` | `expectedType: TEXT = "object"` |
| 2 | `qaip-scenario-authority-manifest-schema-v1#/additionalProperties` | `additionalProperties` | `unexpectedProperty: TEXT = property` |
| 3 | `qaip-scenario-authority-manifest-schema-v1#/required` | `required` | `missingProperty: TEXT = property` |
| 4 | `qaip-scenario-authority-manifest-schema-v1#/properties/format/const` | `const` | `expectedText: TEXT = "qaip-scenario-authority-manifest-v1"` |
| 5 | `qaip-scenario-authority-manifest-schema-v1#/properties/schemaVersion/const` | `const` | `expectedText: TEXT = "1.0"` |
| 6 | `qaip-scenario-authority-manifest-schema-v1#/properties/scenarioIdentityScheme/const` | `const` | `expectedText: TEXT = "qaip-scenario-identity-v1"` |
| 7 | `qaip-scenario-authority-manifest-schema-v1#/properties/scenarios/type` | `type` | `expectedType: TEXT = "array"` |
| 8 | `qaip-scenario-authority-manifest-schema-v1#/$defs/authority/type` | `type` | `expectedType: TEXT = "string"` |
| 9 | `qaip-scenario-authority-manifest-schema-v1#/$defs/authority/minLength` | `minLength` | `minimumCodePointLength: UINT64 = 1` |
| 10 | `qaip-scenario-authority-manifest-schema-v1#/$defs/authority/maxLength` | `maxLength` | `maximumCodePointLength: UINT64 = 200` |
| 11 | `qaip-scenario-authority-manifest-schema-v1#/$defs/authority/pattern` | `pattern` | `requiredPattern: TEXT = "^[A-Za-z0-9][A-Za-z0-9._:/-]*$"` |
| 12 | `qaip-scenario-authority-manifest-schema-v1#/$defs/stableKey/type` | `type` | `expectedType: TEXT = "string"` |
| 13 | `qaip-scenario-authority-manifest-schema-v1#/$defs/stableKey/minLength` | `minLength` | `minimumCodePointLength: UINT64 = 1` |
| 14 | `qaip-scenario-authority-manifest-schema-v1#/$defs/stableKey/maxLength` | `maxLength` | `maximumCodePointLength: UINT64 = 160` |
| 15 | `qaip-scenario-authority-manifest-schema-v1#/$defs/stableKey/pattern` | `pattern` | `requiredPattern: TEXT = "^[A-Za-z0-9][A-Za-z0-9._:-]*$"` |
| 16 | `qaip-scenario-authority-manifest-schema-v1#/$defs/identityScheme/type` | `type` | `expectedType: TEXT = "string"` |
| 17 | `qaip-scenario-authority-manifest-schema-v1#/$defs/identityScheme/minLength` | `minLength` | `minimumCodePointLength: UINT64 = 1` |
| 18 | `qaip-scenario-authority-manifest-schema-v1#/$defs/identityScheme/maxLength` | `maxLength` | `maximumCodePointLength: UINT64 = 160` |
| 19 | `qaip-scenario-authority-manifest-schema-v1#/$defs/identityScheme/pattern` | `pattern` | `requiredPattern: TEXT = "^[A-Za-z0-9][A-Za-z0-9._:-]*$"` |
| 20 | `qaip-scenario-authority-manifest-schema-v1#/$defs/nonBlankString/type` | `type` | `expectedType: TEXT = "string"` |
| 21 | `qaip-scenario-authority-manifest-schema-v1#/$defs/nonBlankString/minLength` | `minLength` | `minimumCodePointLength: UINT64 = 1` |
| 22 | `qaip-scenario-authority-manifest-schema-v1#/$defs/nonBlankString/pattern` | `pattern` | `requiredPattern: TEXT = ".*\\S.*"` |
| 23 | `qaip-scenario-authority-manifest-schema-v1#/$defs/steps/type` | `type` | `expectedType: TEXT = "array"` |
| 24 | `qaip-scenario-authority-manifest-schema-v1#/$defs/steps/minItems` | `minItems` | `minimumItemCount: UINT64 = 1` |
| 25 | `qaip-scenario-authority-manifest-schema-v1#/$defs/scenario/type` | `type` | `expectedType: TEXT = "object"` |
| 26 | `qaip-scenario-authority-manifest-schema-v1#/$defs/scenario/additionalProperties` | `additionalProperties` | `unexpectedProperty: TEXT = property` |
| 27 | `qaip-scenario-authority-manifest-schema-v1#/$defs/scenario/required` | `required` | `missingProperty: TEXT = property` |
| 28 | `qaip-scenario-authority-manifest-schema-v1#/$defs/scenario/properties/ruleRefs/type` | `type` | `expectedType: TEXT = "array"` |
| 29 | `qaip-scenario-authority-manifest-schema-v1#/$defs/scenario/properties/ruleRefs/uniqueItems` | `uniqueItems` | `firstIndex: UINT64 = lowest equivalent index`; `duplicateIndex: UINT64 = later equivalent index` |
| 30 | `qaip-scenario-authority-manifest-schema-v1#/$defs/httpOperationReference/type` | `type` | `expectedType: TEXT = "object"` |
| 31 | `qaip-scenario-authority-manifest-schema-v1#/$defs/httpOperationReference/additionalProperties` | `additionalProperties` | `unexpectedProperty: TEXT = property` |
| 32 | `qaip-scenario-authority-manifest-schema-v1#/$defs/httpOperationReference/required` | `required` | `missingProperty: TEXT = property` |
| 33 | `qaip-scenario-authority-manifest-schema-v1#/$defs/httpOperationReference/properties/identityScheme/const` | `const` | `expectedText: TEXT = "qaip-http-operation-reference-v1"` |
| 34 | `qaip-scenario-authority-manifest-schema-v1#/$defs/businessRuleReference/type` | `type` | `expectedType: TEXT = "object"` |
| 35 | `qaip-scenario-authority-manifest-schema-v1#/$defs/businessRuleReference/additionalProperties` | `additionalProperties` | `unexpectedProperty: TEXT = property` |
| 36 | `qaip-scenario-authority-manifest-schema-v1#/$defs/businessRuleReference/required` | `required` | `missingProperty: TEXT = property` |

`$ref`, `properties`, and `items` are applicators and are not V1 diagnostic
rules. Failure to load or resolve the immutable schema or one of its references
is a processing/compatibility failure, not `SCHEMA_VIOLATION`.

## Keyword parameter vocabulary

The complete V1 keyword vocabulary contains exactly nine mappings:

| Keyword | Required parameters |
| --- | --- |
| `type` | `expectedType: TEXT` |
| `required` | `missingProperty: TEXT` |
| `additionalProperties` | `unexpectedProperty: TEXT` |
| `const` | `expectedText: TEXT` |
| `minLength` | `minimumCodePointLength: UINT64` |
| `maxLength` | `maximumCodePointLength: UINT64` |
| `pattern` | `requiredPattern: TEXT` |
| `minItems` | `minimumItemCount: UINT64` |
| `uniqueItems` | `firstIndex: UINT64`, `duplicateIndex: UINT64` |

Parameter names are exact and case-sensitive. Parameters are ordered by name
using exact Unicode code-point ordering and are unique. Values use the ADR-015
canonical typed-parameter encodings. No actual invalid value, rendered JSON
fragment, validator argument, or extra parameter is admitted unless a future
mapping-contract version explicitly defines it.

String lengths are JSON Schema Unicode code-point lengths, not UTF-16 code-unit
counts or encoded byte lengths. `UINT64` values have the ADR-015 unsigned range
and overflow rules.

## RFC 6901 instance-location semantics

The structural-location contract is `rfc-6901-json-pointer-v1`.

- The empty string identifies the document root.
- Each object property contributes one reference token.
- Within a token, `~` is encoded as `~0` and `/` as `~1`.
- Array positions use zero-based decimal index tokens without a sign or leading
  zero, except that zero is exactly `0`.
- `type`, `const`, `minLength`, `maxLength`, `pattern`, and `minItems` point to
  the value that violates the rule.
- `required` points to the object from which the property is absent;
  `missingProperty` identifies the absent property.
- `additionalProperties` points to the object that contains the unexpected
  property; `unexpectedProperty` identifies that property.
- `uniqueItems` points to the array; `firstIndex` and `duplicateIndex` identify
  the equivalent entries.
- A rule reached through `$ref` retains the concrete instance location. A
  schema location or `$ref` path never replaces the instance pointer.

The adapter constructs and verifies these pointers from the parsed instance.
It must not accept a validator-rendered path without translating and checking
it against these rules.

## Expansion and consolidation

Validator diagnostic cardinality is not normative.

### Required properties

A combined validator diagnostic naming multiple missing properties expands to
one QAIP diagnostic for each missing property. Each diagnostic has the same
owning-object instance location and `required` rule ID and has one
`missingProperty` parameter.

### Additional properties

A combined validator diagnostic naming multiple unexpected properties expands
to one QAIP diagnostic for each unexpected property. Each diagnostic has the
same owning-object instance location and `additionalProperties` rule ID and has
one `unexpectedProperty` parameter.

### Unique items

Equivalent array items are grouped according to JSON Schema 2020-12 equality,
not Java object identity, JSON rendering, property insertion order, or
validator prose. For each group containing two or more equivalent items, the
adapter emits one diagnostic pairing the group's lowest index as `firstIndex`
with each later equivalent index as `duplicateIndex`.

For equivalent items at indexes `1`, `4`, and `7`, the canonical diagnostics
therefore contain pairs `(1,4)` and `(1,7)`, never `(4,7)` as an additional
pair.

### Consolidation

Multiple library diagnostics that map to the same complete QAIP diagnostic
tuple consolidate to one tuple at the adapter boundary. After consolidation,
the canonical diagnostic collection rejects any duplicate tuple as an
integrity failure, as required by ADR-015. Consolidation does not merge
different missing properties, unexpected properties, or duplicate-index
pairs.

## Deterministic ordering

After expansion and consolidation, diagnostics are ordered by this exact
tuple:

1. `instanceLocation` by Unicode code point;
2. `normativeSchemaKeyword` by Unicode code point;
3. `schemaRuleId` by Unicode code point;
4. `stableDiagnosticCode` by Unicode code point; and
5. complete canonical typed-parameter bytes lexicographically as unsigned
   bytes.

Parameters within one diagnostic are ordered by parameter name using Unicode
code-point ordering. Validator emission order, collection implementation order,
locale, and filesystem order have no effect.

## Validator adapter boundary and exclusions

A validator adapter may inspect validator-specific fields only as untrusted
routing inputs. It must confirm the mapped keyword, rule, instance location,
and parameters against the immutable V1 schema and parsed instance.

The following are explicitly excluded from canonical evidence:

- NetworkNT code, numeric or textual;
- validator schema URI, retrieval URI, or schema-location rendering;
- validator evaluation path;
- human-readable or localized message and message key;
- exception type, stack trace, or exception message;
- validator-library emission order;
- serialized `ValidationMessage` or equivalent library result;
- library-rendered instance or schema fragments; and
- any field whose stability is controlled by the validator implementation
  rather than this contract.

Changing any excluded value must not change a QAIP diagnostic.

## Unknown, ambiguous, or incomplete mappings

The stable processing/compatibility failure code is:

```text
UNSUPPORTED_SCHEMA_DIAGNOSTIC_MAPPING
```

It applies when a validator result is unknown, cannot be mapped to exactly one
or more rules in the finite table, has ambiguous rule or instance attribution,
has missing or inconsistent parameter data that cannot be verified from the
schema and instance, or exposes a violation outside this V1 vocabulary.

This failure is not a schema diagnostic and must never:

- become a generic `SCHEMA_VIOLATION`;
- be placed in `StableScenarioSchemaDiagnosticV1`;
- cause the affected member to be structurally admitted;
- produce a partially mapped canonical rejected-member result or fingerprint;
- copy validator messages or codes into canonical evidence; or
- suppress independent processing of other captured members.

The affected member produces no canonical schema-admission result until all of
its validator failures map completely. The failure remains distinct from
ordinary `STRUCTURALLY_REJECTED` evidence.

## Validator-upgrade compatibility

The V1 conformance corpus is a frozen compatibility gate for every validator
library or version change. Before an upgraded validator may be accepted, the
adapter must demonstrate that:

1. every one of the 36 rule IDs remains exercised;
2. every keyword parameter shape remains exercised;
3. the same schema/instance inputs produce byte-for-byte equal QAIP diagnostic
   records and equal ordered diagnostic collections;
4. changes in validator codes, URIs, messages, arguments, grouping, duplication,
   and emission order do not change canonical results;
5. expansion and consolidation produce the same QAIP tuples;
6. unknown, ambiguous, incomplete, or newly emitted diagnostics fail with
   `UNSUPPORTED_SCHEMA_DIAGNOSTIC_MAPPING`; and
7. no corpus expectation is changed merely to accommodate new validator
   behavior.

An adapter implementation may change while this contract remains V1 only when
the resulting canonical diagnostics remain identical. A schema rule, pointer,
constraint, parameter, or mapping-semantics change requires a new schema or
mapping-contract version as applicable; it must not silently redefine V1.

## Normative golden-vector requirements

Golden vectors must publish the complete logical diagnostic input, complete
canonical typed-parameter bytes, and complete deterministically ordered result
for:

- all 36 schema-rule IDs, including each reused `$defs` rule at representative
  concrete instance locations;
- all nine keyword mappings and every fixed V1 parameter value;
- root and nested `type` violations;
- every `const` rule;
- all authority, stable-key, identity-scheme, and non-blank-string length and
  pattern boundaries;
- Unicode code-point length boundaries, including BMP, supplementary, and
  combining-code-point inputs;
- empty Step arrays for `minItems`;
- one missing property and a combined multi-`required` validator result;
- one unexpected property and a combined multi-`additionalProperties`
  validator result;
- one `uniqueItems` pair, multiple independent pairs, and an equivalence group
  of at least three items;
- semantically equal JSON objects with different object-member order for
  `uniqueItems`;
- root location, nested object location, array index location, and RFC 6901
  tokens containing `/` and `~`;
- validator diagnostics supplied in different orders producing the same QAIP
  order;
- one library diagnostic expanding to multiple QAIP diagnostics;
- multiple library diagnostics consolidating to one QAIP diagnostic;
- distinct parameter tuples remaining distinct after consolidation;
- changes only to library code, schema URI/rendering, evaluation path, human or
  localized message, exception type, serialized result, or rendered fragments
  producing identical QAIP diagnostics;
- duplicate canonical tuples being rejected if they survive adapter
  consolidation;
- unknown keyword, unknown schema rule, ambiguous rule attribution, malformed
  validator arguments, inconsistent validator data, and incomplete mapping all
  producing `UNSUPPORTED_SCHEMA_DIAGNOSTIC_MAPPING`; and
- repeated mapping producing identical results.

The frozen corpus must include validator-independent expected QAIP records. A
library's own serialized validation result is not a golden vector.

## Ownership and conformance

The Scenario source adapter owns the finite conversion from validator results
to this mapping contract and the verification of schema/instance facts.
Evidence Governance owns the canonical diagnostic record and its typed binary
encoding under ADR-015.

Runtime, Coverage, Explorer, authority qualification, Operation or Business
Rule resolution, Canonical Ontology, and attributed-member fingerprinting are
outside this contract.

An implementation is conformant only if it supports all 36 rules, all nine
keyword mappings, the exact expansion, consolidation, location, and ordering
semantics, and the fail-closed unknown-diagnostic policy. Partial support must
not be advertised as V1 support.

## ADR-015 compatibility

This subordinate contract introduces no contradiction with ADR-015. Adapter
consolidation occurs before construction of the canonical collection; ADR-015's
requirement that duplicate canonical tuples are rejected still applies after
that boundary. All diagnostic fields, types, ordering, exclusions, and
ownership remain those defined by ADR-015.
