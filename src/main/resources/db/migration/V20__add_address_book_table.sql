CREATE TABLE blockchain_api_service.address_book (
    id             UUID                     PRIMARY KEY,
    alias          VARCHAR                  NOT NULL,
    wallet_address VARCHAR                  NOT NULL,
    phone_number   VARCHAR,
    email          VARCHAR,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id     UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    CONSTRAINT address_book_per_project_unique_alias UNIQUE (project_id, alias)
);

CREATE INDEX address_book_project_id ON blockchain_api_service.address_book(project_id);
