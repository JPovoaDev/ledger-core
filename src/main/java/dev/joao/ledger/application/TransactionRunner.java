package dev.joao.ledger.application;

import java.util.function.Function;

public interface TransactionRunner {
    <T> T run(Function<Repositories,T>work);

} 

