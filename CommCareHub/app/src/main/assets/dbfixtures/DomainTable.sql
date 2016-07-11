CREATE TABLE DomainList
(
id INTEGER PRIMARY KEY,
domain_guid,
status,
last_synced,
hidden,
UNIQUE(domain_guid)
);
