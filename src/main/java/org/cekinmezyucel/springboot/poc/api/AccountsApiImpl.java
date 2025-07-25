package org.cekinmezyucel.springboot.poc.api;

import java.util.List;

import org.cekinmezyucel.springboot.poc.model.Account;
import org.cekinmezyucel.springboot.poc.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountsApiImpl implements AccountsApi {
    private final AccountService accountService;

    public AccountsApiImpl(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<List<Account>> getAccounts() {
        return ResponseEntity.ok(accountService.getAccounts());
    }

    @Override
    public ResponseEntity<Account> createAccount(Account account) {
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
