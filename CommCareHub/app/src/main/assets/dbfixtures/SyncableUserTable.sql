CREATE TABLE SyncableUser
(
id INTEGER PRIMARY KEY,
username,
domain,
status,
last_updated,
password,
key_record_location,
sync_file_location
);
