package dev.joao.ledger.integration;

import static dev.joao.ledger.integration.TestFixtures.NOW;
import static dev.joao.ledger.integration.TestFixtures.transfer;
import static dev.joao.ledger.integration.TestFixtures.userAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.TransactionRunner;
import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.Direction;
import dev.joao.ledger.domain.LedgerEntry;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.domain.Transfer;
import dev.joao.ledger.persistence.JdbcTransactionRunner;

class JdbcLedgerEntryRepositoryTest extends PostgresIntegrationTest {

    private final TransactionRunner runner = new JdbcTransactionRunner(DATA_SOURCE);

    private Account source;
    private Account dest;
    private Transfer firstTransfer;
    private Transfer secondTransfer;

    @BeforeEach
    void persistAccountsAndTransfers() {
        source = userAccount("EUR", 0);
        dest = userAccount("EUR", 0);
        firstTransfer = transfer(source.id(), dest.id(), "client-1", "key-1");
        secondTransfer = transfer(source.id(), dest.id(), "client-1", "key-2");
        runner.run(repos -> {
            repos.accounts().insert(source);
            repos.accounts().insert(dest);
            repos.transfers().insert(firstTransfer);
            repos.transfers().insert(secondTransfer);
            return null;
        });
    }

    @Test
    void findByTransferId_returnsBothEntriesOfTheTransfer() {
        var entries = firstTransfer.toEntries(UUID.randomUUID(), UUID.randomUUID(), NOW);
        insertAll(entries);

        var found = runner.run(repos -> repos.entries().findByTransferId(firstTransfer.id()));

        assertThat(found).containsExactlyInAnyOrderElementsOf(entries);
    }

    @Test
    void findByTransferId_doesNotMixEntriesOfDifferentTransfers() {
        var first = firstTransfer.toEntries(UUID.randomUUID(), UUID.randomUUID(), NOW);
        var second = secondTransfer.toEntries(UUID.randomUUID(), UUID.randomUUID(), NOW);
        insertAll(first);
        insertAll(second);

        var found = runner.run(repos -> repos.entries().findByTransferId(secondTransfer.id()));

        assertThat(found).containsExactlyInAnyOrderElementsOf(second);
    }

    @Test
    void findByTransferId_returnsAnEmptyListForAnUnknownTransfer() {
        var found = runner.run(repos -> repos.entries().findByTransferId(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    @Test
    void insert_rejectsAnEntryOfAnUnknownTransfer() {
        var orphan = new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), source.id(),
                Direction.DEBIT, Money.of(500, "EUR"), NOW);

        assertThatThrownBy(() -> insertAll(java.util.List.of(orphan)))
                .isInstanceOf(PersistenceException.class);
    }

    private void insertAll(java.util.List<LedgerEntry> entries) {
        runner.run(repos -> {
            entries.forEach(repos.entries()::insert);
            return null;
        });
    }
}