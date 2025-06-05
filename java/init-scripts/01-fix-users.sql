-- Add PRIMARY KEY constraint to users table
ALTER TABLE users ADD PRIMARY KEY (id);

-- Now create the friend table with correct references
CREATE TABLE IF NOT EXISTS friend (
  user_id   INT  NOT NULL,
  friend_id INT  NOT NULL,
  PRIMARY KEY (user_id, friend_id),
  FOREIGN KEY (user_id)   REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_friend_user_id ON friend(user_id);
CREATE INDEX IF NOT EXISTS idx_friend_friend_id ON friend(friend_id);
CREATE INDEX IF NOT EXISTS idx_users_dni ON users(dni); 