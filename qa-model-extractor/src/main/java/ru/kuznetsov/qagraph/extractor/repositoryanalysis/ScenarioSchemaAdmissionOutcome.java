package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

/** One immutable terminal ADR-014 schema-admission outcome for one processed member. */
public sealed interface ScenarioSchemaAdmissionOutcome
        permits AttributedMemberSchemaAdmissionOutcome, AttributedMemberSchemaAdmissionFailure,
        UnattributableMemberProcessingOutcome {

    ParentCapturedMemberRef parentMemberRef();
}
