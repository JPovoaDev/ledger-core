package dev.joao.ledger.domain;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import java.util.UUID;

public record Account(
        UUID id,
        String ownerRef,
        AccountKind kind,
        AccountStatus status,
        Money balance,
        Instant createdAt

) {
    public Account {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ownerRef);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(status);
        Objects.requireNonNull(balance);
        Objects.requireNonNull(createdAt);

        if (kind == AccountKind.USER && balance.isNegative()) {
            throw new IllegalArgumentException("A USER account cannot have a negative balance");
        }
    }

    public static Account open(UUID id, String ownerRef, Currency currency, Instant now) {
        return new Account(id, ownerRef, AccountKind.USER, AccountStatus.ACTIVE, new Money(0, currency), now);
    }

    public Account credit(Money amount) {
        requireActive();
        requirePositive(amount);

        return withBalance(balance().plus(amount));
    }

    public Account debit(Money amount) {
        requireActive();
        requirePositive(amount);
        Money newAmount = balance().minus(amount);

        if (kind == AccountKind.USER && newAmount.isNegative()) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        return withBalance(newAmount);
    }

    private Account withBalance(Money balance) {
        return new Account(id, ownerRef, kind, status, balance, createdAt);
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account not active");
        }
    }

    private void requirePositive(Money amount) {
        if (!amount.isPositive()) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }
}
