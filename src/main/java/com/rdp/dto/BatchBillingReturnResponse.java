package com.rdp.dto;

import java.math.BigDecimal;
import java.util.List;

public record BatchBillingReturnResponse(
        List<BillingReturnResponse> returnedItems,
        BigDecimal totalReturnRefund,
        String billingNumber,
        Long billingId,
        BigDecimal newBalance,
        String message
) {}
