package ru.kuznetsov.qagraph.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImplementationRoleTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesCanonicalRoleNames() throws Exception {
        for (ImplementationRole role : ImplementationRole.values()) {
            assertEquals('"' + role.name() + '"', mapper.writeValueAsString(role));
        }
    }

    @Test
    void deserializesEveryCanonicalRole() throws Exception {
        Set<ImplementationRole> deserialized = Arrays.stream(ImplementationRole.values())
                .map(role -> deserialize(role.name()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(ImplementationRole.values()), deserialized);
    }

    @Test
    void rejectsUnknownAndIncorrectlyCasedRoles() {
        assertThrows(InvalidFormatException.class, () -> mapper.readValue("\"KAFKA_PRODUCER\"", ImplementationRole.class));
        assertThrows(InvalidFormatException.class, () -> mapper.readValue("\"message_producer\"", ImplementationRole.class));
    }

    private ImplementationRole deserialize(String value) {
        try {
            return mapper.readValue('"' + value + '"', ImplementationRole.class);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
