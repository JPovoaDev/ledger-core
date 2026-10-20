package dev.joao.ledger.domain;

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
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (transferId == null) {
            throw new IllegalArgumentException("transferId cannot be null");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (amount.minorUnits() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }
}
