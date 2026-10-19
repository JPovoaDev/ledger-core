package dev.joao.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void plus_sumsAmountsOfTheSameCurrency() {
        var result = Money.of(1_000, "EUR").plus(Money.of(250, "EUR"));

        assertThat(result).isEqualTo(Money.of(1_250, "EUR"));
    }
}