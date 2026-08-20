package ru.kuznetsov.qaip.evidencegovernance.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.regex.Pattern;

import static ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1.Code;

/** Sole authoritative safe Scenario Authority V1 claimed-authority attributor. */
public final class ScenarioAuthorityAttributorV1 {
    public static final String CONTRACT_IDENTIFIER = ScenarioAuthorityAttributionV1.ATTRIBUTION_CONTRACT_IDENTIFIER;
    private static final Pattern AUTHORITY = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:/-]*$");
    private static final int MAX_AUTHORITY_LENGTH = 200;

    public ScenarioAuthorityAttributionV1 attribute(ScenarioAuthorityParsedJsonV1 parsedJson) {
        Objects.requireNonNull(parsedJson, "parsedJson");
        JsonNode document = parsedJson.authoritativeDocument();
        if (!document.isObject()) throw reject(Code.NON_OBJECT_ROOT, "");
        if (!document.has("authority")) throw reject(Code.MISSING_AUTHORITY, "/authority");
        JsonNode value = document.get("authority");
        if (!value.isTextual()) throw reject(Code.AUTHORITY_NOT_STRING, "/authority");
        String authority = value.textValue();
        if (authority.length() < 1 || authority.length() > MAX_AUTHORITY_LENGTH
                || !AUTHORITY.matcher(authority).matches())
            throw reject(Code.INVALID_AUTHORITY, "/authority");
        return new ScenarioAuthorityAttributionV1(parsedJson, authority);
    }

    private static ScenarioAuthorityAttributionRejectionV1 reject(Code code, String location) {
        return new ScenarioAuthorityAttributionRejectionV1(code, location);
    }
}
