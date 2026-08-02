package ru.kuznetsov.qagraph.extractor.validation;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanValidationEvidenceExtractorTest {
    private final BeanValidationEvidenceExtractor extractor = new BeanValidationEvidenceExtractor();

    @Test
    void extractsOneConstraintFromAField() throws Exception {
        BeanValidationEvidence email = find("example.request.CreateRequest", "email", "Email");

        assertEquals("jakarta.validation.constraints.Email", email.annotationType());
        assertEquals(Map.of(), email.declaredAttributes());
        assertNull(email.explicitMessage());
    }

    @Test
    void extractsMultipleConstraintsFromOneFieldSeparately() throws Exception {
        List<BeanValidationEvidence> codeEvidence = extractor.extract(fixtureRepository()).stream()
                .filter(evidence -> evidence.owningJavaType().equals("example.request.CreateRequest"))
                .filter(evidence -> evidence.memberName().equals("code"))
                .toList();

        assertEquals(List.of(
                        "jakarta.validation.constraints.Pattern",
                        "jakarta.validation.constraints.Size"),
                codeEvidence.stream().map(BeanValidationEvidence::annotationType).toList());
    }

    @Test
    void extractsRecordComponentConstraints() throws Exception {
        List<String> recordEvidence = extractor.extract(fixtureRepository()).stream()
                .filter(evidence -> evidence.owningJavaType().equals("example.request.RegisterRequest"))
                .map(evidence -> evidence.memberName() + ":" + evidence.annotationType())
                .toList();

        assertEquals(List.of(
                "password:javax.validation.constraints.NotEmpty",
                "password:javax.validation.constraints.Size",
                "tier:javax.validation.constraints.Max",
                "username:javax.validation.constraints.NotNull"), recordEvidence);
    }

    @Test
    void preservesOnlyExplicitAttributesAndMessage() throws Exception {
        BeanValidationEvidence size = find("example.request.CreateRequest", "code", "Size");
        BeanValidationEvidence title = find("example.request.CreateRequest", "title", "NotBlank");
        BeanValidationEvidence tier = find("example.request.RegisterRequest", "tier", "Max");

        assertEquals(Map.of("max", "32", "min", "2"), size.declaredAttributes());
        assertNull(size.explicitMessage());
        assertEquals(Map.of("message", "\"title is required\""), title.declaredAttributes());
        assertEquals("title is required", title.explicitMessage());
        assertEquals(Map.of("value", "10"), tier.declaredAttributes());
    }

    @Test
    void ignoresForeignSameNamedAndUnsupportedCustomAnnotations() throws Exception {
        List<BeanValidationEvidence> evidence = extractor.extract(fixtureRepository());

        assertTrue(evidence.stream().noneMatch(item ->
                item.owningJavaType().equals("example.request.ForeignConstraintRequest")));
        assertTrue(evidence.stream().noneMatch(item ->
                item.annotationType().endsWith("ValidCode")));
    }

    @Test
    void classWithoutSupportedConstraintsContributesNoEvidence() throws Exception {
        assertTrue(extractor.extract(fixtureRepository()).stream().noneMatch(evidence ->
                evidence.owningJavaType().equals("example.request.NoConstraintsRequest")));
    }

    @Test
    void producesAllExpectedEvidenceOnceInStableOrderWithStableLocations() throws Exception {
        List<BeanValidationEvidence> first = extractor.extract(fixtureRepository());
        List<BeanValidationEvidence> second = extractor.extract(fixtureRepository());

        assertEquals(first, second);
        assertEquals(9, first.size());
        assertEquals(9, first.stream().distinct().count());
        assertEquals(List.of(
                "example.request.CreateRequest|code|Pattern",
                "example.request.CreateRequest|code|Size",
                "example.request.CreateRequest|email|Email",
                "example.request.CreateRequest|quantity|Min",
                "example.request.CreateRequest|title|NotBlank",
                "example.request.RegisterRequest|password|NotEmpty",
                "example.request.RegisterRequest|password|Size",
                "example.request.RegisterRequest|tier|Max",
                "example.request.RegisterRequest|username|NotNull"),
                first.stream().map(BeanValidationEvidenceExtractorTest::summary).toList());

        BeanValidationEvidence title = find("example.request.CreateRequest", "title", "NotBlank");
        assertEquals("src/main/java/example/request/CreateRequest.java", title.repositoryRelativePath());
        assertEquals(11, title.line());
        assertEquals(5, title.column());
    }

    private BeanValidationEvidence find(String owner, String member, String annotation) throws Exception {
        return extractor.extract(fixtureRepository()).stream()
                .filter(evidence -> evidence.owningJavaType().equals(owner))
                .filter(evidence -> evidence.memberName().equals(member))
                .filter(evidence -> evidence.annotationType().endsWith('.' + annotation))
                .findFirst()
                .orElseThrow();
    }

    private static String summary(BeanValidationEvidence evidence) {
        String annotation = evidence.annotationType()
                .substring(evidence.annotationType().lastIndexOf('.') + 1);
        return evidence.owningJavaType() + "|" + evidence.memberName() + "|" + annotation;
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(BeanValidationEvidenceExtractorTest.class.getResource(
                "/fixtures/bean-validation-repository").toURI());
    }
}
