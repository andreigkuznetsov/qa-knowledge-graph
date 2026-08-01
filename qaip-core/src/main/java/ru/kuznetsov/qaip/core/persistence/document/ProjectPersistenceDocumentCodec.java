package ru.kuznetsov.qaip.core.persistence.document;

import ru.kuznetsov.qaip.core.domain.Project;

/** Infrastructure bridge for the frozen Project Persistence Document v1 format. */
public interface ProjectPersistenceDocumentCodec {
    static ProjectPersistenceDocumentCodec v1() {
        return new JacksonProjectPersistenceDocumentCodec();
    }

    String encode(Project project);

    Project decode(String payload);
}
