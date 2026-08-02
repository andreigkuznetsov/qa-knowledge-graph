package example.repository;

import org.springframework.data.repository.CrudRepository;

public interface AuditRepository extends CrudRepository<Object, Long> {
}
