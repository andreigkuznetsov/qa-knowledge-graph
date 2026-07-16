package ru.kuznetsov.qagraph.storyanalyzer.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kuznetsov.qagraph.storyanalyzer.analysis.StoryAnalyzer;
import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisRequest;
import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisResult;

@RestController
@RequestMapping("/api/v1/story")
public class StoryAnalysisController {

    private final StoryAnalyzer storyAnalyzer;

    public StoryAnalysisController(StoryAnalyzer storyAnalyzer) {
        this.storyAnalyzer = storyAnalyzer;
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StoryAnalysisResult> analyze(
            @Valid @RequestBody StoryAnalysisRequest request
    ) {
        return ResponseEntity.ok(storyAnalyzer.analyze(request));
    }
}
