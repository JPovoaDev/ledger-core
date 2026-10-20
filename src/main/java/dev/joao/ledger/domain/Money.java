package dev.joao.ledger.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency");
        // um exemplo se a currency for XXX da -1 ent fazemos este teste para dar uma exception
        if (currency.getDefaultFractionDigits() < 0) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
    }

    public static Money of(long minorUnits, String currencyCode) {
        return new Money(minorUnits, Currency.getInstance(currencyCode));
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        long result = avoidSumOverFlow(other.minorUnits);

        return new Money(result, currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        long result = avoidSubOverFlow(other.minorUnits);
        return new Money(result, currency);

    }

    public boolean isPositive() {
        return this.minorUnits > 0;
    }

    public boolean isNegative() {
        return this.minorUnits < 0;
    }

    public boolean isZero() {
        return this.minorUnits == 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    private long avoidSumOverFlow(long otherMinorUnit) {
        return Math.addExact(this.minorUnits, otherMinorUnit);
    }

    private long avoidSubOverFlow(long otherMinorUnit) {
        return Math.subtractExact(this.minorUnits, otherMinorUnit);
    }

    @Override
    public String toString() {
        int decimals = this.currency.getDefaultFractionDigits();
        BigDecimal amount = BigDecimal.valueOf(this.minorUnits);
        BigDecimal result = amount.movePointLeft(decimals);
        return result + " " + this.currency;

    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
           throw new CurrencyMismatchException(currency, other.currency);
        }
    }
}
