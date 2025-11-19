package com.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResponse {

    private boolean success;
    private String transactionId;
    private String errorMessage;
    private String gatewayResponseCode;

    @Override
    public String toString() {
        return String.format("PaymentGatewayResponse{success=%s, transactionId='%s', errorMessage='%s', gatewayResponseCode='%s'}",
                success, transactionId, errorMessage, gatewayResponseCode);
    }
}
