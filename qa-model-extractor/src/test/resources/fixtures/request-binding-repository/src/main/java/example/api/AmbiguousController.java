package example.api;

import example.one.*;
import example.two.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AmbiguousController {
    @PostMapping("/ambiguous")
    public void ambiguous(@RequestBody DuplicateRequest request) {
    }
}
