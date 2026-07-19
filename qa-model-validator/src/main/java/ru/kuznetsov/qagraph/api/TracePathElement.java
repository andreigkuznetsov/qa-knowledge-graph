package ru.kuznetsov.qagraph.api;

public sealed interface TracePathElement
        permits TraceNodeElement, TraceRelationshipElement {
}
