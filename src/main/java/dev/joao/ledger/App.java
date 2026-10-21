package dev.joao.ledger;

import org.flywaydb.core.Flyway;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.javalin.Javalin;

public final class App {
    public static void main(String[] args) {

        HikariDataSource dataSource = creatDataSource();

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        var app = Javalin.create(config -> config.routes.get("/health",
                ctx -> ctx.contentType("application/json").result("{\"status\":\"UP\"}")));
        app.start(7070);
    }

    private static HikariDataSource creatDataSource() {
        var config = new HikariConfig();
        config.setJdbcUrl(env("DB_URL", "jdbc:postgresql://localhost:5432/ledger"));
        config.setUsername(env("DB_USER", "ledger"));
        config.setPassword(env("DB_PASSWORD", "ledger"));
        return new HikariDataSource(config);

    }

    private static String env(String key, String fallback) {
        var value = System.getenv(key);
        return value != null ? value : fallback;
    }
}