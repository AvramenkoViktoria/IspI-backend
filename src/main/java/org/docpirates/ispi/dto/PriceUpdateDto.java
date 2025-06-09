package org.docpirates.ispi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceUpdateDto {
    private BigDecimal newPrice;
}

