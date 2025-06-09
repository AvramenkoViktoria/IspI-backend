package org.docpirates.ispi.dto;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        String subscription,
        LocalDateTime lastActivationDate,
        LocalDateTime next_payment_date
) {}
