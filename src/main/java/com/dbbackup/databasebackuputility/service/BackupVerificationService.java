package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class BackupVerificationService {

    public void verify(
            DatabaseConfig config,
            Path backupFile
    ) throws Exception {

        switch (config.type()) {

            case MYSQL ->
                    verifyMySql(
                            config,
                            backupFile
                    );

            case POSTGRESQL ->
                    verifyPostgreSql(
                            config,
                            backupFile
                    );

            case MONGODB ->
                    verifyMongoDb(
                            config,
                            backupFile
                    );

            case SQLITE ->
                    verifySqlite(
                            config,
                            backupFile
                    );
        }
    }

    private void verifyMySql(
            DatabaseConfig config,
            Path backupFile
    ) throws Exception {

        String verificationDatabase =
                config.database()
                        + "_backup_verify";

        DatabaseConfig verifyConfig =
                new DatabaseConfig(
                        config.type(),
                        config.host(),
                        config.port(),
                        config.username(),
                        config.password(),
                        verificationDatabase
                );

        System.out.println(
                "Restoring MySQL backup for verification..."
        );

        MySqlRestoreService restoreService =
                new MySqlRestoreService();

        restoreService.restore(
                verifyConfig,
                backupFile
        );

        String url =
                "jdbc:mysql://"
                        + config.host()
                        + ":"
                        + config.port()
                        + "/"
                        + verificationDatabase;

        String password =
                config.password() == null
                        ? ""
                        : new String(config.password());

        try (
                Connection connection =
                        DriverManager.getConnection(
                                url,
                                config.username(),
                                password
                        );

                Statement statement =
                        connection.createStatement()
        ) {

            verifyEmployeesTable(
                    statement,
                    "MySQL"
            );
        }

        System.out.println(
                "MySQL backup verification successful."
        );
    }

    private void verifyPostgreSql(
            DatabaseConfig config,
            Path backupFile
    ) throws Exception {

        String verificationDatabase =
                config.database()
                        + "_backup_verify";

        DatabaseConfig verifyConfig =
                new DatabaseConfig(
                        config.type(),
                        config.host(),
                        config.port(),
                        config.username(),
                        config.password(),
                        verificationDatabase
                );

        System.out.println(
                "Restoring PostgreSQL backup for verification..."
        );

        PostgreSqlRestoreService restoreService =
                new PostgreSqlRestoreService();

        restoreService.restore(
                verifyConfig,
                backupFile
        );

        String url =
                "jdbc:postgresql://"
                        + config.host()
                        + ":"
                        + config.port()
                        + "/"
                        + verificationDatabase;

        String password =
                config.password() == null
                        ? ""
                        : new String(config.password());

        try (
                Connection connection =
                        DriverManager.getConnection(
                                url,
                                config.username(),
                                password
                        );

                Statement statement =
                        connection.createStatement()
        ) {

            verifyEmployeesTable(
                    statement,
                    "PostgreSQL"
            );
        }

        System.out.println(
                "PostgreSQL backup verification successful."
        );
    }

    private void verifyMongoDb(
            DatabaseConfig config,
            Path backupFile
    ) throws Exception {

        String verificationDatabase =
                config.database()
                        + "_backup_verify";

        DatabaseConfig verifyConfig =
                new DatabaseConfig(
                        config.type(),
                        config.host(),
                        config.port(),
                        config.username(),
                        config.password(),
                        verificationDatabase
                );

        System.out.println(
                "Restoring MongoDB backup for verification..."
        );

        MongoDbRestoreService restoreService =
                new MongoDbRestoreService();

        restoreService.restore(
                verifyConfig,
                backupFile,
                config.database()
        );

        String connectionString =
                "mongodb://"
                        + config.host()
                        + ":"
                        + config.port();

        try (
                MongoClient mongoClient =
                        MongoClients.create(
                                connectionString
                        )
        ) {

            MongoDatabase database =
                    mongoClient.getDatabase(
                            verificationDatabase
                    );

            boolean employeesFound = false;

            for (String collectionName :
                    database.listCollectionNames()) {

                if ("employees".equals(
                        collectionName
                )) {

                    employeesFound = true;
                    break;
                }
            }

            if (!employeesFound) {

                throw new RuntimeException(
                        "Backup verification failed: "
                                + "employees collection not found."
                );
            }

            System.out.println(
                    "employees collection found."
            );

            long count =
                    database
                            .getCollection("employees")
                            .countDocuments();

            System.out.println(
                    "Restored document count: "
                            + count
            );

            if (count <= 0) {

                throw new RuntimeException(
                        "Backup verification failed: "
                                + "employees collection contains no documents."
                );
            }
        }

        System.out.println(
                "MongoDB backup verification successful."
        );
    }

    private void verifySqlite(
            DatabaseConfig config,
            Path backupFile
    ) throws Exception {

        String verificationDatabase =
                "sqlite_backup_verify.db";

        SqliteRestoreService restoreService =
                new SqliteRestoreService();

        System.out.println(
                "Restoring SQLite backup for verification..."
        );

        restoreService.restore(
                backupFile,
                verificationDatabase
        );

        String url =
                "jdbc:sqlite:"
                        + verificationDatabase;

        try (
                Connection connection =
                        DriverManager.getConnection(url);

                Statement statement =
                        connection.createStatement()
        ) {

            try (
                    ResultSet resultSet =
                            statement.executeQuery(
                                    "PRAGMA integrity_check"
                            )
            ) {

                if (!resultSet.next()) {

                    throw new RuntimeException(
                            "Unable to run SQLite integrity check."
                    );
                }

                String result =
                        resultSet.getString(1);

                if (!"ok".equalsIgnoreCase(result)) {

                    throw new RuntimeException(
                            "SQLite integrity check failed: "
                                    + result
                    );
                }

                System.out.println(
                        "SQLite integrity check: OK"
                );
            }

            verifyEmployeesTable(
                    statement,
                    "SQLite"
            );
        }

        System.out.println(
                "SQLite backup verification successful."
        );
    }

    private void verifyEmployeesTable(
            Statement statement,
            String databaseType
    ) throws Exception {

        try (
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT COUNT(*) FROM employees"
                        )
        ) {

            if (!resultSet.next()) {

                throw new RuntimeException(
                        databaseType
                                + " backup verification failed: "
                                + "unable to read employees table."
                );
            }

            int count =
                    resultSet.getInt(1);

            System.out.println(
                    "employees table found."
            );

            System.out.println(
                    "Restored row count: "
                            + count
            );

            if (count <= 0) {

                throw new RuntimeException(
                        databaseType
                                + " backup verification failed: "
                                + "employees table contains no records."
                );
            }
        }
    }
}