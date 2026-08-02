package example.support;

public enum ApiPath {
    REGISTER("/auth/enum-register");

    private final String path;

    ApiPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
