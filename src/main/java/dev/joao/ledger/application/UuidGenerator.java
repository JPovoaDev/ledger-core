package dev.joao.ledger.application;

import java.util.UUID;

@FunctionalInterface
public interface UuidGenerator {
    UUID generate();
}
