package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/** Immutable QAIP-owned canonical Scenario Authority schema diagnostic. */
public record ScenarioSchemaDiagnostic(
        String diagnosticContractVersion,
        Code stableCode,
        String instanceLocation,
        String normativeSchemaKeyword,
        String schemaRuleIdentifier,
        List<CanonicalTypedParameter> typedParameters
) {
    public static final String DIAGNOSTIC_CONTRACT_VERSION =
            "scenario-authority-schema-diagnostic-v1";

    public ScenarioSchemaDiagnostic {
        requireExact(diagnosticContractVersion, DIAGNOSTIC_CONTRACT_VERSION,
                "diagnosticContractVersion");
        if (stableCode != Code.SCHEMA_VIOLATION) {
            throw new IllegalArgumentException("stableCode must be SCHEMA_VIOLATION");
        }
        instanceLocation = requireJsonPointer(instanceLocation);
        normativeSchemaKeyword = requireNonBlank(normativeSchemaKeyword, "normativeSchemaKeyword");
        schemaRuleIdentifier = requireNonBlank(schemaRuleIdentifier, "schemaRuleIdentifier");
        typedParameters = List.copyOf(Objects.requireNonNull(typedParameters, "typedParameters"));
        String previous = null;
        for (CanonicalTypedParameter parameter : typedParameters) {
            Objects.requireNonNull(parameter, "typedParameter");
            if (previous != null && UnicodeCodePointOrder.compare(previous, parameter.name()) >= 0) {
                throw new IllegalArgumentException(
                        "typed parameter names must be unique and ordered by Unicode code point");
            }
            previous = parameter.name();
        }
    }

    public enum Code {
        SCHEMA_VIOLATION
    }

    public enum ParameterType {
        TEXT,
        UINT64
    }

    public sealed interface CanonicalTypedParameter permits TextParameter, Unsigned64Parameter {
        String name();

        ParameterType type();
    }

    public record TextParameter(String name, String value) implements CanonicalTypedParameter {
        public TextParameter {
            name = requireNonBlank(name, "name");
            Objects.requireNonNull(value, "value");
        }

        @Override
        public ParameterType type() {
            return ParameterType.TEXT;
        }
    }

    public record Unsigned64Parameter(String name, BigInteger value)
            implements CanonicalTypedParameter {
        private static final BigInteger MAXIMUM = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

        public Unsigned64Parameter {
            name = requireNonBlank(name, "name");
            Objects.requireNonNull(value, "value");
            if (value.signum() < 0 || value.compareTo(MAXIMUM) > 0) {
                throw new IllegalArgumentException("value must have unsigned 64-bit semantics");
            }
        }

        public Unsigned64Parameter(String name, long value) {
            this(name, BigInteger.valueOf(value));
        }

        @Override
        public ParameterType type() {
            return ParameterType.UINT64;
        }
    }

    private static String requireJsonPointer(String value) {
        Objects.requireNonNull(value, "instanceLocation");
        if (!value.isEmpty() && !value.startsWith("/")) {
            throw new IllegalArgumentException("instanceLocation must be an RFC 6901 JSON Pointer");
        }
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '~') {
                if (++index >= value.length()
                        || (value.charAt(index) != '0' && value.charAt(index) != '1')) {
                    throw new IllegalArgumentException("instanceLocation has an invalid RFC 6901 escape");
                }
            }
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private static void requireExact(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
