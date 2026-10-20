package dev.joao.ledger.domain;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LedgerEntryTest {

    @Test
    void createsLedgerEntryWithPositiveAmount() {
        LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Direction.DEBIT,
                Money.of(500, "EUR"),
                Instant.parse("2026-01-01T10:00:00Z")
        );

        assertEquals(Direction.DEBIT, entry.direction());
        assertEquals(500L, entry.amount().minorUnits());
    }

    @Test
    void zeroAmountThrowsInvalidAmount() {
        assertThrows(
                InvalidAmountException.class,
                () -> new LedgerEntry(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Direction.DEBIT,
                        Money.of(0, "EUR"),
                        Instant.parse("2026-01-01T10:00:00Z")
                )
        );
    }

    @Test
    void negativeAmountThrowsInvalidAmount() {
        assertThrows(
                InvalidAmountException.class,
                () -> new LedgerEntry(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Direction.CREDIT,
                        Money.of(-100, "EUR"),
                        Instant.parse("2026-01-01T10:00:00Z")
                )
        );
    }
}
