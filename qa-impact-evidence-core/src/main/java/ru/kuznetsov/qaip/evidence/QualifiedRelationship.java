package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

record QualifiedRelationship(RelationshipEvidence evidence,
        CanonicalIdentity propagationFrom, CanonicalIdentity propagationTo)
        implements RelationshipQualification { }
