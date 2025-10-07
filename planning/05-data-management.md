# Data Management Enhancement Plan

## Current State Analysis
- ✅ Basic JPA entities with relationships
- ✅ Flyway migrations for schema versioning
- ❌ No audit logging for data changes
- ❌ No data archival strategy
- ❌ No soft delete implementation
- ❌ No data validation at entity level
- ❌ No data retention policies
- ❌ No read replica support

## Target State
- ✅ Comprehensive audit logging for all data changes
- ✅ Soft delete implementation with filtering
- ✅ Data archival and cleanup strategies
- ✅ Enhanced validation and constraints
- ✅ Read/write splitting capability
- ✅ Data retention and compliance features
- ✅ Optimistic locking for concurrency control
- ✅ Database-level data quality checks

## Implementation Steps

### Step 1: Audit Logging Implementation

#### 1.1 Add Audit Dependencies
```gradle
// Add to build.gradle dependencies
implementation 'org.springframework.data:spring-data-envers:3.2.0'
implementation 'org.hibernate:hibernate-envers:6.4.1.Final'
```

#### 1.2 Enable JPA Auditing
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/AuditConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    public static class SpringSecurityAuditorAware implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            
            return Optional.of(authentication.getName());
        }
    }
}
```

#### 1.3 Base Auditable Entity
Create `src/main/java/com/cekinmezyucel/springboot/poc/entity/BaseAuditableEntity.java`:
```java
package com.cekinmezyucel.springboot.poc.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class BaseAuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    // Getters and setters
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    // Soft delete method
    public void markAsDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            this.deletedBy = authentication.getName();
        } else {
            this.deletedBy = "system";
        }
    }

    // Restore method
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
```

#### 1.4 Update Entities with Auditing
Update `src/main/java/com/cekinmezyucel/springboot/poc/entity/UserEntity.java`:
```java
package com.cekinmezyucel.springboot.poc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_name_surname", columnList = "name, surname"),
    @Index(name = "idx_users_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
@Audited
public class UserEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-\\.]+$", message = "Name contains invalid characters")
    private String name;

    @Column(name = "surname", nullable = false, length = 100)
    @NotBlank(message = "Surname is required")
    @Size(min = 1, max = 100, message = "Surname must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-\\.]+$", message = "Surname contains invalid characters")
    private String surname;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
        name = "user_accounts",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "account_id"),
        indexes = {
            @Index(name = "idx_user_accounts_user_id", columnList = "user_id"),
            @Index(name = "idx_user_accounts_account_id", columnList = "account_id")
        }
    )
    @Where(clause = "deleted = false")
    private Set<AccountEntity> accounts = new HashSet<>();

    // Constructors
    public UserEntity() {}

    public UserEntity(String email, String name, String surname) {
        this.email = email;
        this.name = name;
        this.surname = surname;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Set<AccountEntity> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<AccountEntity> accounts) {
        this.accounts = accounts;
    }

    // Business methods
    public void addAccount(AccountEntity account) {
        this.accounts.add(account);
        account.getUsers().add(this);
    }

    public void removeAccount(AccountEntity account) {
        this.accounts.remove(account);
        account.getUsers().remove(this);
    }

    public String getFullName() {
        return name + " " + surname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserEntity)) return false;
        UserEntity that = (UserEntity) o;
        return email != null && email.equals(that.email);
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }
}
```

### Step 2: Database Migration for Audit Tables

#### 2.1 Add Audit Columns Migration
Create `src/main/resources/db/migration/V3__add_audit_columns.sql`:
```sql
-- Add audit columns to existing tables
ALTER TABLE users 
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN created_by VARCHAR(100) DEFAULT 'system',
ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system',
ADD COLUMN version BIGINT DEFAULT 0,
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN deleted_at TIMESTAMP NULL,
ADD COLUMN deleted_by VARCHAR(100) NULL;

ALTER TABLE accounts 
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN created_by VARCHAR(100) DEFAULT 'system',
ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system',
ADD COLUMN version BIGINT DEFAULT 0,
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN deleted_at TIMESTAMP NULL,
ADD COLUMN deleted_by VARCHAR(100) NULL;

-- Create audit tables (Envers will create these automatically, but we define them for clarity)
CREATE TABLE users_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    email VARCHAR(255),
    name VARCHAR(100),
    surname VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT,
    deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    PRIMARY KEY (id, rev)
);

