package dev.joao.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

public class AccountTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private Account openAccount() {
        return Account.open(
                UUID.randomUUID(), "user-123", EUR, NOW);
    }

    private Account accountWithBalance(long cents) {
        return openAccount().credit(Money.of(cents, "EUR"));
    }

    @Test
    void opensActiveUserAccountWithZeroBalance() {
        Account account = openAccount();

        assertAll(
                () -> assertEquals("user-123", account.ownerRef()),
                () -> assertEquals(AccountKind.USER, account.kind()),
                () -> assertEquals(AccountStatus.ACTIVE, account.status()),
                () -> assertEquals(0L, account.balance().minorUnits()),
                () -> assertEquals(EUR, account.balance().currency()),
                () -> assertEquals(NOW, account.createdAt()));
    }

    @Test
    void creditIncreasesBalanceWithoutChangingOriginal() {
        Account original = openAccount();

        Account credited = original.credit(Money.of(500, "EUR"));

        assertEquals(0L, original.balance().minorUnits());
        assertEquals(500L, credited.balance().minorUnits());
    }

    @Test
    void debitDecreasesBalanceWithoutChangingOriginal() {
        Account original = accountWithBalance(500);

        Account debited = original.debit(Money.of(200, "EUR"));

        assertEquals(500L, original.balance().minorUnits());
        assertEquals(300L, debited.balance().minorUnits());
    }

    @Test
    void debitExactBalanceLeavesZero() {
        Account account = accountWithBalance(500);

        Account debited = account.debit(Money.of(500, "EUR"));

        assertEquals(0L, debited.balance().minorUnits());
    }

    @Test
    void debitMoreThanBalanceThrowsInsufficientFunds() {
        Account account = accountWithBalance(500);

        assertThrows(
                InsufficientFundsException.class,
                () -> account.debit(Money.of(501, "EUR")));
    }

    @Test
    void creditZeroAmountThrowsInvalidAmount() {
        Account account = openAccount();

        assertThrows(
                InvalidAmountException.class,
                () -> account.credit(Money.of(0, "EUR")));
    }

    @Test
    void creditNegativeAmountThrowsInvalidAmount() {
        Account account = openAccount();

        assertThrows(
                InvalidAmountException.class,
                () -> account.credit(Money.of(-100, "EUR")));
    }

    @Test
    void debitZeroAmountThrowsInvalidAmount() {
        Account account = accountWithBalance(500);

        assertThrows(
                InvalidAmountException.class,
                () -> account.debit(Money.of(0, "EUR")));
    }

    @Test
    void debitNegativeAmountThrowsInvalidAmount() {
        Account account = accountWithBalance(500);

        assertThrows(
                InvalidAmountException.class,
                () -> account.debit(Money.of(-100, "EUR")));
    }

    @Test
    void rejectsDifferentCurrency() {
        Account account = openAccount();

        assertAll(
                () -> assertThrows(
                        CurrencyMismatchException.class,
                        () -> account.credit(Money.of(100, "USD"))),
                () -> assertThrows(
                        CurrencyMismatchException.class,
                        () -> account.debit(Money.of(100, "USD"))));
    }

    @Test
    void frozenAccountCannotReceiveCredit() {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Account account = new Account(
                UUID.randomUUID(),
                "user-123",
                AccountKind.USER,
                AccountStatus.FROZEN,
                Money.of(500, "EUR"),
                now);

        assertThrows(
                AccountNotActiveException.class,
                () -> account.credit(Money.of(100, "EUR")));
    }

    @Test
    void frozenAccountCannotReceiveDebit() {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Account account = new Account(
                UUID.randomUUID(),
                "user-123",
                AccountKind.USER,
                AccountStatus.FROZEN,
                Money.of(500, "EUR"),
                now);

        assertThrows(
                AccountNotActiveException.class,
                () -> account.debit(Money.of(100, "EUR")));
    }

    void closedAccountCannotReceiveCredit() {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Account account = new Account(
                UUID.randomUUID(),
                "user-123",
                AccountKind.USER,
                AccountStatus.CLOSED,
                Money.of(500, "EUR"),
                now);

        assertThrows(
                AccountNotActiveException.class,
                () -> account.credit(Money.of(100, "EUR")));
    }

    void closedAccountCannotReceiveDebit() {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Account account = new Account(
                UUID.randomUUID(),
                "user-123",
                AccountKind.USER,
                AccountStatus.CLOSED,
                Money.of(500, "currency"),
                now);

        assertThrows(
                AccountNotActiveException.class,
                () -> account.debit(Money.of(100, "EUR")));
    }

    @Test
    void systemAccountCanHaveNegativeBalance() {
        Account system = new Account(
                UUID.randomUUID(),
                "system-123",
                AccountKind.SYSTEM,
                AccountStatus.ACTIVE,
                Money.of(100, "EUR"),
                NOW);

        Account debited = system.debit(Money.of(200, "EUR"));

        assertEquals(-100L, debited.balance().minorUnits());
    }
}