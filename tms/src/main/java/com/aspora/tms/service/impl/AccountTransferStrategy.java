package com.aspora.tms.service.impl;


import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.entity.Account;
import com.aspora.tms.entity.Transaction;
import com.aspora.tms.enums.TransactionStatus;
import com.aspora.tms.enums.TransactionType;
import com.aspora.tms.exception.ResourceNotFoundException;
import com.aspora.tms.exception.InsufficientBalanceException;
import com.aspora.tms.mappers.TransactionMapper;
import com.aspora.tms.repository.AccountRepository;
import com.aspora.tms.repository.TransactionRepository;
import com.aspora.tms.service.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountTransferStrategy implements PaymentStrategy {

    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final TransactionMapper transactionMapper;

    @Override
    public boolean supports(String paymentMethod) {
        return "ACCOUNT".equalsIgnoreCase(paymentMethod);
    }

    @Override
    @Transactional
    public synchronized TransactionResponseDTO processPayment(TransactionRequestDTO request) {
        Account from = accountRepo.findById(request.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account to = accountRepo.findById(request.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Target account not found"));

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        accountRepo.save(from);
        accountRepo.save(to);

        Transaction txn = new Transaction();
        txn.setFromAccount(from);
        txn.setToAccount(to);
        txn.setAmount(request.getAmount());
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setType(TransactionType.TRANSFER);
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());
        transactionRepo.save(txn);

        return transactionMapper.toDTO(txn);
    }
}