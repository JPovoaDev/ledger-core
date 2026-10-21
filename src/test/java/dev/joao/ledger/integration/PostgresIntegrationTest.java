package dev.joao.ledger.integration;

import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");
    protected static final HikariDataSource DATA_SOURCE;

    static {
        POSTGRES.start();
        var config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        DATA_SOURCE = new HikariDataSource(config);
        Flyway.configure().dataSource(DATA_SOURCE).load().migrate();
    }

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (var connection = DATA_SOURCE.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("DELETE FROM ledger_entry");
            statement.execute("DELETE FROM transfer");
            statement.execute("DELETE FROM account WHERE kind = 'USER'");
            statement.execute("UPDATE account SET balance_minor = 0");
        }
    }
}
