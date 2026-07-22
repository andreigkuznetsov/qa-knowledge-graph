package ru.kuznetsov.qagraph.change.root;

public enum RootReconstructionDiagnosticCode {
    BASE_ROOT_NOT_AVAILABLE(0),
    BASE_ROOT_EVIDENCE_MISMATCH(1),
    ROOT_VERSION_UNSUPPORTED(2),
    PROPOSED_MODEL_VERSION_MISMATCH(3),
    ROOT_RECONSTRUCTION_INCOMPLETE(4);

    private final int rank;
    RootReconstructionDiagnosticCode(int rank) { this.rank = rank; }
    public int rank() { return rank; }
}
