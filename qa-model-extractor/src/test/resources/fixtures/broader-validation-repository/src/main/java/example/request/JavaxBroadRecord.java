package example.request;

import javax.validation.constraints.*;

public record JavaxBroadRecord(
        @Null String absent,
        @AssertTrue boolean accepted,
        @AssertFalse boolean disabled,
        @Positive int positive,
        @PositiveOrZero int positiveOrZero,
        @Negative int negative,
        @NegativeOrZero int negativeOrZero,
        @DecimalMin(value = "0.10", inclusive = false) String decimalMin,
        @DecimalMax(value = "99.99", inclusive = true) String decimalMax,
        @Digits(integer = 4, fraction = 2) String digits,
        @Past String past,
        @PastOrPresent String pastOrPresent,
        @Future String future,
        @FutureOrPresent String futureOrPresent
) {
}
