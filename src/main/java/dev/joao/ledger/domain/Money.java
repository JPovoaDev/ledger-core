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
        requiresSameCurrency(other);
        avoidSumOverFlow(other.minorUnits);
        return new Money(minorUnits + other.minorUnits,currency);
    }

    public Money minus (Money other){
        requiresSameCurrency(other);
       
        return new Money(minorUnits - other.minorUnits,currency);
    }

    public Boolean isPositive(){
        return this.minorUnits > 0;
    }

    public Boolean isNegative(){
        return this.minorUnits < 0;
    }

    public Boolean isZero(){
        return this.minorUnits == 0 ;
    }

    public void avoidSumOverFlow(long otherMinorUnit){
        Math.addExact(this.minorUnits, otherMinorUnit);

    }
    public void requiresSameCurrency(Money other){
        if(!this.currency.equals(other.currency)){
                throw new CurrencyMismatchException("Cannot add amounts with differnt currencies");
            }
    }
}
