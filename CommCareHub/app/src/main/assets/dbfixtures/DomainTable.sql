CREATE TABLE DomainList
(
id INTEGER PRIMARY KEY,
domain_guid,
pending_sync_request,
last_synced,
hidden,
UNIQUE(domain_guid)
);
