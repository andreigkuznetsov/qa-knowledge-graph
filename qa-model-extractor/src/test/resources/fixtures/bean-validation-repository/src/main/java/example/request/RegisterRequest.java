package example.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record RegisterRequest(
        @NotNull String username,
        @NotEmpty @Size(max = 64) String password,
        @Max(10) int tier
) {
}
