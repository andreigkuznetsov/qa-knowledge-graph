package ru.kuznetsov.qaip.core.importing.binding;

import ru.kuznetsov.qaip.core.importing.parsing.SchemaValidProjectDocument;

public interface ProjectBinder {
    BindingResult bind(SchemaValidProjectDocument document);
}
