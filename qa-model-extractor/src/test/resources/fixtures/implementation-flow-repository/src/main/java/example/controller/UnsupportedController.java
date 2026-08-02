package example.controller;

import example.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UnsupportedController {
    private final MultipleRepositoryService multipleRepositoryService;
    private final ChainedService chainedService;
    private final ServicePort dynamicService;

    @PostMapping("/multiple")
    public void multiple() {
        multipleRepositoryService.run();
    }

    @PostMapping("/chain")
    public void chain() {
        chainedService.run();
    }

    @PostMapping("/dynamic")
    public void dynamic() {
        dynamicService.run();
    }
}
