package org.cekinmezyucel.springboot.poc.api;

import java.util.List;
import java.util.stream.Collectors;

import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
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

    private Account toModel(AccountEntity entity) {
        Account model = new Account();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setIndustry(entity.getType());
        // Optionally map users if needed
        return model;
    }

    private AccountEntity toEntity(Account model) {
        AccountEntity entity = new AccountEntity();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setType(model.getIndustry());
        // Optionally map users if needed
        return entity;
    }

    @Override
    public ResponseEntity<List<Account>> getAccounts() {
        List<Account> accounts = accountService.getAccounts().stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

    @Override
    public ResponseEntity<Account> createAccount(Account account) {
        AccountEntity createdEntity = accountService.createAccount(toEntity(account));
        Account created = toModel(createdEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

}
