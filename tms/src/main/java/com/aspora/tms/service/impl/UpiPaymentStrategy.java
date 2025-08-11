package com.aspora.tms.service.impl;


import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.service.PaymentStrategy;
import org.springframework.stereotype.Service;

@Service
public class UpiPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(String paymentMethod) {
        return "UPI".equalsIgnoreCase(paymentMethod);
    }

    @Override
    public TransactionResponseDTO processPayment(TransactionRequestDTO request) {
        // Call UPI API or mock logic
        // Simulate successful payment
        return null;
    }
}