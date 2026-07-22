package ru.kuznetsov.qagraph.change.complete;

public enum CompleteValidationDiagnosticOrigin {
    SCHEMA(0),
    SEMANTIC(1);

    private final int rank;

    CompleteValidationDiagnosticOrigin(int rank) {
        this.rank = rank;
    }

    int rank() {
        return rank;
    }
}
