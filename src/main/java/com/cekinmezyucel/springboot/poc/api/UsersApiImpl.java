package com.cekinmezyucel.springboot.poc.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.cekinmezyucel.springboot.poc.model.User;
import com.cekinmezyucel.springboot.poc.service.UserService;

@RestController
public class UsersApiImpl implements UsersApi {
  private final UserService userService;

  public UsersApiImpl(UserService userService) {
    this.userService = userService;
  }

  @Override
  public ResponseEntity<List<User>> getUsers() {
    return ResponseEntity.ok(userService.getUsers());
  }

  @Override
  public ResponseEntity<User> createUser(User user) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user));
  }

  @Override
  public ResponseEntity<Void> linkUserToAccount(Long userId, Long accountId) {
    userService.linkUserToAccountWithMembership(userId, accountId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unlinkUserFromAccount(Long userId, Long accountId) {
    userService.unlinkUserFromAccountWithMembership(userId, accountId);
    return ResponseEntity.ok().build();
  }
}
