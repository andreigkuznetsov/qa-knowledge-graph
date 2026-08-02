package example.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import example.validation.ValidCode;

public class CreateRequest {
    @NotBlank(message = "title is required")
    private String title;

    @Size(max = 32, min = 2)
    @Pattern(regexp = "[A-Z].*")
    @ValidCode
    private String code;

    @Min(value = 1, message = "quantity must be positive")
    private int quantity;

    @Email
    private String email;
}
