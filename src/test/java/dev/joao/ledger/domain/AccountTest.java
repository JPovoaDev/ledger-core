package dev.joao.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static Account account(AccountKind kind, AccountStatus status, long balanceMinor) {
        return new Account(UUID.randomUUID(), "owner-1", kind, status,
                Money.of(balanceMinor, "EUR"), NOW);
    }

    private static Account activeUser(long balanceMinor) {
        return account(AccountKind.USER, AccountStatus.ACTIVE, balanceMinor);
    }

    private static Account activeSystem(long balanceMinor) {
        return account(AccountKind.SYSTEM, AccountStatus.ACTIVE, balanceMinor);
    }


    @Test
    void open_createsActiveUserAccountWithZeroBalance() {
        var id = UUID.randomUUID();

        var account = Account.open(id, "user-123", Currency.getInstance("EUR"), NOW);

        assertThat(account.id()).isEqualTo(id);
        assertThat(account.ownerRef()).isEqualTo("user-123");
        assertThat(account.kind()).isEqualTo(AccountKind.USER);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.balance()).isEqualTo(Money.of(0, "EUR"));
        assertThat(account.createdAt()).isEqualTo(NOW);
    }

    @Test
    void constructor_rejectsNegativeBalanceOnUserAccount() {
        assertThatThrownBy(() -> activeUser(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_allowsNegativeBalanceOnSystemAccount() {
        assertThat(activeSystem(-1).balance()).isEqualTo(Money.of(-1, "EUR"));
    }

    @Test
    void credit_increasesBalance() {
        var credited = activeUser(500).credit(Money.of(250, "EUR"));

        assertThat(credited.balance()).isEqualTo(Money.of(750, "EUR"));
    }

    @Test
    void credit_leavesOriginalUntouched() {
        var original = activeUser(500);

        original.credit(Money.of(250, "EUR"));

        assertThat(original.balance()).isEqualTo(Money.of(500, "EUR"));
    }

    @Test
    void credit_changesOnlyTheBalance() {
        var original = activeUser(500);

        var credited = original.credit(Money.of(250, "EUR"));

        assertThat(credited).usingRecursiveComparison()
                .ignoringFields("balance")
                .isEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1, -100 })
    void credit_rejectsNonPositiveAmounts(long minorUnits) {
        assertThatThrownBy(() -> activeUser(500).credit(Money.of(minorUnits, "EUR")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void credit_rejectsDifferentCurrency() {
        assertThatThrownBy(() -> activeUser(500).credit(Money.of(100, "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EXCLUDE, names = "ACTIVE")
    void credit_rejectsInactiveAccounts(AccountStatus status) {
        var account = account(AccountKind.USER, status, 500);

        assertThatThrownBy(() -> account.credit(Money.of(100, "EUR")))
                .isInstanceOf(AccountNotActiveException.class);
    }



    @Test
    void debit_decreasesBalance() {
        var debited = activeUser(500).debit(Money.of(200, "EUR"));

        assertThat(debited.balance()).isEqualTo(Money.of(300, "EUR"));
    }

    @Test
    void debit_exactBalanceLeavesZero() {
        var debited = activeUser(500).debit(Money.of(500, "EUR"));

        assertThat(debited.balance()).isEqualTo(Money.of(0, "EUR"));
    }

    @Test
    void debit_leavesOriginalUntouched() {
        var original = activeUser(500);

        original.debit(Money.of(200, "EUR"));

        assertThat(original.balance()).isEqualTo(Money.of(500, "EUR"));
    }

    @Test
    void debit_changesOnlyTheBalance() {
        var original = activeUser(500);

        var debited = original.debit(Money.of(200, "EUR"));

        assertThat(debited).usingRecursiveComparison()
                .ignoringFields("balance")
                .isEqualTo(original);
    }

    @Test
    void debit_rejectsMoreThanBalanceOnUserAccount() {
        assertThatThrownBy(() -> activeUser(500).debit(Money.of(501, "EUR")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void debit_onSystemAccountMayGoNegative() {
        var debited = activeSystem(0).debit(Money.of(200, "EUR"));

        assertThat(debited.balance()).isEqualTo(Money.of(-200, "EUR"));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1, -100 })
    void debit_rejectsNonPositiveAmounts(long minorUnits) {
        assertThatThrownBy(() -> activeUser(500).debit(Money.of(minorUnits, "EUR")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void debit_rejectsDifferentCurrency() {
        assertThatThrownBy(() -> activeUser(500).debit(Money.of(100, "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EXCLUDE, names = "ACTIVE")
    void debit_rejectsInactiveAccounts(AccountStatus status) {
        var account = account(AccountKind.USER, status, 500);

        assertThatThrownBy(() -> account.debit(Money.of(100, "EUR")))
                .isInstanceOf(AccountNotActiveException.class);
    }


    @Test
    void operations_checkStatusBeforeAmount() {
        var frozen = account(AccountKind.USER, AccountStatus.FROZEN, 500);

        assertThatThrownBy(() -> frozen.credit(Money.of(0, "EUR")))
                .isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> frozen.debit(Money.of(0, "EUR")))
                .isInstanceOf(AccountNotActiveException.class);
    }
}