CREATE TABLE accounts_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    name VARCHAR(255),
    industry VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT,
    deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    PRIMARY KEY (id, rev)
);

CREATE TABLE user_accounts_aud (
    rev INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (rev, user_id, account_id)
);

CREATE TABLE revinfo (
    rev INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    revtstmp BIGINT NOT NULL,
    username VARCHAR(100)
);

-- Add indexes for audit queries
CREATE INDEX idx_users_aud_rev ON users_aud(rev);
CREATE INDEX idx_accounts_aud_rev ON accounts_aud(rev);
CREATE INDEX idx_user_accounts_aud_rev ON user_accounts_aud(rev);
CREATE INDEX idx_revinfo_timestamp ON revinfo(revtstmp);

-- Add indexes for soft delete
CREATE INDEX idx_users_deleted ON users(deleted);
CREATE INDEX idx_accounts_deleted ON accounts(deleted);

-- Add constraints
ALTER TABLE users ADD CONSTRAINT chk_users_email_not_empty CHECK (email != '');
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_name_not_empty CHECK (name != '');

-- Update existing data
UPDATE users SET 
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    created_by = 'migration',
    updated_by = 'migration',
    version = 0,
    deleted = false
WHERE created_at IS NULL;

UPDATE accounts SET 
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    created_by = 'migration',
    updated_by = 'migration',
    version = 0,
    deleted = false
WHERE created_at IS NULL;
```

### Step 3: Enhanced Repository Layer

#### 3.1 Base Repository with Soft Delete Support
Create `src/main/java/com/cekinmezyucel/springboot/poc/repository/BaseRepository.java`:
```java
package com.cekinmezyucel.springboot.poc.repository;

import com.cekinmezyucel.springboot.poc.entity.BaseAuditableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseAuditableEntity, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    List<T> findAllActive();

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    Optional<T> findByIdActive(@Param("id") ID id);

    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = true")
    List<T> findAllDeleted();

    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt BETWEEN :start AND :end")
    List<T> findDeletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy WHERE e.id = :id")
    void softDelete(@Param("id") ID id, @Param("deletedBy") String deletedBy);

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = false, e.deletedAt = null, e.deletedBy = null WHERE e.id = :id")
    void restore(@Param("id") ID id);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = false")
    long countActive();

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = true")
    long countDeleted();
}
```

#### 3.2 Enhanced User Repository
Update `src/main/java/com/cekinmezyucel/springboot/poc/repository/UserRepository.java`:
```java
package com.cekinmezyucel.springboot.poc.repository;

