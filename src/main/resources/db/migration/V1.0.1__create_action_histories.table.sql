CREATE TABLE action_histories (
    id           INTEGER PRIMARY KEY,
    action_title VARCHAR(200) NOT NULL,
    action_desc  VARCHAR(1000) NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);