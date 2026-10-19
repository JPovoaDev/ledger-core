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

    @Test 
    void plus_throwsArithmeticException() {
        assertThatThrownBy(() ->
            Money.of(Long.MAX_VALUE, "EUR").plus(Money.of(250, "EUR"))
        ).isInstanceOf(ArithmeticException.class);
    }

    @Test 
    void minus_throwsArithmeticException() {
        assertThatThrownBy(() ->
            Money.of(Long.MIN_VALUE, "EUR").minus(Money.of(250, "EUR"))
        ).isInstanceOf(ArithmeticException.class);
    }

    @Test 
    void creat_throwsIllegalException() {
        assertThatThrownBy(() ->
            Money.of(1_000, "XXX1")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compareToPositive(){
        var results =  Money.of(1_000, "EUR").compareTo(Money.of(250, "EUR"));
        assertThat(results).isPositive();
    }

    @Test
    void compareToNegative(){
        var results =  Money.of(250, "EUR").compareTo(Money.of(1_000, "EUR"));
        assertThat(results).isNegative();
    }

    @Test
    void compareToZero(){
        var results =  Money.of(1_000, "EUR").compareTo(Money.of(1_000, "EUR"));
        assertThat(results).isZero();
    }

     @Test
    void compare_throwsCurrencyMismatch(){
        assertThatThrownBy(() ->
            Money.of(1_000, "EUR").compareTo(Money.of(250, "USD"))
        ).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test 
    void MoneytoString(){
        Money money = Money.of(1050,"EUR");
        assertThat(money.toString()).isEqualTo("10.50 EUR");
    }
    
}