package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QaModelFingerprintCalculatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final QaModelFingerprintCalculator calculator =
            new QaModelFingerprintCalculator();

    @Test
    void shouldIgnoreObjectInsertionOrderIncludingNestedObjects() throws Exception {
        JsonNode first = mapper.readTree(
                "{\"z\":1,\"nested\":{\"b\":true,\"a\":\"x\"}}"
        );
        JsonNode second = mapper.readTree(
                "{\"nested\":{\"a\":\"x\",\"b\":true},\"z\":1}"
        );

        assertEquals(calculator.calculate(first), calculator.calculate(first));
        assertEquals(calculator.calculate(first), calculator.calculate(second));
        assertTrue(calculator.calculate(first).matches(
                "qamodel-c14n-v1:[0-9a-f]{64}"));
    }

    @Test
    void shouldPreserveArrayOrderAndScalarTypes() throws Exception {
        assertNotEquals(fingerprint("{\"a\":[1,2]}"),
                fingerprint("{\"a\":[2,1]}"));
        assertNotEquals(fingerprint("{\"a\":1}"),
                fingerprint("{\"a\":\"1\"}"));
        assertNotEquals(fingerprint("{\"a\":true}"),
                fingerprint("{\"a\":\"true\"}"));
    }

    @Test
    void numericallyEquivalentJsonNumbersShouldHaveSameFingerprint()
            throws Exception {
        assertEquivalentNumbers("0", "-0");
        assertEquivalentNumbers("1", "1.0", "1.00", "1e0", "1000e-3");
        assertNotEquals(fingerprint("{\"n\":1}"),
                fingerprint("{\"n\":1.01}"));
        assertNotEquals(fingerprint("{\"n\":1}"),
                fingerprint("{\"n\":\"1\"}"));
    }

    @Test
    void unicodeShouldBeStableAndInputMustNotBeMutated() throws Exception {
        JsonNode model = mapper.readTree(
                "{\"name\":\"Тест ✓\",\"number\":1.00}"
        );
        JsonNode before = model.deepCopy();
        assertEquals(calculator.calculate(model), calculator.calculate(model));
        assertEquals(before, model);
    }

    @Test
    void concurrentCallsShouldBeDeterministic() throws Exception {
        JsonNode model = mapper.readTree(
                "{\"nodes\":[{\"id\":\"N\"}],\"relationships\":[]}"
        );
        String expected = calculator.calculate(model);
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            calls.add(() -> calculator.calculate(model));
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            assertTrue(executor.invokeAll(calls).stream().allMatch(future -> {
                try {
                    return expected.equals(future.get());
                } catch (Exception exception) {
                    return false;
                }
            }));
        }
    }

    private String fingerprint(String json) throws Exception {
        return calculator.calculate(mapper.readTree(json));
    }

    private void assertEquivalentNumbers(String expected, String... variants)
            throws Exception {
        String expectedFingerprint = fingerprint("{\"n\":" + expected + "}");
        for (String variant : variants) {
            assertEquals(expectedFingerprint,
                    fingerprint("{\"n\":" + variant + "}"));
        }
    }
}
