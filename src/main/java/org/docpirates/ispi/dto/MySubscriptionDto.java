package org.docpirates.ispi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MySubscriptionDto(
        Long subscription_id,
        String name,
        String description,
        BigDecimal price,
        LocalDateTime last_activation_date,
        LocalDateTime next_payment_date
) {}
