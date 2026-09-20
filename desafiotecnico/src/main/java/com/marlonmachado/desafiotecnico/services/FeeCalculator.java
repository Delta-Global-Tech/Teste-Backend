package com.marlonmachado.desafiotecnico.services;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeCalculator {

    private static final long EXEMPTION_THRESHOLD_CENTS = 10_000L;
    private static final long MIN_FEE_CENTS = 100L;
    private static final long MAX_FEE_CENTS = 2_000L;
    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");

    public long calculate(long amountCents) {
        if (amountCents <= EXEMPTION_THRESHOLD_CENTS) {
            return 0L;
        }

        BigDecimal rawFee = BigDecimal.valueOf(amountCents)
                .multiply(FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP);

        long feeCents = rawFee.longValueExact();

        if (feeCents < MIN_FEE_CENTS) {
            return MIN_FEE_CENTS;
        }
        return Math.min(feeCents, MAX_FEE_CENTS);
    }
}
