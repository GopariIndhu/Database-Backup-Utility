package com.dbbackup.databasebackuputility.database;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class PostgreSqlAdapter implements DatabaseAdapter {

    @Override
    public void testConnection(DatabaseConfig config) throws Exception {

        String url = "jdbc:postgresql://"
                + config.host()
                + ":"
                + config.port()
                + "/"
                + config.database();

        String password = config.password() == null
                ? ""
                : new String(config.password());

        try (Connection connection = DriverManager.getConnection(
                url,
                config.username(),
                password
        )) {

            if (!connection.isValid(5)) {
                throw new RuntimeException(
                        "PostgreSQL connection is not valid."
                );
            }
        }
    }
}