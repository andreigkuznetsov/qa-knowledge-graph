package ru.kuznetsov.qaip.core.importing.parsing;

/** Parses exactly one JSON value without project-specific validation or binding. */
public interface ProjectJsonParser {
    ProjectParseResult parse(RawProjectJson source);
}
