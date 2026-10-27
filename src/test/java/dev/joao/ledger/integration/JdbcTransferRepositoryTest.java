package dev.joao.ledger.integration;

import static dev.joao.ledger.integration.TestFixtures.NOW;
import static dev.joao.ledger.integration.TestFixtures.transfer;
import static dev.joao.ledger.integration.TestFixtures.userAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.joao.ledger.application.DuplicateIdempotencyKeyException;
import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.TransactionRunner;
import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.domain.Transfer;
import dev.joao.ledger.domain.TransferStatus;
import dev.joao.ledger.persistence.JdbcTransactionRunner;

class JdbcTransferRepositoryTest extends PostgresIntegrationTest {

    private final TransactionRunner runner = new JdbcTransactionRunner(DATA_SOURCE);

    private Account source;
    private Account dest;

    @BeforeEach
    void persistTwoAccounts() {
        source = userAccount("EUR", 0);
        dest = userAccount("EUR", 0);
        runner.run(repos -> {
            repos.accounts().insert(source);
            repos.accounts().insert(dest);
            return null;
        });
    }

    @Test
    void insertThenFindById_returnsTheSameTransfer() {
        var transfer = transfer(source.id(), dest.id(), "client-1", "key-1");

        insert(transfer);

        assertThat(find(transfer.id())).contains(transfer);
    }

    @Test
    void roundTrip_keepsTheFailureReasonOfAFailedTransfer() {
        var failed = new Transfer(UUID.randomUUID(), "client-1", "key-1", "hash-1",
                source.id(), dest.id(), Money.of(500, "EUR"),
                TransferStatus.FAILED, "INSUFFICIENT_FUNDS", NOW);

        insert(failed);

        assertThat(find(failed.id())).contains(failed);
    }

    @Test
    void findById_returnsEmptyForAnUnknownId() {
        assertThat(find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void insert_rejectsTheSameIdempotencyKeyFromTheSameClient() {
        insert(transfer(source.id(), dest.id(), "client-1", "key-1"));

        assertThatThrownBy(() -> insert(transfer(source.id(), dest.id(), "client-1", "key-1")))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);
    }

    @Test
    void insert_acceptsTheSameIdempotencyKeyFromADifferentClient() {
        insert(transfer(source.id(), dest.id(), "client-1", "key-1"));
        var other = transfer(source.id(), dest.id(), "client-2", "key-1");

        insert(other);

        assertThat(find(other.id())).isPresent();
    }

    @Test
    void insert_rejectsUnknownAccounts() {
        var orphan = transfer(UUID.randomUUID(), UUID.randomUUID(), "client-1", "key-1");

        assertThatThrownBy(() -> insert(orphan)).isInstanceOf(PersistenceException.class);
    }

    @Test
    void insert_reportsADuplicatedIdAsAPersistenceErrorNotAnIdempotencyConflict() {
        var first = transfer(source.id(), dest.id(), "client-1", "key-1");
        insert(first);
        var sameIdOtherKey = new Transfer(first.id(), "client-1", "key-2", "hash-2",
                source.id(), dest.id(), Money.of(500, "EUR"),
                TransferStatus.COMPLETED, null, NOW);

        assertThatThrownBy(() -> insert(sameIdOtherKey))
                .isInstanceOf(PersistenceException.class);
    }

    private void insert(Transfer transfer) {
        runner.run(repos -> {
            repos.transfers().insert(transfer);
            return null;
        });
    }

    private Optional<Transfer> find(UUID id) {
        return runner.run(repos -> repos.transfers().findById(id));
    }
}