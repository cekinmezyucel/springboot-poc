package org.cekinmezyucel.springboot.poc.service;

import java.util.List;
import java.util.Optional;

import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.cekinmezyucel.springboot.poc.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public List<UserEntity> getUsers() {
        return userRepository.findAll();
    }

    public UserEntity createUser(UserEntity user) {
        return userRepository.save(user);
    }

    public UserEntity createUserWithMemberships(UserEntity user, List<Integer> accountIds) {
        UserEntity created = createUser(user);
        if (created.getId() != null && accountIds != null) {
            for (Integer accountId : accountIds) {
                if (accountId != null) {
                    linkUserToAccountWithMembership(created.getId(), accountId);
                }
            }
        }
        return created;
    }

    public void linkUserToAccountWithMembership(Integer userId, Integer accountId) {
        linkUserToAccount(userId, accountId);
        // Bidirectional link
        Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (accountOpt.isPresent() && userOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            UserEntity user = userOpt.get();
            account.getUsers().add(user);
            accountRepository.save(account);
        }
    }

    public void unlinkUserFromAccountWithMembership(Integer userId, Integer accountId) {
        unlinkUserFromAccount(userId, accountId);
        // Bidirectional unlink
        Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (accountOpt.isPresent() && userOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            UserEntity user = userOpt.get();
            account.getUsers().remove(user);
            accountRepository.save(account);
        }
    }

    public void linkUserToAccount(Integer userId, Integer accountId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
        if (userOpt.isPresent() && accountOpt.isPresent()) {
            UserEntity user = userOpt.get();
            AccountEntity account = accountOpt.get();
            user.getAccounts().add(account);
            userRepository.save(user);
        }
    }

    public void unlinkUserFromAccount(Integer userId, Integer accountId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
        if (userOpt.isPresent() && accountOpt.isPresent()) {
            UserEntity user = userOpt.get();
            AccountEntity account = accountOpt.get();
            user.getAccounts().remove(account);
            userRepository.save(user);
        }
    }
}
