package ru.kuznetsov.qaip.core.validation;

import ru.kuznetsov.qaip.core.importing.binding.BoundProjectDocument;

public interface ProjectApplicationValidator {
    ApplicationValidationResult validate(BoundProjectDocument document);
}
