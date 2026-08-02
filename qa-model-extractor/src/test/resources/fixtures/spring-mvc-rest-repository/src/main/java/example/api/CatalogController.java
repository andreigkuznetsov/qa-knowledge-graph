package example.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/")
public class CatalogController {
    @GetMapping
    public void list() {
    }

    @PostMapping("/create")
    public void create() {
    }

    @GetMapping(path = {"/search/author", "/search/title"})
    public void search() {
    }

    @PutMapping("/{id}")
    public void replace() {
    }

    @PatchMapping("/{id}")
    public void update() {
    }

    @DeleteMapping("/{id}")
    public void delete() {
    }

    @RequestMapping(path = "/export", method = {
            RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS
    })
    public void export() {
    }

    @RequestMapping("/unbound")
    public void noHttpMethod() {
    }
}
