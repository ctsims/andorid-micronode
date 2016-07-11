CREATE TABLE AppAsset
(
id INTEGER PRIMARY KEY,
app_manifest_id,
version,
ccz_path,
sandbox_path,
status,
auth_uri,
source_uri,
paused
);
