package ru.kuznetsov.qagraph.model;

/**
 * Technology-neutral engineering responsibility of a technical implementation.
 */
public enum ImplementationRole {
    REST_CONTROLLER,
    APPLICATION_SERVICE,
    REPOSITORY,
    MESSAGE_PRODUCER,
    MESSAGE_DESTINATION,
    MESSAGE_CONSUMER
}
