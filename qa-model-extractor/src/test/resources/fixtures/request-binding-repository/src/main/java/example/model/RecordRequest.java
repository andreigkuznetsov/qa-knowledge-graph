package example.model;

import jakarta.validation.constraints.NotNull;

public record RecordRequest(@NotNull String value) {
}
