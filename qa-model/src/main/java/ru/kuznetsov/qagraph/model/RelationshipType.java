package ru.kuznetsov.qagraph.model;

public enum RelationshipType {
    DESCRIBES,
    GOVERNED_BY,
    SPECIFIED_BY,
    IMPLEMENTED_BY,
    VALIDATES,
    USES,
    HAS_CHECK,
    COVERS,
    DEPENDS_ON,
    REFINES,
    SUPERSEDES,
    RELATED_TO;

    public static RelationshipType from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
