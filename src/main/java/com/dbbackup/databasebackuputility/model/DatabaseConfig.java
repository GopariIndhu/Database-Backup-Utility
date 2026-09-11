package com.dbbackup.databasebackuputility.model;

public record DatabaseConfig(
        DatabaseType type,
        String host,
        int port,
        String username,
        char[] password,
        String database
) {
}