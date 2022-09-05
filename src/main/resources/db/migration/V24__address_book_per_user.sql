DROP TABLE blockchain_api_service.address_book;

CREATE TABLE blockchain_api_service.address_book (
    id             UUID                     PRIMARY KEY,
    alias          VARCHAR                  NOT NULL,
    wallet_address VARCHAR                  NOT NULL,
    phone_number   VARCHAR,
    email          VARCHAR,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id        UUID                     NOT NULL REFERENCES blockchain_api_service.user_identifier(id),
    CONSTRAINT address_book_per_user_unique_alias UNIQUE (user_id, alias)
);

CREATE INDEX address_book_user_id ON blockchain_api_service.address_book(user_id);
