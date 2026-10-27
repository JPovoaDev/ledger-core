package dev.joao.ledger.application;
import dev.joao.ledger.domain.Transfer;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    void insert(Transfer transfer);
    Optional<Transfer> findById(UUID id);
} 