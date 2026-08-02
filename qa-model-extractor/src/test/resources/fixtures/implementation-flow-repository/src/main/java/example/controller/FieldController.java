package example.controller;

import example.service.FieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FieldController {
    @Autowired
    private FieldService service;

    @PostMapping("/field")
    public void create() {
        service.create();
    }
}
