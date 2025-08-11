package com.aspora.tms.service;

import com.aspora.tms.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountService {

    Account createAccount(Account account);

    List<Account> getAllAccounts();

    Optional<Account> getAccountById(Long id);

    Account updateAccount(Long id, Account updatedAccount);

    void deleteAccount(Long id);
}