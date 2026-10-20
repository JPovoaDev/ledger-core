package dev.joao.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransferTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static Transfer transfer(UUID source, UUID dest, Money amount, TransferStatus status) {
        return new Transfer(UUID.randomUUID(), "client-1", "key-1", "hash-1",
                source, dest, amount, status, null, NOW);
    }

    private static Transfer completedTransfer() {
        return transfer(UUID.randomUUID(), UUID.randomUUID(),
                Money.of(500, "EUR"), TransferStatus.COMPLETED);
    }

    private static Transfer withKeys(String clientId, String idempotencyKey, String requestHash) {
        return new Transfer(UUID.randomUUID(), clientId, idempotencyKey, requestHash,
                UUID.randomUUID(), UUID.randomUUID(), Money.of(500, "EUR"),
                TransferStatus.PENDING, null, NOW);
    }

    private static Money total(List<LedgerEntry> entries, Direction direction) {
        return entries.stream()
                .filter(entry -> entry.direction() == direction)
                .map(LedgerEntry::amount)
                .reduce(Money::plus)
                .orElseThrow();
    }


    @Test
    void constructor_keepsEveryFieldInTheRightPlace() {
        var id = UUID.randomUUID();
        var source = UUID.randomUUID();
        var dest = UUID.randomUUID();

        var transfer = new Transfer(id, "client-1", "key-1", "hash-1",
                source, dest, Money.of(500, "EUR"), TransferStatus.PENDING, null, NOW);

        assertThat(transfer.id()).isEqualTo(id);
        assertThat(transfer.sourceAccountId()).isEqualTo(source);
        assertThat(transfer.destAccountId()).isEqualTo(dest);
        assertThat(transfer.amount()).isEqualTo(Money.of(500, "EUR"));
        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.failureReason()).isNull();
    }

    @Test
    void constructor_rejectsSameSourceAndDestination() {
        var accountId = UUID.randomUUID();

        assertThatThrownBy(() ->
                transfer(accountId, accountId, Money.of(500, "EUR"), TransferStatus.PENDING))
                .isInstanceOf(InvalidTransferException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -500 })
    void constructor_rejectsNonPositiveAmount(long minorUnits) {
        assertThatThrownBy(() ->
                transfer(UUID.randomUUID(), UUID.randomUUID(),
                        Money.of(minorUnits, "EUR"), TransferStatus.PENDING))
                .isInstanceOf(InvalidAmountException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void constructor_rejectsBlankClientId(String blank) {
        assertThatThrownBy(() -> withKeys(blank, "key-1", "hash-1"))
                .isInstanceOf(InvalidTransferException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void constructor_rejectsBlankIdempotencyKey(String blank) {
        assertThatThrownBy(() -> withKeys("client-1", blank, "hash-1"))
                .isInstanceOf(InvalidTransferException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void constructor_rejectsBlankRequestHash(String blank) {
        assertThatThrownBy(() -> withKeys("client-1", "key-1", blank))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void toEntries_debitsTheSourceAndCreditsTheDestination() {
        var source = UUID.randomUUID();
        var dest = UUID.randomUUID();
        var debitId = UUID.randomUUID();
        var creditId = UUID.randomUUID();
        var transfer = transfer(source, dest, Money.of(500, "EUR"), TransferStatus.COMPLETED);

        var entries = transfer.toEntries(debitId, creditId, NOW);

        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(LedgerEntry::direction)
                .containsExactly(Direction.DEBIT, Direction.CREDIT);
        assertThat(entries).extracting(LedgerEntry::accountId)
                .containsExactly(source, dest);
        assertThat(entries).extracting(LedgerEntry::id)
                .containsExactly(debitId, creditId);
        assertThat(entries).extracting(LedgerEntry::transferId)
                .containsOnly(transfer.id());
        assertThat(entries).extracting(LedgerEntry::amount)
                .containsOnly(Money.of(500, "EUR"));
    }

    @Test
    void toEntries_isBalanced() {
        var entries = completedTransfer()
                .toEntries(UUID.randomUUID(), UUID.randomUUID(), NOW);

        assertThat(total(entries, Direction.DEBIT))
                .isEqualTo(total(entries, Direction.CREDIT));
    }

    @Test
    void toEntries_rejectsFailedTransfer() {
        var failed = transfer(UUID.randomUUID(), UUID.randomUUID(),
                Money.of(500, "EUR"), TransferStatus.FAILED);

        assertThatThrownBy(() -> failed.toEntries(UUID.randomUUID(), UUID.randomUUID(), NOW))
                .isInstanceOf(InvalidTransferException.class);
    }
}