
package dev.joao.ledger.application;

import dev.joao.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository {
    void insert(LedgerEntry entry);

    List<LedgerEntry> findByTransferId(UUID transferId);
}
