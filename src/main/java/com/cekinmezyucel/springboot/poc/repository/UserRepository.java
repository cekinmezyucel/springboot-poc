package com.cekinmezyucel.springboot.poc.repository;

import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {}
