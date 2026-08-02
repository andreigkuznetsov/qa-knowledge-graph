package example.api;

public enum ApiPath {
    ITEM("/api/items/{id}");

    private final String path;

    ApiPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
