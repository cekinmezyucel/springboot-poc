CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    industry VARCHAR(255)
);

CREATE TABLE user_accounts (
    user_id BIGINT NOT NULL REFERENCES users(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    PRIMARY KEY (user_id, account_id)
);
