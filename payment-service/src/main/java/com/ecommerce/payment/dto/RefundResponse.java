package com.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private boolean success;
    private String refundId;
    private BigDecimal amount;
    private String message;
}
