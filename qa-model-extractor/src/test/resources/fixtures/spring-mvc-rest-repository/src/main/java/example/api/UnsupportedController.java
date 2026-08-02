package example.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/unsupported")
public class UnsupportedController {
    @GetMapping("/ignored")
    public void ignored() {
    }
}