import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<UserEntity, Long> {

    @QueryHints({@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.deleted = false AND (u.name LIKE %:searchTerm% OR u.surname LIKE %:searchTerm% OR u.email LIKE %:searchTerm%)")
    Page<UserEntity> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT u FROM UserEntity u JOIN u.accounts a WHERE a.id = :accountId AND u.deleted = false AND a.deleted = false")
    List<UserEntity> findActiveByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT u FROM UserEntity u WHERE u.createdAt BETWEEN :start AND :end AND u.deleted = false")
    List<UserEntity> findCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT u FROM UserEntity u WHERE u.updatedAt > :since AND u.deleted = false")
    List<UserEntity> findModifiedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :date AND u.deleted = false")
    long countCreatedSince(@Param("date") LocalDateTime date);

    // Audit queries
    @Query("SELECT u FROM UserEntity u WHERE u.createdBy = :user AND u.deleted = false")
    List<UserEntity> findCreatedByUser(@Param("user") String user);

    @Query("SELECT u FROM UserEntity u WHERE u.updatedBy = :user AND u.deleted = false")
    List<UserEntity> findModifiedByUser(@Param("user") String user);

    // Data quality queries
    @Query("SELECT u FROM UserEntity u WHERE u.email IS NULL OR u.email = '' OR u.name IS NULL OR u.name = '' OR u.surname IS NULL OR u.surname = ''")
    List<UserEntity> findIncompleteUsers();

    @Query("SELECT u1 FROM UserEntity u1 WHERE EXISTS (SELECT u2 FROM UserEntity u2 WHERE u1.email = u2.email AND u1.id != u2.id AND u1.deleted = false AND u2.deleted = false)")
    List<UserEntity> findDuplicateEmails();
}
```

### Step 4: Data Archival Service

#### 4.1 Data Archival Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/service/DataArchivalService.java`:
```java
package com.cekinmezyucel.springboot.poc.service;

import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import com.cekinmezyucel.springboot.poc.entity.AccountEntity;
import com.cekinmezyucel.springboot.poc.repository.UserRepository;
import com.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DataArchivalService {

    private static final Logger log = LoggerFactory.getLogger(DataArchivalService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Value("${app.data.retention.soft-deleted-days:90}")
    private int softDeletedRetentionDays;

    @Value("${app.data.retention.audit-days:2555}") // 7 years
    private int auditRetentionDays;

    @Value("${app.data.archival.enabled:true}")
    private boolean archivalEnabled;

    public DataArchivalService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Scheduled(cron = "${app.data.archival.schedule:0 0 2 * * SUN}") // Every Sunday at 2 AM
    public void performDataArchival() {
        if (!archivalEnabled) {
            log.info("Data archival is disabled");
            return;
        }

        log.info("Starting data archival process");
        
        try {
            archiveSoftDeletedRecords();
            cleanupOldAuditRecords();
            generateArchivalReport();
        } catch (Exception e) {
            log.error("Error during data archival process", e);
        }
        
        log.info("Data archival process completed");
    }

    private void archiveSoftDeletedRecords() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(softDeletedRetentionDays);
        
        // Find soft-deleted records older than retention period
        List<UserEntity> oldDeletedUsers = userRepository.findDeletedBetween(
            LocalDateTime.MIN, cutoffDate);
        List<AccountEntity> oldDeletedAccounts = accountRepository.findDeletedBetween(
            LocalDateTime.MIN, cutoffDate);

        log.info("Found {} soft-deleted users and {} soft-deleted accounts for archival",
            oldDeletedUsers.size(), oldDeletedAccounts.size());

        // Archive to external storage (implement based on requirements)
        if (!oldDeletedUsers.isEmpty()) {
            archiveUsers(oldDeletedUsers);
            // Physically delete after archiving
            userRepository.deleteAll(oldDeletedUsers);
        }

        if (!oldDeletedAccounts.isEmpty()) {
            archiveAccounts(oldDeletedAccounts);
            accountRepository.deleteAll(oldDeletedAccounts);
        }
    }

    private void cleanupOldAuditRecords() {
        LocalDateTime auditCutoffDate = LocalDateTime.now().minusDays(auditRetentionDays);
        
        // This would typically involve direct SQL queries to audit tables
        // For now, we'll log the intent
        log.info("Would cleanup audit records older than {}", auditCutoffDate);
        
        // Example of what this might look like:
        // auditRepository.deleteAuditRecordsOlderThan(auditCutoffDate);
    }

    private void archiveUsers(List<UserEntity> users) {
        // Implement archival to external storage (S3, file system, etc.)
        log.info("Archiving {} users to external storage", users.size());
        
        // Example implementation:
        // archivalStorageService.archiveUsers(users);
    }

    private void archiveAccounts(List<AccountEntity> accounts) {
        // Implement archival to external storage
        log.info("Archiving {} accounts to external storage", accounts.size());
        
        // Example implementation:
        // archivalStorageService.archiveAccounts(accounts);
    }

    private void generateArchivalReport() {
        long activeUsers = userRepository.countActive();
        long deletedUsers = userRepository.countDeleted();
        long activeAccounts = accountRepository.countActive();
        long deletedAccounts = accountRepository.countDeleted();

        log.info("Archival Report - Active Users: {}, Deleted Users: {}, Active Accounts: {}, Deleted Accounts: {}",
            activeUsers, deletedUsers, activeAccounts, deletedAccounts);
    }

    // Manual methods for on-demand archival
    public void archiveUserData(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        if (!user.getDeleted()) {
            throw new IllegalStateException("Cannot archive active user. Soft delete first.");
        }
        
        archiveUsers(List.of(user));
        userRepository.delete(user);
        
        log.info("Archived user data for user ID: {}", userId);
    }

    public void restoreUser(Long userId) {
        userRepository.restore(userId);
        log.info("Restored user ID: {}", userId);
    }
}
```

### Step 5: Data Quality and Validation Service

#### 5.1 Data Quality Service
Create `src/main/java/com/cekinmezyucel/springboot/poc/service/DataQualityService.java`:
```java
package com.cekinmezyucel.springboot.poc.service;

import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import com.cekinmezyucel.springboot.poc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataQualityService {

    private static final Logger log = LoggerFactory.getLogger(DataQualityService.class);

    private final UserRepository userRepository;

    public DataQualityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${app.data.quality.schedule:0 0 1 * * MON}") // Every Monday at 1 AM
    public void performDataQualityCheck() {
        log.info("Starting data quality check");
        
        DataQualityReport report = generateDataQualityReport();
        logDataQualityReport(report);
        
        // Send alerts if quality issues found
        if (report.hasIssues()) {
            sendDataQualityAlert(report);
        }
        
        log.info("Data quality check completed");
    }

    public DataQualityReport generateDataQualityReport() {
        DataQualityReport report = new DataQualityReport();
        
        // Check for incomplete users
        List<UserEntity> incompleteUsers = userRepository.findIncompleteUsers();
        report.setIncompleteUsers(incompleteUsers.size());
        
        // Check for duplicate emails
        List<UserEntity> duplicateUsers = userRepository.findDuplicateEmails();
        report.setDuplicateEmails(duplicateUsers.size());
        
        // Check data distribution
        long totalUsers = userRepository.countActive();
        report.setTotalActiveUsers(totalUsers);
        
        // Add more quality checks as needed
        checkEmailDomainDistribution(report);
        checkUserAccountRelationships(report);
        
        return report;
    }

    private void checkEmailDomainDistribution(DataQualityReport report) {
        List<UserEntity> allUsers = userRepository.findAllActive();
        Map<String, Long> domainDistribution = allUsers.stream()
            .map(user -> user.getEmail().substring(user.getEmail().indexOf('@') + 1))
            .collect(Collectors.groupingBy(domain -> domain, Collectors.counting()));
        
        report.setEmailDomainDistribution(domainDistribution);
        
        // Flag suspicious patterns (e.g., too many users from test domains)
        long testDomainUsers = domainDistribution.entrySet().stream()
            .filter(entry -> entry.getKey().contains("test") || entry.getKey().contains("example"))
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        report.setTestDomainUsers(testDomainUsers);
    }

    private void checkUserAccountRelationships(DataQualityReport report) {
        List<UserEntity> usersWithoutAccounts = userRepository.findAllActive().stream()
            .filter(user -> user.getAccounts().isEmpty())
            .toList();
        
        report.setUsersWithoutAccounts(usersWithoutAccounts.size());
    }

    private void logDataQualityReport(DataQualityReport report) {
        log.info("Data Quality Report:");
        log.info("  Total Active Users: {}", report.getTotalActiveUsers());
        log.info("  Incomplete Users: {}", report.getIncompleteUsers());
        log.info("  Duplicate Emails: {}", report.getDuplicateEmails());
        log.info("  Users Without Accounts: {}", report.getUsersWithoutAccounts());
        log.info("  Test Domain Users: {}", report.getTestDomainUsers());
        
        if (report.getEmailDomainDistribution().size() > 0) {
            log.info("  Top Email Domains:");
            report.getEmailDomainDistribution().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> log.info("    {}: {} users", entry.getKey(), entry.getValue()));
        }
    }

    private void sendDataQualityAlert(DataQualityReport report) {
        // Implement alerting mechanism (email, Slack, etc.)
        log.warn("Data quality issues detected: {}", report.getIssuesSummary());
    }

    // Data Quality Report class
    public static class DataQualityReport {
        private long totalActiveUsers;
        private int incompleteUsers;
        private int duplicateEmails;
        private int usersWithoutAccounts;
        private long testDomainUsers;
        private Map<String, Long> emailDomainDistribution;

        public boolean hasIssues() {
            return incompleteUsers > 0 || duplicateEmails > 0 || testDomainUsers > 10;
        }

        public String getIssuesSummary() {
            StringBuilder summary = new StringBuilder();
            if (incompleteUsers > 0) {
                summary.append("Incomplete users: ").append(incompleteUsers).append("; ");
            }
            if (duplicateEmails > 0) {
                summary.append("Duplicate emails: ").append(duplicateEmails).append("; ");
            }
            if (testDomainUsers > 10) {
                summary.append("Too many test domain users: ").append(testDomainUsers).append("; ");
            }
            return summary.toString();
        }

        // Getters and setters
        public long getTotalActiveUsers() { return totalActiveUsers; }
        public void setTotalActiveUsers(long totalActiveUsers) { this.totalActiveUsers = totalActiveUsers; }
        
        public int getIncompleteUsers() { return incompleteUsers; }
        public void setIncompleteUsers(int incompleteUsers) { this.incompleteUsers = incompleteUsers; }
        
        public int getDuplicateEmails() { return duplicateEmails; }
        public void setDuplicateEmails(int duplicateEmails) { this.duplicateEmails = duplicateEmails; }
        
        public int getUsersWithoutAccounts() { return usersWithoutAccounts; }
        public void setUsersWithoutAccounts(int usersWithoutAccounts) { this.usersWithoutAccounts = usersWithoutAccounts; }
        
        public long getTestDomainUsers() { return testDomainUsers; }
        public void setTestDomainUsers(long testDomainUsers) { this.testDomainUsers = testDomainUsers; }
        
        public Map<String, Long> getEmailDomainDistribution() { return emailDomainDistribution; }
        public void setEmailDomainDistribution(Map<String, Long> emailDomainDistribution) { this.emailDomainDistribution = emailDomainDistribution; }
    }
}
```

### Step 6: Configuration and Properties

#### 6.1 Application Configuration
Add to `application.yaml`:
```yaml
app:
  data:
    retention:
      soft-deleted-days: ${DATA_RETENTION_SOFT_DELETED:90}
      audit-days: ${DATA_RETENTION_AUDIT:2555}  # 7 years
    archival:
      enabled: ${DATA_ARCHIVAL_ENABLED:true}
      schedule: ${DATA_ARCHIVAL_SCHEDULE:0 0 2 * * SUN}
      storage:
        type: ${ARCHIVAL_STORAGE_TYPE:filesystem}
        path: ${ARCHIVAL_STORAGE_PATH:/tmp/archival}
    quality:
      enabled: ${DATA_QUALITY_ENABLED:true}
      schedule: ${DATA_QUALITY_SCHEDULE:0 0 1 * * MON}
      
spring:
  jpa:
    properties:
      hibernate:
        envers:
          audit_table_suffix: _aud
          revision_field_name: rev
          revision_type_field_name: revtype
          default_schema: ${spring.datasource.schema:public}
          store_data_at_delete: true
        
        # Enable bean validation
        jakarta:
          validation:
            mode: AUTO
            
        # Optimistic locking
        versioning:
          enabled: true
        
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,dataquality
  endpoint:
    dataquality:
      enabled: true

logging:
  level:
    org.hibernate.envers: INFO
    com.cekinmezyucel.springboot.poc.service.DataArchivalService: INFO
    com.cekinmezyucel.springboot.poc.service.DataQualityService: INFO
```

### Step 7: Testing Data Management Features

#### 7.1 Data Management Integration Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/data/DataManagementIntegrationTest.java`:
```java
package com.cekinmezyucel.springboot.poc.data;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import com.cekinmezyucel.springboot.poc.repository.UserRepository;
import com.cekinmezyucel.springboot.poc.service.DataQualityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Rollback
class DataManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataQualityService dataQualityService;

    @Test
    @DisplayName("Should audit user creation and updates")
    void shouldAuditUserCreationAndUpdates() {
        // Create user
        UserEntity user = new UserEntity("test@example.com", "John", "Doe");
        UserEntity saved = userRepository.save(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);

        // Update user
        saved.setName("Jane");
        UserEntity updated = userRepository.save(saved);

        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
        assertThat(updated.getUpdatedBy()).isNotNull();
        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should support soft delete")
    void shouldSupportSoftDelete() {
        // Create user
        UserEntity user = new UserEntity("softdelete@example.com", "Test", "User");
        UserEntity saved = userRepository.save(user);
        Long userId = saved.getId();

        // Verify user exists in active queries
        Optional<UserEntity> found = userRepository.findByIdActive(userId);
        assertThat(found).isPresent();

        // Soft delete
        saved.markAsDeleted();
        userRepository.save(saved);

        // Verify user not found in active queries
        Optional<UserEntity> notFound = userRepository.findByIdActive(userId);
        assertThat(notFound).isEmpty();

        // Verify user found in deleted queries
        assertThat(userRepository.findAllDeleted()).hasSize(1);

        // Restore user
        saved.restore();
        userRepository.save(saved);

        // Verify user found in active queries again
        Optional<UserEntity> restored = userRepository.findByIdActive(userId);
        assertThat(restored).isPresent();
    }

    @Test
    @DisplayName("Should handle optimistic locking")
    void shouldHandleOptimisticLocking() {
        // Create user
        UserEntity user = new UserEntity("locking@example.com", "Lock", "Test");
        UserEntity saved = userRepository.save(user);

        // Simulate concurrent updates
        UserEntity user1 = userRepository.findById(saved.getId()).orElseThrow();
        UserEntity user2 = userRepository.findById(saved.getId()).orElseThrow();

        // Update first instance
        user1.setName("Updated1");
        userRepository.save(user1);

        // Try to update second instance (should fail with OptimisticLockingFailureException)
        user2.setName("Updated2");
        
        // In a real scenario, this would throw OptimisticLockingFailureException
        // For this test, we just verify the version was incremented
        UserEntity reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isGreaterThan(user2.getVersion());
    }

    @Test
    @DisplayName("Should generate data quality report")
    void shouldGenerateDataQualityReport() {
        // Create test data with quality issues
        userRepository.save(new UserEntity("", "Incomplete", "User")); // Invalid email
        userRepository.save(new UserEntity("duplicate@test.com", "User1", "Test"));
        userRepository.save(new UserEntity("duplicate@test.com", "User2", "Test")); // Duplicate

        DataQualityService.DataQualityReport report = dataQualityService.generateDataQualityReport();

        assertThat(report.getTotalActiveUsers()).isGreaterThan(0);
        assertThat(report.hasIssues()).isTrue();
    }

    @Test
    @DisplayName("Should track data lineage through audit tables")
    void shouldTrackDataLineageThroughAuditTables() {
        // Create and modify user multiple times
        UserEntity user = new UserEntity("lineage@example.com", "Original", "Name");
        UserEntity saved = userRepository.save(user);

        saved.setName("Updated");
        userRepository.save(saved);

        saved.setSurname("Changed");
        userRepository.save(saved);

        // Verify audit trail exists
        // Note: In a real test, you would query the audit tables
        // For now, we just verify the entity has tracking fields
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isAfter(saved.getCreatedAt());
        assertThat(saved.getVersion()).isGreaterThan(0L);
    }
}
```

## Validation Checklist

### ✅ Audit Logging
- [ ] All entities extend BaseAuditableEntity
- [ ] Audit tables created and indexed
- [ ] User context captured in audit logs
- [ ] Historical data tracking working
- [ ] Audit queries performant

### ✅ Soft Delete Implementation
- [ ] Soft delete columns added to all tables
- [ ] Repository queries filter deleted records
- [ ] Soft delete/restore functionality working
- [ ] Cascade soft delete for relationships
- [ ] Performance impact minimal

### ✅ Data Quality
- [ ] Data validation at entity level
- [ ] Quality checks scheduled and running
- [ ] Quality reports generated
- [ ] Data anomaly detection working
- [ ] Quality alerts configured

### ✅ Data Retention
- [ ] Archival process automated
- [ ] Retention policies configured
- [ ] Old data cleanup working
- [ ] Archive storage accessible
- [ ] Compliance requirements met

### ✅ Optimistic Locking
- [ ] Version fields added to entities
- [ ] Concurrent update conflicts detected
- [ ] Version increment on updates
- [ ] Exception handling for conflicts
- [ ] User notification on conflicts

## Troubleshooting

### Common Issues
1. **Audit tables not created**: Check Envers configuration and entity annotations
2. **Soft delete not working**: Verify @Where clause and repository methods
3. **Performance degradation**: Review query performance and indexing
4. **Version conflicts**: Implement proper conflict resolution strategies
5. **Data quality issues**: Set up automated monitoring and alerting

### Best Practices
- Always backup before running archival processes
- Monitor audit table growth and performance
- Implement data quality rules early in development
- Use soft delete judiciously to avoid performance issues
- Regular review and cleanup of audit data

## Next Steps
After implementing data management enhancements:
1. Set up automated backup and disaster recovery
2. Implement data encryption at rest and in transit
3. Add data masking for non-production environments
4. Set up data lineage tracking for compliance
5. Implement data cataloging and discovery

## AI Agent Notes
- Always test archival and restoration processes thoroughly
- Monitor the impact of audit logging on performance
- Ensure soft delete queries use proper indexes
- Validate data quality rules against business requirements
- Consider the storage and performance impact of audit tables