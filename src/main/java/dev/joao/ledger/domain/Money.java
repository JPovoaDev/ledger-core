package dev.joao.ledger.domain;

import java.util.Currency;
import java.util.Objects;

public record Money(long minorUnits, Currency currency) {
    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(long minorUnits, String currencyCode) {
        return new Money(minorUnits, Currency.getInstance(currencyCode));
    }

    public Money plus(Money other) {
        if(!this.currency.equals(other.currency)){
            throw new CurrencyMismatchException("Cannot add amounts with differnt currencies");
        }
        return new Money(minorUnits + other.minorUnits, currency);
    }
}
