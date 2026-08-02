package example.request;

import example.foreign.Positive;
import example.validation.ValidCode;
import org.hibernate.validator.constraints.Range;

public class UnsupportedValidationRequest {
    @Positive
    private int foreign;

    @Range(min = 1, max = 10)
    private int hibernateSpecific;

    @org.hibernate.validator.constraints.NotBlank
    private String hibernateSameNamed;

    @ValidCode
    private String custom;
}
