package com.dbbackup.databasebackuputility.database;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqliteAdapter implements DatabaseAdapter {

    @Override
    public void testConnection(DatabaseConfig config) throws Exception {

        String url = "jdbc:sqlite:" + config.database();

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet resultSet =
                     statement.executeQuery("SELECT sqlite_version()")) {

            if (!resultSet.next()) {
                throw new RuntimeException(
                        "Could not verify SQLite connection."
                );
            }

            System.out.println(
                    "SQLite version: " + resultSet.getString(1)
            );
        }
    }
}