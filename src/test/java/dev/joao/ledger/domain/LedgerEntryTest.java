package dev.joao.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class LedgerEntryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static LedgerEntry entryOf(Direction direction, long amountMinor) {
        return new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                direction, Money.of(amountMinor, "EUR"), NOW);
    }

    @Test
    void constructor_keepsEveryFieldInTheRightPlace() {
        var id = UUID.randomUUID();
        var transferId = UUID.randomUUID();
        var accountId = UUID.randomUUID();

        var entry = new LedgerEntry(id, transferId, accountId,
                Direction.CREDIT, Money.of(500, "EUR"), NOW);

        assertThat(entry.id()).isEqualTo(id);
        assertThat(entry.transferId()).isEqualTo(transferId);
        assertThat(entry.accountId()).isEqualTo(accountId);
        assertThat(entry.direction()).isEqualTo(Direction.CREDIT);
        assertThat(entry.amount()).isEqualTo(Money.of(500, "EUR"));
        assertThat(entry.createdAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void constructor_acceptsPositiveAmountInBothDirections(Direction direction) {
        assertThat(entryOf(direction, 500).direction()).isEqualTo(direction);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1, -100 })
    void constructor_rejectsNonPositiveAmount(long amountMinor) {
        assertThatThrownBy(() -> entryOf(Direction.DEBIT, amountMinor))
                .isInstanceOf(InvalidAmountException.class);
    }
}