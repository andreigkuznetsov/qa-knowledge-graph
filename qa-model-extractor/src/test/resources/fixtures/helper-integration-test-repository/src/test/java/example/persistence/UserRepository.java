package example.persistence;

public interface UserRepository {
    Object findByValue(String value);
}
