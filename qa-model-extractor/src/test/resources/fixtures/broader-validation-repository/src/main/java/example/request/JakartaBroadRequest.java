package example.request;

import jakarta.validation.constraints.*;

public class JakartaBroadRequest {
    @Null
    private String absent;

    @AssertTrue
    private boolean accepted;

    @AssertFalse
    private boolean disabled;

    @Positive
    private int positive;

    @PositiveOrZero
    @Positive(message = "positive override")
    private int positiveOrZero;

    @Negative
    private int negative;

    @NegativeOrZero
    private int negativeOrZero;

    @DecimalMin(value = "0.10", inclusive = false, message = "above minimum")
    private String decimalMin;

    @DecimalMax(value = "99.99", inclusive = true)
    private String decimalMax;

    @Digits(integer = 4, fraction = 2, groups = {Create.class, Update.class},
            payload = Severity.class, message = "numeric format")
    private String digits;

    @Past
    private String past;

    @PastOrPresent
    private String pastOrPresent;

    @Future
    private String future;

    @FutureOrPresent
    private String futureOrPresent;

    public void adjust(@Positive(message = "adjustment must be positive") int adjustment) {
    }
}
