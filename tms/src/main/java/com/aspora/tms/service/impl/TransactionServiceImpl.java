package com.aspora.tms.service.impl;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.entity.Account;
import com.aspora.tms.entity.Transaction;
import com.aspora.tms.enums.PaymentMethod;
import com.aspora.tms.enums.TransactionStatus;
import com.aspora.tms.enums.TransactionType;
import com.aspora.tms.mappers.TransactionMapper;
import com.aspora.tms.repository.AccountRepository;
import com.aspora.tms.repository.TransactionRepository;
import com.aspora.tms.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransactionResponseDTO transferFunds(TransactionRequestDTO request) {
        Transaction txn;

        switch (request.getPaymentMethod()) {
            case ACCOUNT:
                txn = handleAccountTransfer(request);
                break;
            case UPI:
                throw new UnsupportedOperationException("UPI transfer not yet implemented");
            case NET_BANKING:
                throw new UnsupportedOperationException("NetBanking transfer not yet implemented");
            default:
                throw new RuntimeException("Unknown payment method");
        }

        Transaction savedTxn = transactionRepo.save(txn);
        return transactionMapper.toDTO(savedTxn);
    }

    private Transaction handleAccountTransfer(TransactionRequestDTO request) {
        Account from = accountRepo.findById(request.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));
        Account to = accountRepo.findById(request.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
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

        return txn;
    }
}
