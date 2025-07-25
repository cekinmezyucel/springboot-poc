package org.cekinmezyucel.springboot.poc.api;

import java.util.List;
import java.util.stream.Collectors;

import org.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.cekinmezyucel.springboot.poc.model.User;
import org.cekinmezyucel.springboot.poc.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Void> linkUserToAccount(Integer userId, Integer accountId) {
        userService.linkUserToAccountWithMembership(userId, accountId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unlinkUserFromAccount(Integer userId, Integer accountId) {
        userService.unlinkUserFromAccountWithMembership(userId, accountId);
        return ResponseEntity.ok().build();
    }

}
