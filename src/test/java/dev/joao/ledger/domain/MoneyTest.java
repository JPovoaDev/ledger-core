package dev.joao.ledger.domain;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {
    @Test
    void plus_sumsAmountsOfTheSameCurrency() {
        var result = Money.of(1_000, "EUR").plus(Money.of(250, "EUR"));

        assertThat(result).isEqualTo(Money.of(1_250, "EUR"));
    }

  
    @Test
    void plus_throwsWhenCurrenciesDiffer() {
        assertThatThrownBy(() ->
            Money.of(1_000, "EUR").plus(Money.of(250, "USD"))
        ).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void minus_diffAmountsOfTheSameCurrency() {
        var result = Money.of(1_000, "EUR").minus(Money.of(250, "EUR"));

        assertThat(result).isEqualTo(Money.of(750, "EUR"));
    }

  
    @Test
    void minus_throwsWhenCurrenciesDiffer() {
        assertThatThrownBy(() ->
            Money.of(1_000, "EUR").minus(Money.of(250, "USD"))
        ).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void validate_isPositive() {
        var result = Money.of(-1_000,"EUR").isPositive();
        assertThat(result).isEqualTo(false);
    }

    @Test
    void validate_isNegative() {
       var result = Money.of(-1_000,"EUR").isNegative();
        assertThat(result).isEqualTo(true);
    }

    @Test
    void validate_isZero() {
        var result = Money.of(1,"EUR").isZero();
        assertThat(result).isEqualTo(false);
    }
}