package dev.joao.ledger.integration;

import static dev.joao.ledger.integration.TestFixtures.transfer;
import static dev.joao.ledger.integration.TestFixtures.userAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.TransactionRunner;
import dev.joao.ledger.domain.Account;
import dev.joao.ledger.persistence.JdbcTransactionRunner;

class JdbcTransactionRunnerTest extends PostgresIntegrationTest {

    private final TransactionRunner runner = new JdbcTransactionRunner(DATA_SOURCE);

    @Test
    void run_commitsTheWorkAndReturnsItsResult() {
        var account = userAccount("EUR", 0);

        var result = runner.run(repos -> {
            repos.accounts().insert(account);
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(find(account.id())).contains(account);
    }

    @Test
    void run_rollsBackAndRethrowsTheSameExceptionWhenTheWorkFails() {
        var account = userAccount("EUR", 0);

        assertThatThrownBy(() -> runner.run(repos -> {
            repos.accounts().insert(account);
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");

        assertThat(find(account.id())).isEmpty();
    }

    @Test
    void run_leavesNoPartialStateWhenALaterStatementFails() {
        var account = userAccount("EUR", 0);
        // the source and destination accounts do not exist: the foreign key rejects it
        var orphan = transfer(UUID.randomUUID(), UUID.randomUUID(), "client-1", "key-1");

        assertThatThrownBy(() -> runner.run(repos -> {
            repos.accounts().insert(account);
            repos.transfers().insert(orphan);
            return null;
        })).isInstanceOf(PersistenceException.class);

        assertThat(find(account.id())).isEmpty();
    }

    @Test
    void run_keepsUncommittedWorkInvisibleToOtherConnections() {
        var account = userAccount("EUR", 0);

        runner.run(repos -> {
            repos.accounts().insert(account);
            assertThat(countOnAnotherConnection(account)).isZero();
            return null;
        });

        assertThat(countOnAnotherConnection(account)).isEqualTo(1);
    }

    @Test
    void run_returnsTheConnectionToThePoolEvenWhenTheWorkFails() {
        assertThatThrownBy(() -> runner.run(repos -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(DATA_SOURCE.getHikariPoolMXBean().getActiveConnections()).isZero();
    }

    private Optional<Account> find(UUID id) {
        return runner.run(repos -> repos.accounts().findById(id));
    }

    private static long countOnAnotherConnection(Account account) {
        try (var connection = DATA_SOURCE.getConnection();
             var statement = connection.prepareStatement("SELECT count(*) FROM account WHERE id = ?")) {
            statement.setObject(1, account.id());
            try (var rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}