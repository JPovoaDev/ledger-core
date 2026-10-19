package dev.joao.ledger.domain;

import java.util.Currency;
import java.util.Objects;

public record Money (long minorUnits, Currency currency) implements Comparable<Money>{

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(long minorUnits, String currencyCode) {
        return new Money(minorUnits, Currency.getInstance(currencyCode));
    }

    public Money plus(Money other) {
        requiresSameCurrency(other);
        long result = avoidSumOverFlow(other.minorUnits);

        return new Money(result,currency);
    }

    public Money minus (Money other){
        requiresSameCurrency(other);
        long result = avoidSubOverFlow(other.minorUnits);

        return new Money(result,currency);
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
    
    @Override 
    public int compareTo(Money other){
        requiresSameCurrency(other);
        return (this.minorUnits > other.minorUnits) ? 1 : ((this.minorUnits < other.minorUnits) ? -1 :0);
    }

    public long avoidSumOverFlow(long otherMinorUnit){
        return Math.addExact(this.minorUnits, otherMinorUnit);
    } 
    
    public long avoidSubOverFlow(long otherMinorUnit){
        return Math.subtractExact(this.minorUnits, otherMinorUnit);
    }

    public void requiresSameCurrency(Money other){
        if(!this.currency.equals(other.currency)){
                throw new CurrencyMismatchException("Cannot add amounts with differnt currencies");
            }
    }
}
