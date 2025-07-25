package org.cekinmezyucel.springboot.poc.service;

import java.util.List;
import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.cekinmezyucel.springboot.poc.model.Account;
import org.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public List<Account> getAccounts() {
    return accountRepository.findAll().stream().map(AccountService::toModel).toList();
  }

  public Account createAccount(Account account) {
    return toModel(accountRepository.save(toEntity(account)));
  }

  private static Account toModel(AccountEntity accountEntity) {
    Account model = new Account();
    model.setId(accountEntity.getId());
    model.setName(accountEntity.getName());
    model.setIndustry(accountEntity.getType());
    return model;
  }

  public AccountEntity toEntity(Account model) {
    AccountEntity entity = new AccountEntity();
    entity.setId(model.getId());
    entity.setName(model.getName());
    entity.setType(model.getIndustry());
    return entity;
  }
}
