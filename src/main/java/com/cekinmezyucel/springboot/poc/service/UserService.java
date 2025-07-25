package com.cekinmezyucel.springboot.poc.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cekinmezyucel.springboot.poc.entity.AccountEntity;
import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import com.cekinmezyucel.springboot.poc.model.User;
import com.cekinmezyucel.springboot.poc.repository.AccountRepository;
import com.cekinmezyucel.springboot.poc.repository.UserRepository;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;

  public UserService(UserRepository userRepository, AccountRepository accountRepository) {
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
  }

  private static User toModel(UserEntity entity) {
    User model = new User();
    model.setId(entity.getId());
    model.setEmail(entity.getEmail());
    model.setName(entity.getName());
    model.setSurname(entity.getSurname());
    return model;
  }

  private static UserEntity toEntity(User model) {
    UserEntity entity = new UserEntity();
    entity.setId(model.getId());
    entity.setEmail(model.getEmail());
    entity.setName(model.getName());
    entity.setSurname(model.getSurname());
    return entity;
  }

  public List<User> getUsers() {
    return userRepository.findAll().stream().map(UserService::toModel).toList();
  }

  public User createUser(User user) {
    return toModel(userRepository.save(toEntity(user)));
  }

  public void linkUserToAccountWithMembership(Long userId, Long accountId) {
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

  public void unlinkUserFromAccountWithMembership(Long userId, Long accountId) {
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

  public void linkUserToAccount(Long userId, Long accountId) {
    Optional<UserEntity> userOpt = userRepository.findById(userId);
    Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
    if (userOpt.isPresent() && accountOpt.isPresent()) {
      UserEntity user = userOpt.get();
      AccountEntity account = accountOpt.get();
      user.getAccounts().add(account);
      userRepository.save(user);
    }
  }

  public void unlinkUserFromAccount(Long userId, Long accountId) {
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
