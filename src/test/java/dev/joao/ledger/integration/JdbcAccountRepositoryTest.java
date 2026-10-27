package dev.joao.ledger.integration;

import static dev.joao.ledger.integration.TestFixtures.NOW;
import static dev.joao.ledger.integration.TestFixtures.userAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.TransactionRunner;
import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.AccountKind;
import dev.joao.ledger.domain.AccountStatus;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.persistence.JdbcTransactionRunner;

class JdbcAccountRepositoryTest extends PostgresIntegrationTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private final TransactionRunner runner = new JdbcTransactionRunner(DATA_SOURCE);


    @Test
    void insertThenFindById_returnsTheSameAccount() {
        var account = userAccount("EUR", 1_500);

        insert(account);

        assertThat(find(account.id())).contains(account);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void roundTrip_preservesEveryStatus(AccountStatus status) {
        var account = new Account(UUID.randomUUID(), "owner-1", AccountKind.USER,
                status, Money.of(0, "USD"), NOW);

        insert(account);

        assertThat(find(account.id())).contains(account);
    }

    @Test
    void findById_returnsEmptyForAnUnknownId() {
        assertThat(find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void insert_failsWhenTheIdAlreadyExists() {
        var account = userAccount("EUR", 0);
        insert(account);

        assertThatThrownBy(() -> insert(account)).isInstanceOf(PersistenceException.class);
    }



    @ParameterizedTest
    @ValueSource(strings = { "EUR", "USD" })
    void findSystemAccount_returnsTheSeededAccount(String currency) {
        var found = runner.run(repos ->
                repos.accounts().findSystemAccount(Currency.getInstance(currency)));

        assertThat(found).hasValueSatisfying(account -> {
            assertThat(account.kind()).isEqualTo(AccountKind.SYSTEM);
            assertThat(account.balance()).isEqualTo(Money.of(0, currency));
        });
    }

    @Test
    void findSystemAccount_returnsEmptyWhenTheCurrencyHasNone() {
        var found = runner.run(repos ->
                repos.accounts().findSystemAccount(Currency.getInstance("GBP")));

        assertThat(found).isEmpty();
    }



    @Test
    void updateBalance_changesOnlyTheBalance() {
        var account = userAccount("EUR", 1_500);
        insert(account);

        update(account.id(), Money.of(2_000, "EUR"));

        var found = find(account.id()).orElseThrow();
        assertThat(found.balance()).isEqualTo(Money.of(2_000, "EUR"));
        assertThat(found).usingRecursiveComparison()
                .ignoringFields("balance")
                .isEqualTo(account);
    }

    @Test
    void updateBalance_allowsANegativeBalanceOnTheSystemAccount() {
        var system = runner.run(repos -> repos.accounts().findSystemAccount(EUR)).orElseThrow();

        update(system.id(), Money.of(-5_000, "EUR"));

        assertThat(find(system.id()).orElseThrow().balance()).isEqualTo(Money.of(-5_000, "EUR"));
    }

    @Test
    void updateBalance_isRejectedByTheDatabaseForANegativeUserBalance() {
        var account = userAccount("EUR", 1_500);
        insert(account);

        assertThatThrownBy(() -> update(account.id(), Money.of(-1, "EUR")))
                .isInstanceOf(PersistenceException.class);
        assertThat(find(account.id()).orElseThrow().balance()).isEqualTo(Money.of(1_500, "EUR"));
    }

    @Test
    void updateBalance_failsForAnUnknownAccount() {
        assertThatThrownBy(() -> update(UUID.randomUUID(), Money.of(100, "EUR")))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void updateBalance_refusesToChangeTheCurrency() {
        var account = userAccount("EUR", 1_500);
        insert(account);

        assertThatThrownBy(() -> update(account.id(), Money.of(100, "USD")))
                .isInstanceOf(PersistenceException.class);
        assertThat(find(account.id()).orElseThrow().balance()).isEqualTo(Money.of(1_500, "EUR"));
    }



    private void insert(Account account) {
        runner.run(repos -> {
            repos.accounts().insert(account);
            return null;
        });
    }

    private void update(UUID id, Money balance) {
        runner.run(repos -> {
            repos.accounts().updateBalance(id, balance);
            return null;
        });
    }

    private Optional<Account> find(UUID id) {
        return runner.run(repos -> repos.accounts().findById(id));
    }
}