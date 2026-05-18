CREATE TABLE flyway_lab_item (
                                 id BIGSERIAL PRIMARY KEY,
                                 code VARCHAR(100) NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT now()
);