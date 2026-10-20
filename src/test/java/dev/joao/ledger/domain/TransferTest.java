package dev.joao.ledger.domain;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferTest {

    @Test
    void shouldCreateTransfer() {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        Transfer transfer = new Transfer(
                UUID.randomUUID(),
                "client-1",
                "key-1",
                "hash-1",
                sourceId,
                destId,
                Money.of(500, "EUR"),
                TransferStatus.PENDING,
                null,
                Instant.parse("2026-01-01T10:00:00Z")
        );

        assertEquals(sourceId, transfer.sourceAccountId());
        assertEquals(destId, transfer.destAccountId());
        assertEquals(500, transfer.amount().minorUnits());
        assertEquals(TransferStatus.PENDING, transfer.status());
    }

    @Test
    void shouldRejectSameSourceAndDestination() {
        UUID accountId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                new Transfer(
                        UUID.randomUUID(),
                        "client-1",
                        "key-1",
                        "hash-1",
                        accountId,
                        accountId,
                        Money.of(500, "EUR"),
                        TransferStatus.PENDING,
                        null,
                        Instant.parse("2026-01-01T10:00:00Z")
                )
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(InvalidAmountException.class, () ->
                new Transfer(
                        UUID.randomUUID(),
                        "client-1",
                        "key-1",
                        "hash-1",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Money.of(0, "EUR"),
                        TransferStatus.PENDING,
                        null,
                        Instant.parse("2026-01-01T10:00:00Z")
                )
        );
    }

    @Test
    void shouldGenerateTwoBalancedEntries() {
        UUID transferId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        UUID debitId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Transfer transfer = new Transfer(
                transferId,
                "client-1",
                "key-1",
                "hash-1",
                sourceId,
                destId,
                Money.of(500, "EUR"),
                TransferStatus.PENDING,
                null,
                now
        );

        List<LedgerEntry> entries = transfer.toEntries(
                debitId,
                creditId,
                now
        );

        assertEquals(2, entries.size());

        LedgerEntry debit = entries.get(0);
        LedgerEntry credit = entries.get(1);

        assertEquals(Direction.DEBIT, debit.direction());
        assertEquals(Direction.CREDIT, credit.direction());

        assertEquals(sourceId, debit.accountId());
        assertEquals(destId, credit.accountId());

        assertEquals(transferId, debit.transferId());
        assertEquals(transferId, credit.transferId());

        assertEquals(500, debit.amount().minorUnits());
        assertEquals(500, credit.amount().minorUnits());

        assertEquals(debitId, debit.id());
        assertEquals(creditId, credit.id());
    }
}
