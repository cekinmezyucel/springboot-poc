package org.cekinmezyucel.springboot.poc.service;


import java.util.List;
import java.util.Optional;

import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.cekinmezyucel.springboot.poc.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public List<AccountEntity> getAccounts() {
        return accountRepository.findAll();
    }

    public AccountEntity createAccount(AccountEntity account) {
        return accountRepository.save(account);
    }

    public void linkAccountToUser(Integer accountId, Integer userId) {
        Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (accountOpt.isPresent() && userOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            UserEntity user = userOpt.get();
            account.getUsers().add(user);
            accountRepository.save(account);
        }
    }

    public void unlinkAccountFromUser(Integer accountId, Integer userId) {
        Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (accountOpt.isPresent() && userOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            UserEntity user = userOpt.get();
            account.getUsers().remove(user);
            accountRepository.save(account);
        }
    }
}
