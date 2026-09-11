package com.dbbackup.databasebackuputility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqliteTestDataSetup {

    public static void main(String[] args) throws Exception {

        String url = "jdbc:sqlite:sqlite_test.db";

        try (
                Connection connection =
                        DriverManager.getConnection(url);

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS employees (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        email TEXT,
                        department TEXT
                    )
                    """);

            // Keep the test repeatable
            statement.executeUpdate(
                    "DELETE FROM employees"
            );

            statement.executeUpdate("""
                    INSERT INTO employees
                    (name, email, department)
                    VALUES
                    ('Alice', 'alice@example.com', 'Engineering'),
                    ('Bob', 'bob@example.com', 'HR')
                    """);

            try (
                    ResultSet resultSet =
                            statement.executeQuery(
                                    "SELECT * FROM employees"
                            )
            ) {

                System.out.println(
                        "SQLite test data:"
                );

                while (resultSet.next()) {

                    System.out.println(
                            resultSet.getInt("id")
                                    + " | "
                                    + resultSet.getString("name")
                                    + " | "
                                    + resultSet.getString("email")
                                    + " | "
                                    + resultSet.getString("department")
                    );
                }
            }

            try (
                    ResultSet resultSet =
                            statement.executeQuery(
                                    "SELECT COUNT(*) FROM employees"
                            )
            ) {

                if (resultSet.next()) {

                    System.out.println(
                            "Row count: "
                                    + resultSet.getInt(1)
                    );
                }
            }
        }

        System.out.println(
                "SQLite database created successfully."
        );
    }
}