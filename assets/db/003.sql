ALTER TABLE users ADD COLUMN password TEXT DEFAULT '';

ALTER TABLE users ADD COLUMN access_token TEXT DEFAULT '';

ALTER TABLE users ADD COLUMN keep_logged_in BOOLEAN DEFAULT FALSE;

CREATE TABLE settings (id INTEGER PRIMARY KEY AUTOINCREMENT, 
			key INTEGER NOT NULL, 
			value TEXT NOT NULL);
