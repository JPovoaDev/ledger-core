package dev.joao.ledger.application;

public interface Repositories {
    AccountRepository accounts();
    TransferRepository transfers();
    LedgerEntryRepository entries();
}
