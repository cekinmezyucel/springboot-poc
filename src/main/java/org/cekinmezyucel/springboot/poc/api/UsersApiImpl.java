package org.cekinmezyucel.springboot.poc.api;

import java.util.List;

import org.cekinmezyucel.springboot.poc.model.User;
import org.cekinmezyucel.springboot.poc.service.AccountService;
import org.cekinmezyucel.springboot.poc.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersApiImpl implements UsersApi {
    private final UserService userService;
    private final AccountService accountService;

    public UsersApiImpl(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @Override
    public ResponseEntity<User> createUser(User user) {
        User created = userService.createUser(user);
        Integer userId = created.getId();
        if (userId != null && created.getAccountIds() != null) {
            for (Integer accountId : created.getAccountIds()) {
                if (accountId != null) {
                    accountService.linkAccountToUser(accountId, userId);
                    userService.linkUserToAccount(userId, accountId);
                }
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<Void> linkUserToAccount(Integer userId, Integer accountId) {
        userService.linkUserToAccount(userId, accountId);
        accountService.linkAccountToUser(accountId, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unlinkUserFromAccount(Integer userId, Integer accountId) {
        userService.unlinkUserFromAccount(userId, accountId);
        accountService.unlinkAccountFromUser(accountId, userId);
        return ResponseEntity.ok().build();
    }

}
