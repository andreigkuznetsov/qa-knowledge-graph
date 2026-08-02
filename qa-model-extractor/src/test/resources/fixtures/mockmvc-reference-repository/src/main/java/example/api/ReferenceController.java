package example.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ReferenceController {
    @GetMapping("/{id}")
    public void get(@PathVariable String id) {
    }

    @PostMapping
    public void create() {
    }
}
