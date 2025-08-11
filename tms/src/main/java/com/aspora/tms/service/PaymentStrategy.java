package com.aspora.tms.service;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;

public interface PaymentStrategy {
    boolean supports(String paymentMethod);
    TransactionResponseDTO processPayment(TransactionRequestDTO request);
}