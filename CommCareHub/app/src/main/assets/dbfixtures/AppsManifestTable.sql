CREATE TABLE AppManifest
(
_id INTEGER PRIMARY KEY,
app_guid,
domain_id,
app_descriptor,
version,
status,
download_url,
UNIQUE(app_guid)
);
