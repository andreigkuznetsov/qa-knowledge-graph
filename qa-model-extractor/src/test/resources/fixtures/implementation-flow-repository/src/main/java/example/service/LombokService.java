package example.service;

import example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LombokService {
    private final UserRepository repository;

    public void create() {
        repository.save(new Object());
    }
}
