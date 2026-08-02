package example.api;

import example.web.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wrong")
public class WrongMappingController {
    @GetMapping("/ignored")
    public void ignored() {
    }
}
