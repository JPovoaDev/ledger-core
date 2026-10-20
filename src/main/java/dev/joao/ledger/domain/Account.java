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
    }

    public static Account open(UUID id, String ownerRef, Currency currency, Instant now) {
        return new Account(id, ownerRef, AccountKind.USER, AccountStatus.ACTIVE, new Money(0, currency), now);
    }

    public Account credit(Money amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account not active");
        }
        if (amount.minorUnits() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (amount.currency() != balance().currency()) {
            throw new CurrencyMismatchException("Currency does not match account balance currecy");
        }

        return new Account(id, ownerRef, kind, status, balance().plus(amount), createdAt);
    }

    public Account debit(Money amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account not active");
        }
        if (amount.minorUnits() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (!amount.currency().equals(balance.currency())) {
            throw new CurrencyMismatchException("Currency does not match account balance currecy");
        }

        Money newAmount = balance().minus(amount);

        if (newAmount.minorUnits() < 0 && kind != AccountKind.SYSTEM) {
            throw new InsufficientFundsException("Insufficient founds");
        }
        return new Account(id, ownerRef, kind, status, newAmount, createdAt);
    }
}
