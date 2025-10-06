package com.cekinmezyucel.springboot.poc.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cekinmezyucel.springboot.poc.model.Account;
import com.cekinmezyucel.springboot.poc.service.AccountService;

@Service
public class AccountsApiImpl implements AccountsApiDelegate {
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
    return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(account));
  }
}
