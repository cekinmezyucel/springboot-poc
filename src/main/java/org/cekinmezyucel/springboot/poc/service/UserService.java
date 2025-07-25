package org.cekinmezyucel.springboot.poc.service;

import java.util.Arrays;
import java.util.List;

import org.cekinmezyucel.springboot.poc.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public List<User> getUsers() {
        return Arrays.asList(
            new User().id(1).email("alice@example.com").name("Alice").surname("Smith"),
            new User().id(2).email("bob@example.com").name("Bob").surname("Johnson")
        );
    }
}
