package example.service;

import example.repository.ExplicitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FieldService {
    @Autowired
    private ExplicitRepository repository;

    public void create() {
        repository.store();
    }
}
