package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class PostgreSqlRestoreService {

    private static final String PSQL_PATH =
            "C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe";

    public void restore(
            DatabaseConfig config,
            Path backupFile
    ) throws Exception {

        if (!Files.exists(backupFile)) {
            throw new IllegalArgumentException(
                    "Backup file does not exist: " + backupFile
            );
        }

        createDatabaseIfNeeded(config);

        List<String> command = new ArrayList<>();

        command.add(PSQL_PATH);
        command.add("--host=" + config.host());
        command.add("--port=" + config.port());
        command.add("--username=" + config.username());
        command.add("--dbname=" + config.database());

        command.add("--no-password");

        // Stop immediately if any SQL statement fails
        command.add("--set=ON_ERROR_STOP=1");

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        if (config.password() != null) {

            processBuilder.environment().put(
                    "PGPASSWORD",
                    new String(config.password())
            );
        }

        Path errorFile =
                Files.createTempFile(
                        "postgres-restore-",
                        ".log"
                );

        processBuilder.redirectError(
                errorFile.toFile()
        );

        Process process =
                processBuilder.start();

        try (
                InputStream backupInput =
                        openBackupFile(backupFile);

                OutputStream psqlInput =
                        process.getOutputStream()
        ) {

            backupInput.transferTo(psqlInput);
        }

        int exitCode =
                process.waitFor();

        try {

            if (exitCode != 0) {

                String error =
                        Files.readString(
                                errorFile,
                                StandardCharsets.UTF_8
                        );

                throw new RuntimeException(
                        "PostgreSQL restore failed. "
                                + error.trim()
                );
            }

        } finally {

            Files.deleteIfExists(
                    errorFile
            );

            processBuilder
                    .environment()
                    .remove("PGPASSWORD");
        }
    }

    private InputStream openBackupFile(
            Path backupFile
    ) throws Exception {

        InputStream input =
                Files.newInputStream(backupFile);

        if (backupFile
                .getFileName()
                .toString()
                .endsWith(".gz")) {

            return new GZIPInputStream(input);
        }

        return input;
    }

    private void createDatabaseIfNeeded(
            DatabaseConfig config
    ) throws Exception {

        String databaseName =
                config.database();

        if (!databaseName.matches(
                "[a-zA-Z0-9_]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid PostgreSQL database name."
            );
        }

        String url =
                "jdbc:postgresql://"
                        + config.host()
                        + ":"
                        + config.port()
                        + "/postgres";

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
                        )
        ) {

            boolean exists;

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    """
                                    SELECT 1
                                    FROM pg_database
                                    WHERE datname = ?
                                    """
                            )
            ) {

                statement.setString(
                        1,
                        databaseName
                );

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    exists =
                            resultSet.next();
                }
            }

            if (!exists) {

                try (
                        Statement statement =
                                connection.createStatement()
                ) {

                    statement.executeUpdate(
                            "CREATE DATABASE \""
                                    + databaseName
                                    + "\""
                    );
                }
            }
        }
    }
}