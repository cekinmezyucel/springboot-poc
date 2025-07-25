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

    private User toModel(UserEntity entity) {
        User model = new User();
        model.setId(entity.getId());
        model.setEmail(entity.getEmail());
        model.setName(entity.getName());
        model.setSurname(entity.getSurname());
        // Optionally map accounts if needed
        return model;
    }

    private UserEntity toEntity(User model) {
        UserEntity entity = new UserEntity();
        entity.setId(model.getId());
        entity.setEmail(model.getEmail());
        entity.setName(model.getName());
        entity.setSurname(model.getSurname());
        // Optionally map accounts if needed
        return entity;
    }

    @Override
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userService.getUsers().stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @Override
    public ResponseEntity<User> createUser(User user) {
        UserEntity createdEntity = userService.createUser(toEntity(user));
        User created = toModel(createdEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
