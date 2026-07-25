package ru.kuznetsov.qaip.core.importing.parsing;

/**
 * Parses exactly one JSON value without project-specific validation or binding.
 * Expected invalid input is returned as a rejection; a null source is a
 * programming error. {@link JacksonProjectJsonParser} instances are safe for
 * concurrent calls.
 */
public interface ProjectJsonParser {
    ProjectParseResult parse(RawProjectJson source);
}
