package dev.joao.ledger.domain;

import java.util.Objects;
import java.util.UUID;

import java.time.Instant;

public record LedgerEntry(

        UUID id,
        UUID transferId,
        UUID accountId,
        Direction direction,
        Money amount,
        Instant createdAt) {

    public LedgerEntry {

        Objects.requireNonNull(id);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(createdAt);

        if (!amount.isPositive()) {
            throw new InvalidAmountException("Ledger entry amount must be positive");
        }
    }
}
