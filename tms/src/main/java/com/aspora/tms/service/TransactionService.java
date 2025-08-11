package com.aspora.tms.service;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;

public interface TransactionService {
    TransactionResponseDTO transferFunds(TransactionRequestDTO request);
}
