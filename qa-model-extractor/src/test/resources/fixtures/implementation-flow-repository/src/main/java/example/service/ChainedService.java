package example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChainedService {
    private final OtherService otherService;

    public void run() {
        otherService.run();
    }
}
