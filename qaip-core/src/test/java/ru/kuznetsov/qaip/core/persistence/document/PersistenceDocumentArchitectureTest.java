package ru.kuznetsov.qaip.core.persistence.document;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceDocumentArchitectureTest {
    private static final List<Class<?>> INFRASTRUCTURE_TYPES = List.of(
            JacksonProjectPersistenceDocumentCodec.class, ProjectPersistenceDocument.class,
            ProjectDocument.class, MetadataDocument.class, SubjectDocument.class, NodeDocument.class,
            RelationshipDocument.class, EvidenceManifestDocument.class, DeclaredChangeDocument.class,
            PersistenceValueDocument.class);

    @Test
    void document_and_implementation_types_are_private_while_the_narrow_codec_bridge_is_public() {
        for (Class<?> type : INFRASTRUCTURE_TYPES) {
            assertFalse(Modifier.isPublic(type.getModifiers()), type.getName());
            if (!type.isInterface()) assertTrue(Modifier.isFinal(type.getModifiers()), type.getName());
        }
        assertTrue(Modifier.isPublic(ProjectPersistenceDocumentCodec.class.getModifiers()));
        assertTrue(Modifier.isPublic(ProjectPersistenceDocumentException.class.getModifiers()));
        for (Class<?> document : INFRASTRUCTURE_TYPES.stream().filter(Class::isRecord).toList()) {
            assertTrue(Modifier.isFinal(document.getModifiers()));
        }
    }

    @Test
    void public_persistence_and_application_apis_do_not_expose_document_types() {
        for (Class<?> api : List.of(ProjectRepository.class, PersistProject.class)) {
            for (var method : api.getMethods()) {
                assertFalse(method.toGenericString().contains("persistence.document"));
            }
        }
    }

    @Test
    void frozen_layers_do_not_depend_on_document_and_codec_has_no_forbidden_dependencies() throws IOException {
        for (String frozen : List.of("domain", "importing", "validation", "application")) {
            assertSourceExcludes("src/main/java/ru/kuznetsov/qaip/core/" + frozen,
                    "core.persistence.document", "ProjectPersistenceDocumentCodec");
        }
        assertSourceExcludes("src/main/java/ru/kuznetsov/qaip/core/persistence/document",
                "core.importing", "core.validation", "core.application", "java.sql", "javax.sql",
                "org.postgresql", "org.springframework", "jakarta.persistence", "org.hibernate");
        assertSourceExcludes("src/main/java/ru/kuznetsov/qaip/core/domain", "com.fasterxml.jackson");
    }

    @Test
    void codec_boundary_mentions_only_project_string_and_its_own_result_types() throws Exception {
        assertTrue(ProjectPersistenceDocumentCodec.class.getDeclaredMethod("encode", Project.class)
                .getReturnType().equals(String.class));
        assertTrue(ProjectPersistenceDocumentCodec.class.getDeclaredMethod("decode", String.class)
                .getReturnType().equals(Project.class));
    }

    private static void assertSourceExcludes(String root, String... forbidden) throws IOException {
        try (var files = Files.walk(Path.of(root))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String value : forbidden) {
                    assertFalse(source.contains(value), () -> file + " contains " + value);
                }
            }
        }
    }
}
