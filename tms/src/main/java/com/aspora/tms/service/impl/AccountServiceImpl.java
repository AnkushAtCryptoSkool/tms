package com.aspora.tms.service.impl;

import com.aspora.tms.entity.Account;
import com.aspora.tms.repository.AccountRepository;
import com.aspora.tms.service.AccountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    @Override
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    @Override
    public Account updateAccount(Long id, Account updatedAccount) {
        return accountRepository.findById(id)
                .map(existing -> {
                    existing.setOwner(updatedAccount.getOwner());
                    existing.setBalance(updatedAccount.getBalance());
                    return accountRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));
    }

    @Override
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found with id " + id);
        }
        accountRepository.deleteById(id);
    }
}