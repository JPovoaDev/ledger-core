package dev.joao.ledger;
import org.flywaydb.core.Flyway;
import io.javalin.Javalin;

public final class App {
    public static void main(String[] args) {
        Flyway.configure()
              .dataSource(env("DB_URL", "jdbc:postgresql://localhost:5432/ledger"),
                          env("DB_USER", "ledger"),
                          env("DB_PASSWORD", "ledger"))
              .load()
              .migrate();

        var app = Javalin.create(config ->
            config.routes.get("/health", ctx ->
                ctx.contentType("application/json").result("{\"status\":\"UP\"}")));
        app.start(7070);
    }

    private static String env(String key, String fallback) {
        var value = System.getenv(key);
        return value != null ? value : fallback;
    }
}