package example.service;

import example.repository.AuditRepository;
import example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MultipleRepositoryService {
    private final UserRepository users;
    private final AuditRepository audits;

    public void run() {
        users.count();
        audits.count();
    }
}
