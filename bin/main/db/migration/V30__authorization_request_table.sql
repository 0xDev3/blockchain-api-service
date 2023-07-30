CREATE TABLE blockchain_api_service.authorization_request (
    id                           UUID                     PRIMARY KEY,
    project_id                   UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    redirect_url                 VARCHAR                  NOT NULL,
    requested_wallet_address     VARCHAR,
    actual_wallet_address        VARCHAR,
    signed_message               VARCHAR,
    arbitrary_data               JSON,
    screen_before_action_message VARCHAR,
    screen_after_action_message  VARCHAR,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX authorization_request_project_id ON blockchain_api_service.authorization_request(project_id);
CREATE INDEX authorization_request_created_at ON blockchain_api_service.authorization_request(created_at);
