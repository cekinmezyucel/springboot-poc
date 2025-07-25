package org.cekinmezyucel.springboot.poc.api;

import java.util.List;

import org.cekinmezyucel.springboot.poc.model.Account;
import org.cekinmezyucel.springboot.poc.service.AccountService;
import org.cekinmezyucel.springboot.poc.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountsApiImpl implements AccountsApi {
    private final AccountService accountService;
    private final UserService userService;

    public AccountsApiImpl(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    @Override
    public ResponseEntity<List<Account>> getAccounts() {
        return ResponseEntity.ok(accountService.getAccounts());
    }

    @Override
    public ResponseEntity<Account> createAccount(Account account) {
        Account created = accountService.createAccount(account);
        // Link account to users if provided
        Integer accountId = created.getId();
        if (accountId != null && created.getUserIds() != null) {
            for (Integer userId : created.getUserIds()) {
                if (userId != null) {
                    userService.linkUserToAccount(userId, accountId);
                    accountService.linkAccountToUser(accountId, userId);
                }
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

}
