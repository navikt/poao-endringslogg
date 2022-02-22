CREATE TABLE seen_forced (
                             user_id VARCHAR(255) NOT NULL,
                             document_id UUID NOT NULL,
                             PRIMARY KEY (user_id, document_id));