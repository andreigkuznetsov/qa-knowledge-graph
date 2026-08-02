package example.api;

import example.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RequestBindingController {
    @PostMapping("/class")
    public void create(@Valid @RequestBody CreateRequest request,
                       @PathVariable String id,
                       @RequestParam String query,
                       @RequestHeader String header,
                       @CookieValue String cookie,
                       HttpServletRequest servletRequest,
                       Principal principal) {
    }

    @PostMapping("/record")
    public void record(@RequestBody RecordRequest request) {
    }

    @PostMapping("/fully-qualified")
    public void fullyQualified(@RequestBody example.model.CreateRequest request) {
    }

    @GetMapping("/filter")
    public void filter(@Validated @ModelAttribute FilterRequest filter) {
    }

    @PostMapping("/implicit")
    public void implicit(ImplicitRequest request) {
    }

    @PostMapping("/list")
    public void list(@RequestBody List<CreateRequest> requests) {
    }

    @PostMapping("/array")
    public void array(@RequestBody CreateRequest[] requests) {
    }

    @PostMapping("/missing")
    public void missing(@RequestBody MissingRequest request) {
    }
}
