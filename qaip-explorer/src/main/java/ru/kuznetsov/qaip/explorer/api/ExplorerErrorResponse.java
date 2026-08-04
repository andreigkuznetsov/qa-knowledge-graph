package ru.kuznetsov.qaip.explorer.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExplorerErrorResponse(String code, String message, String repositoryId) {
    public ExplorerErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
