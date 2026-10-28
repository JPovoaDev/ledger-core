package dev.joao.ledger.integration;

import static dev.joao.ledger.integration.TestFixtures.NOW;
import static dev.joao.ledger.integration.TestFixtures.userAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.SQLException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.joao.ledger.application.AccountNotFoundException;
import dev.joao.ledger.application.DepositService;
import dev.joao.ledger.application.DuplicateIdempotencyKeyException;
import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.TransactionRunner;
import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.AccountKind;
import dev.joao.ledger.domain.AccountNotActiveException;
import dev.joao.ledger.domain.AccountStatus;
import dev.joao.ledger.domain.CurrencyMismatchException;
import dev.joao.ledger.domain.Direction;
import dev.joao.ledger.domain.InvalidAmountException;
import dev.joao.ledger.domain.LedgerEntry;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.domain.TransferStatus;
import dev.joao.ledger.persistence.JdbcTransactionRunner;

class DepositServiceTest extends PostgresIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final TransactionRunner runner = new JdbcTransactionRunner(DATA_SOURCE);
    private final DepositService service = new DepositService(runner, CLOCK, UUID::randomUUID);

    // ---------- the happy path ----------

    @Test
    void deposit_addsToTheExistingBalance() {
        // starts at 1000 on purpose: with a zero start, a bug that OVERWRITES the
        // balance would pass
        var account = persisted(userAccount("EUR", 1_000));

        service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1");

        assertThat(balanceOf(account.id())).isEqualTo(Money.of(6_000, "EUR"));
    }

    @Test
    void deposit_debitsTheSystemAccount() {
        var account = persisted(userAccount("EUR", 0));

        service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1");

        assertThat(systemAccount("EUR").balance()).isEqualTo(Money.of(-5_000, "EUR"));
    }

    @Test
    void deposit_returnsACompletedTransferFromTheSystemAccountToTheTarget() {
        var account = persisted(userAccount("EUR", 0));

        var transfer = service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1");

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.sourceAccountId()).isEqualTo(systemAccount("EUR").id());
        assertThat(transfer.destAccountId()).isEqualTo(account.id());
        assertThat(transfer.amount()).isEqualTo(Money.of(5_000, "EUR"));
        assertThat(transfer.idempotencyKey()).isEqualTo("key-1");
        var persistedTransfer = runner.run(repos -> repos.transfers().findById(transfer.id()));

        assertThat(persistedTransfer).contains(transfer);
    }

    @Test
    void deposit_createsOneDebitOnTheSystemAndOneCreditOnTheTarget() {
        var account = persisted(userAccount("EUR", 0));
        var system = systemAccount("EUR");

        var transfer = service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1");

        var entries = runner.run(repos -> repos.entries().findByTransferId(transfer.id()));
        assertThat(entries)
                .extracting(LedgerEntry::direction, LedgerEntry::accountId, LedgerEntry::amount)
                .containsExactlyInAnyOrder(
                        tuple(Direction.DEBIT, system.id(), Money.of(5_000, "EUR")),
                        tuple(Direction.CREDIT, account.id(), Money.of(5_000, "EUR")));
    }

    @Test
    void deposit_stampsTheTransferAndTheEntriesWithTheClockTime() {
        var account = persisted(userAccount("EUR", 0));

        var transfer = service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1");

        var entries = runner.run(repos -> repos.entries().findByTransferId(transfer.id()));
        assertThat(transfer.createdAt()).isEqualTo(NOW);
        assertThat(entries).extracting(LedgerEntry::createdAt).containsOnly(NOW);
    }

    @Test
    void deposit_keepsEveryCurrencyBalancedAcrossAllAccounts() {
        var maria = persisted(userAccount("EUR", 0));
        var rui = persisted(userAccount("EUR", 0));
        var john = persisted(userAccount("USD", 0));

        service.deposit(maria.id(), Money.of(5_000, "EUR"), "key-1");
        service.deposit(rui.id(), Money.of(1_234, "EUR"), "key-2");
        service.deposit(john.id(), Money.of(777, "USD"), "key-3");

        // invariant 3: for each currency, the sum of all balances (system included) is
        // 0
        assertThat(strings("SELECT currency FROM account GROUP BY currency HAVING sum(balance_minor) <> 0"))
                .isEmpty();
    }

    @Test
    void deposit_keepsEveryBalanceEqualToTheSumOfItsEntries() {
        var maria = persisted(userAccount("EUR", 0));
        var rui = persisted(userAccount("EUR", 0));

        service.deposit(maria.id(), Money.of(5_000, "EUR"), "key-1");
        service.deposit(maria.id(), Money.of(250, "EUR"), "key-2");
        service.deposit(rui.id(), Money.of(1_234, "EUR"), "key-3");

        // invariant 2: balance = sum(CREDIT) - sum(DEBIT). Returns the accounts that
        // violate it.
        assertThat(strings("""
                SELECT a.id::text
                FROM account a
                LEFT JOIN ledger_entry e ON e.account_id = a.id
                GROUP BY a.id, a.balance_minor
                HAVING a.balance_minor <> COALESCE(SUM(CASE WHEN e.direction = 'CREDIT'
                                                            THEN e.amount_minor
                                                            ELSE -e.amount_minor END), 0)
                """)).isEmpty();
    }

    @Test
    void deposit_toAnUnknownAccount_persistsNothing() {
        assertThatThrownBy(() -> service.deposit(UUID.randomUUID(), Money.of(5_000, "EUR"), "key-1"))
                .isInstanceOf(AccountNotFoundException.class);

        assertNothingPersisted();
    }

    @Test
    void deposit_toAFrozenAccount_persistsNothing() {
        var frozen = persisted(new Account(UUID.randomUUID(), "owner-1", AccountKind.USER,
                AccountStatus.FROZEN, Money.of(0, "EUR"), NOW));

        assertThatThrownBy(() -> service.deposit(frozen.id(), Money.of(5_000, "EUR"), "key-1"))
                .isInstanceOf(AccountNotActiveException.class);

        assertNothingPersisted();
    }

    @Test
    void deposit_inAnotherCurrency_persistsNothing() {
        var account = persisted(userAccount("EUR", 0));

        assertThatThrownBy(() -> service.deposit(account.id(), Money.of(5_000, "USD"), "key-1"))
                .isInstanceOf(CurrencyMismatchException.class);

        assertNothingPersisted();
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -100 })
    void deposit_rejectsNonPositiveAmounts(long minorUnits) {
        var account = persisted(userAccount("EUR", 0));

        assertThatThrownBy(() -> service.deposit(account.id(), Money.of(minorUnits, "EUR"), "key-1"))
                .isInstanceOf(InvalidAmountException.class);

        assertNothingPersisted();
    }

    @Test
    void deposit_inACurrencyWithoutSystemAccount_persistsNothing() {
        var account = persisted(userAccount("GBP", 0));

        assertThatThrownBy(() -> service.deposit(account.id(), Money.of(5_000, "GBP"), "key-1"))
                .isInstanceOf(IllegalStateException.class);

        assertNothingPersisted();
    }

    @Test
    void deposit_withARepeatedKey_isRejectedAndChangesNothing() {
        var account = persisted(userAccount("EUR", 0));
        service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1");

        assertThatThrownBy(() -> service.deposit(account.id(), Money.of(5_000, "EUR"), "key-1"))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);

        assertThat(balanceOf(account.id())).isEqualTo(Money.of(5_000, "EUR"));
        assertThat(systemAccount("EUR").balance()).isEqualTo(Money.of(-5_000, "EUR"));
        assertThat(count("SELECT count(*) FROM ledger_entry")).isEqualTo(2);
    }

    @Test
    void deposit_leavesNoPartialStateWhenTheSecondEntryFails() {
        var account = persisted(userAccount("EUR", 0));
        var sameId = UUID.randomUUID();
        // transfer id, debit id and credit id are all equal: the transfer and the first
        // entry
        // are inserted, then the second entry violates the primary key and the work
        // fails midway
        var brokenService = new DepositService(runner, CLOCK, () -> sameId);

        assertThatThrownBy(() -> brokenService.deposit(account.id(), Money.of(5_000, "EUR"), "key-1"))
                .isInstanceOf(PersistenceException.class);

        assertNothingPersisted();
    }

    private Account persisted(Account account) {
        runner.run(repos -> {
            repos.accounts().insert(account);
            return null;
        });
        return account;
    }

    private Money balanceOf(UUID id) {
        return runner.run(repos -> repos.accounts().findById(id)).orElseThrow().balance();
    }

    private Account systemAccount(String currency) {
        return runner.run(repos -> repos.accounts().findSystemAccount(Currency.getInstance(currency)))
                .orElseThrow();
    }

    // the accounts of these tests start at zero, so any balance change would show
    // up here
    private void assertNothingPersisted() {
        assertThat(count("SELECT count(*) FROM transfer")).isZero();
        assertThat(count("SELECT count(*) FROM ledger_entry")).isZero();
        assertThat(count("SELECT count(*) FROM account WHERE balance_minor <> 0")).isZero();
    }

    private static long count(String sql) {
        return Long.parseLong(strings(sql).get(0));
    }

    private static List<String> strings(String sql) {
        try (var connection = DATA_SOURCE.getConnection();
                var statement = connection.createStatement();
                var rs = statement.executeQuery(sql)) {
            var result = new ArrayList<String>();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}