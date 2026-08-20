package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.Objects; import java.util.regex.Pattern;
public record ScenarioIdentityGroupFingerprint(String value) {
    public static final String VALUE_IDENTIFIER="scenario-authority-scenario-identity-group-v1";
    public static final String VALUE_PREFIX=VALUE_IDENTIFIER+":";
    private static final Pattern P=Pattern.compile("^"+VALUE_IDENTIFIER+":[0-9a-f]{64}$");
    public ScenarioIdentityGroupFingerprint { Objects.requireNonNull(value); if(!P.matcher(value).matches())throw new IllegalArgumentException("invalid Scenario identity-group fingerprint"); }
}
