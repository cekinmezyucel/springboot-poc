package org.cekinmezyucel.springboot.poc.service;

import java.util.Arrays;
import java.util.List;

import org.cekinmezyucel.springboot.poc.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public List<User> getUsers() {
        log.debug("Fetching all users");
        List<User> users = Arrays.asList(
            new User().id(1).email("alice@example.com").name("Alice").surname("Smith"),
            new User().id(2).email("bob@example.com").name("Bob").surname("Johnson")
        );
        log.info("Returned {} users", users.size());
        return users;
    }
}
