package dev.joao.ledger.domain;
import java.time.Instant;
import java.util.List;
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
        Instant createdAt
) {
    public Transfer {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey cannot be blank");
        }
        if (requestHash == null || requestHash.isBlank()) {
            throw new IllegalArgumentException("requestHash cannot be blank");
        }
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("sourceAccountId cannot be null");
        }
        if (destAccountId == null) {
            throw new IllegalArgumentException("destAccountId cannot be null");
        }
        if (sourceAccountId.equals(destAccountId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (amount.minorUnits() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }
     public List<LedgerEntry> toEntries (UUID debitEntryId,UUID creditEntryId,Instant now) {
        LedgerEntry debit = new LedgerEntry (debitEntryId,id,sourceAccountId,Direction.DEBIT,amount,now);

        LedgerEntry credit = new LedgerEntry (creditEntryId,id,destAccountId,Direction.CREDIT,amount,now);

        return List.of(debit, credit);
    }
}