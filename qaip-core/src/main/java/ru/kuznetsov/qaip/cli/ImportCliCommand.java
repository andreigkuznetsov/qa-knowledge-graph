package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportResult;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ImportCliCommand {
    private static final String READ_FAILURE_MESSAGE = "Cannot read import file.";

    private final ImportProjectUseCase useCase;
    private final ImportResultMapper mapper;

    public ImportCliCommand(ImportProjectUseCase useCase, ImportResultMapper mapper) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public ImportResult execute(Path filePath) {
        Objects.requireNonNull(filePath, "filePath");
        final String source;
        try {
            source = Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ImportFileReadException(READ_FAILURE_MESSAGE, exception);
        }
        return mapper.map(useCase.execute(new RawProjectJson(source)));
    }
}
