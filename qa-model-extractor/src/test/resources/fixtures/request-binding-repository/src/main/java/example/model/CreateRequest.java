package example.model;

import jakarta.validation.constraints.NotBlank;

public class CreateRequest {
    @NotBlank
    private String name;
}
