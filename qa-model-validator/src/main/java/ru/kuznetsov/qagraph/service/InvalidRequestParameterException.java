package ru.kuznetsov.qagraph.service;

public class InvalidRequestParameterException extends RuntimeException {

    private final String parameter;

    public InvalidRequestParameterException(String parameter) {
        super("Query parameter '" + parameter + "' must not be blank");
        this.parameter = parameter;
    }

    public String parameter() {
        return parameter;
    }
}
