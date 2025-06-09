package org.docpirates.ispi.dto;

import org.docpirates.ispi.entity.Subscription;

import java.math.BigDecimal;

public record SubscriptionDto(
        Long subscription_id,
        String name,
        String description,
        BigDecimal price
) {
    public static SubscriptionDto from(Subscription s) {
        return new SubscriptionDto(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getPrice()
        );
    }
}

