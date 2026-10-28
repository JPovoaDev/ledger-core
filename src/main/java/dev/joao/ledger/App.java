package dev.joao.ledger;

import java.time.Clock;
import java.util.UUID;

import org.flywaydb.core.Flyway;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.joao.ledger.persistence.JdbcTransactionRunner;
import io.javalin.Javalin;
import dev.joao.ledger.application.DepositService;
import dev.joao.ledger.application.TransactionRunner;

public final class App {
    public static void main(String[] args) {

        HikariDataSource dataSource = createDataSource();

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        TransactionRunner runner = new JdbcTransactionRunner(dataSource);
        DepositService depositService = new DepositService(runner,Clock.systemUTC(),UUID::randomUUID);

        var app = Javalin.create(config -> config.routes.get("/health",
                ctx -> ctx.contentType("application/json").result("{\"status\":\"UP\"}")));
        app.start(7070);
    }

    private static HikariDataSource createDataSource() {
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