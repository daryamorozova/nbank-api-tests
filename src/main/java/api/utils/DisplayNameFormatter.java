package api.utils;

public class DisplayNameFormatter {
    private DisplayNameFormatter() {}

    public static String forUi(String raw) {
        return java.util.Optional.ofNullable(raw)
                .filter(s -> !s.isBlank())
                .orElse("Noname");
    }
}