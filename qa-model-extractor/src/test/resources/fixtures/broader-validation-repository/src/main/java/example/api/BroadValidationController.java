package example.api;

import example.request.JakartaBroadRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BroadValidationController {
    @PostMapping("/broad")
    public void create(@RequestBody JakartaBroadRequest request) {
    }
}
