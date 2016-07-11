CREATE TABLE MobileUser
(
_id INTEGER PRIMARY KEY,
domain_id,
user_id,
username,
readable_name,
UNIQUE(username)
);
