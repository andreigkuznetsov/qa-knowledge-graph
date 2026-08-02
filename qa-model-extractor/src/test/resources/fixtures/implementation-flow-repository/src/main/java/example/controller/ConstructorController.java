package example.controller;

import example.service.ConstructorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConstructorController {
    private final ConstructorService service;

    public ConstructorController(ConstructorService service) {
        this.service = service;
    }

    @PostMapping("/constructor")
    public void create() {
        service.create();
    }
}
