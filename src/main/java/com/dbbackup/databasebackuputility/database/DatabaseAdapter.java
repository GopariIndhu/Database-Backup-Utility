package com.dbbackup.databasebackuputility.database;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

public interface DatabaseAdapter {

    void testConnection(DatabaseConfig config) throws Exception;
}