package example.service;

import example.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ConstructorService {
    private final UserRepository repository;

    public ConstructorService(UserRepository repository) {
        this.repository = repository;
    }

    public void create() {
        repository.save(new Object());
    }
}
