package org.cekinmezyucel.springboot.poc.repository;

import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {}
