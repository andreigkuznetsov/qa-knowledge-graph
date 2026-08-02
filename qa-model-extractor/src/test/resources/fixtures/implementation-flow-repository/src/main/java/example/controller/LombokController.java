package example.controller;

import example.service.LombokService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LombokController {
    private final LombokService service;

    @PostMapping("/lombok")
    public void create() {
        service.create();
    }
}
