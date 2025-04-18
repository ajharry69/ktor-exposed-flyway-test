ALTER TABLE users ADD dob DATE DEFAULT '2025-04-02'::date NOT NULL;
CREATE INDEX users_dob ON users (dob);
