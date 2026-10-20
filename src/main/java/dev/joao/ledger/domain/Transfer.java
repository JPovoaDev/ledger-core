package dev.joao.ledger.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Transfer(
        UUID id,
        String clientId,
        String idempotencyKey,
        String requestHash,
        UUID sourceAccountId,
        UUID destAccountId,
        Money amount,
        TransferStatus status,
        String failureReason,
        Instant createdAt) {

    public Transfer {
        Objects.requireNonNull(id);
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(requestHash);
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(destAccountId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);

        requireNotBlank(clientId, "clientId");
        requireNotBlank(idempotencyKey, "idempotencyKey");
        requireNotBlank(requestHash, "requestHash");

        if (sourceAccountId.equals(destAccountId)) {
            throw new InvalidTransferException("Source and destination accounts must be different");
        }

        if (!amount.isPositive()) {
            throw new InvalidAmountException("Transfer amount must be positive");
        }
    }

    private static void requireNotBlank(String value, String name) {
        if (value.isBlank()) {
            throw new InvalidTransferException(name + " must not be blank");
        }
    }

    public List<LedgerEntry> toEntries(UUID debitEntryId, UUID creditEntryId, Instant now) {

        if (status == TransferStatus.FAILED) {
            throw new InvalidTransferException("A failed transfer cannot generate ledger entries");
        }

        LedgerEntry debit = new LedgerEntry(debitEntryId, id, sourceAccountId, Direction.DEBIT, amount, now);

        LedgerEntry credit = new LedgerEntry(creditEntryId, id, destAccountId, Direction.CREDIT, amount, now);

        return List.of(debit, credit);
    }
}