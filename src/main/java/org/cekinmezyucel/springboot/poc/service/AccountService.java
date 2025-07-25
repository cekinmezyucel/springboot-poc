package org.cekinmezyucel.springboot.poc.service;

import java.util.Arrays;
import java.util.List;

import org.cekinmezyucel.springboot.poc.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    public List<Account> getAccounts() {
        log.debug("Fetching all accounts");
        List<Account> accounts = Arrays.asList(
            new Account().id(1).name("Acme Corp").industry("Technology"),
            new Account().id(2).name("Beta LLC").industry("Finance")
        );
        log.info("Returned {} accounts", accounts.size());
        return accounts;
    }
}
