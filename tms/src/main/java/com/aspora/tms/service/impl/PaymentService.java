package com.aspora.tms.service.impl;


import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.service.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Main entry point for processing payments.
 * Uses PaymentStrategy to decide how to process a request.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final List<PaymentStrategy> strategies;

    public TransactionResponseDTO process(TransactionRequestDTO request) {
        return strategies.stream()
                .filter(s -> s.supports(String.valueOf(request.getPaymentMethod())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported payment method: " + request.getPaymentMethod()
                ))
                .processPayment(request);
    }
}