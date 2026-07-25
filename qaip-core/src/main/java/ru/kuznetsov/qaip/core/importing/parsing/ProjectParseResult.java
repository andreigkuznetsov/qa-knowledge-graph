package ru.kuznetsov.qaip.core.importing.parsing;

/** Result of syntax-only project JSON parsing. */
public sealed interface ProjectParseResult permits ProjectParseAccepted, ProjectParseRejected {
}
