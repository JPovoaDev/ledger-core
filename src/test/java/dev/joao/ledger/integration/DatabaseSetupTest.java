package dev.joao.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class DatabaseSetupTest extends PostgresIntegrationTest {

    @Test
    void flywayAppliedBothMigrations() throws SQLException {
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(2);
    }

    @Test
    void systemAccountsExistForEurAndUsd() throws SQLException {
        assertThat(count("SELECT count(*) FROM account WHERE kind = 'SYSTEM'")).isEqualTo(2);
    }

    @Test
    void secondSystemAccountForTheSameCurrencyIsRejected() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO account (id, owner_ref, currency, kind) "
                        + "VALUES (gen_random_uuid(), 'SYSTEM', 'EUR', 'SYSTEM')"))
                .isInstanceOfSatisfying(SQLException.class,
                        e -> assertThat(e.getSQLState()).isEqualTo("23505")); // unique_violation
    }

    @Test
    void userAccountWithNegativeBalanceIsRejected() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO account (id, owner_ref, currency, kind, balance_minor) "
                        + "VALUES (gen_random_uuid(), 'maria', 'EUR', 'USER', -1)"))
                .isInstanceOfSatisfying(SQLException.class,
                        e -> assertThat(e.getSQLState()).isEqualTo("23514")); // check_violation
    }

    @Test
    void cleanDatabase_removesUserDataAndResetsSystemBalances() throws SQLException {
        execute("INSERT INTO account (id, owner_ref, currency) VALUES (gen_random_uuid(), 'maria', 'EUR')");
        execute("UPDATE account SET balance_minor = -100 WHERE kind = 'SYSTEM' AND currency = 'EUR'");

        cleanDatabase();

        assertThat(count("SELECT count(*) FROM account WHERE kind = 'USER'")).isZero();
        assertThat(count("SELECT count(*) FROM account WHERE kind = 'SYSTEM'")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM account WHERE balance_minor <> 0")).isZero();
    }

    private static void execute(String sql) throws SQLException {
        try (var connection = DATA_SOURCE.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long count(String sql) throws SQLException {
        try (var connection = DATA_SOURCE.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}