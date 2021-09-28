CREATE TABLE seen (
  user_id VARCHAR(255) NOT NULL,
  app_id VARCHAR(255) NOT NULL,
  document_id UUID NOT NULL,
  opened_link boolean DEFAULT FALSE,
  opened_modal boolean DEFAULT FALSE,
  time_stamp TIMESTAMP NOT NULL,
  PRIMARY KEY (user_id, document_id));

CREATE TABLE user_session (
  user_id VARCHAR(255) NOT NULL,
  app_id VARCHAR(255) NOT NULL,
  duration INT NOT NULL,
  unseen_fields INT NOT NULL,
  time_stamp TIMESTAMP NOT NULL,
  PRIMARY KEY (user_id, time_stamp));
