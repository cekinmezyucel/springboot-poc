package org.cekinmezyucel.springboot.poc.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cekinmezyucel.springboot.poc.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final Map<Integer, User> users = new HashMap<>();
    private int userIdSeq = 1;

    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public User createUser(User user) {
        user.setId(userIdSeq++);
        if (user.getAccountIds() == null) {
            user.setAccountIds(new ArrayList<>());
        }
        users.put(user.getId(), user);
        return user;
    }

    public void linkUserToAccount(int userId, int accountId) {
        User user = users.get(userId);
        if (user != null && !user.getAccountIds().contains(accountId)) {
            user.getAccountIds().add(accountId);
        }
    }

    public void unlinkUserFromAccount(int userId, int accountId) {
        User user = users.get(userId);
        if (user != null) {
            user.getAccountIds().remove(Integer.valueOf(accountId));
        }
    }
}
