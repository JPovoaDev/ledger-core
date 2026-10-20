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
        assertThatThrownBy(() -> Money.of(1_000, "EUR").plus(Money.of(250, "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void minus_diffAmountsOfTheSameCurrency() {
        var result = Money.of(1_000, "EUR").minus(Money.of(250, "EUR"));

        assertThat(result).isEqualTo(Money.of(750, "EUR"));
    }

    @Test
    void minus_throwsWhenCurrenciesDiffer() {
        assertThatThrownBy(() -> Money.of(1_000, "EUR").minus(Money.of(250, "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void validate_isPositive() {
        var result = Money.of(-1_000, "EUR").isPositive();
        assertThat(result).isEqualTo(false);
    }

    @Test
    void validate_isNegative() {
        var result = Money.of(-1_000, "EUR").isNegative();
        assertThat(result).isEqualTo(true);
    }

    @Test
    void validate_isZero() {
        var result = Money.of(1, "EUR").isZero();
        assertThat(result).isEqualTo(false);
    }

    @Test
    void plus_throwsArithmeticException() {
        assertThatThrownBy(() -> Money.of(Long.MAX_VALUE, "EUR").plus(Money.of(250, "EUR")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void minus_throwsArithmeticException() {
        assertThatThrownBy(() -> Money.of(Long.MIN_VALUE, "EUR").minus(Money.of(250, "EUR")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void creat_throwsIllegalException() {
        assertThatThrownBy(() -> Money.of(1_000, "XXX1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void positiveAmount_isOnlyPositive() {
        var money = Money.of(1_000, "EUR");
        assertThat(money.isPositive()).isTrue();
        assertThat(money.isZero()).isFalse();
        assertThat(money.isNegative()).isFalse();
    }

    @Test
    void zeroAmount_isOnlyZero() {
        var money = Money.of(0, "EUR");
        assertThat(money.isZero()).isTrue();
        assertThat(money.isPositive()).isFalse();
        assertThat(money.isNegative()).isFalse();
    }

    @Test
    void negativeAmount_isOnlyNegative() {
        var money = Money.of(-1_000, "EUR");
        assertThat(money.isNegative()).isTrue();
        assertThat(money.isPositive()).isFalse();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    void compare_throwsCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of(1_000, "EUR").compareTo(Money.of(250, "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void moneytoString() {
        Money money = Money.of(1050, "EUR");
        assertThat(money.toString()).isEqualTo("10.50 EUR");
    }

    @Test
    void equals_isFalseWhenCurrenciesDiffer() {
        assertThat(Money.of(1_000, "EUR")).isNotEqualTo(Money.of(1_000, "USD"));
    }

    @Test
    void constructor_rejectsNullCurrency() {
        assertThatThrownBy(() -> new Money(1, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void minus_canProduceNegativeAmount() {
        var result = Money.of(250, "EUR").minus(Money.of(1_000, "EUR"));
        assertThat(result).isEqualTo(Money.of(-750, "EUR"));
    }

    @Test
    void toString_usesZeroDecimalsForJpy() {
        assertThat(Money.of(1_000, "JPY").toString()).isEqualTo("1000 JPY");
    }

    @Test
    void toString_padsSmallAmounts() {
        assertThat(Money.of(5, "EUR").toString()).isEqualTo("0.05 EUR");
    }

    @Test
    void toString_showsNegativeSign() {
        assertThat(Money.of(-1_050, "EUR").toString()).isEqualTo("-10.50 EUR");
    }

    @Test
    void of_rejectsCurrenciesWithoutMinorUnits() {
        assertThatThrownBy(() -> Money.of(1_000, "XXX"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}