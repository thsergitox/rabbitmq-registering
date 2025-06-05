-- Fix the friend table foreign key references
-- The original db.sql references 'usuario' table but the actual table is 'users'

-- Drop the friend table if it exists with wrong references
DROP TABLE IF EXISTS friend CASCADE;

-- Recreate friend table with correct references to users table
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