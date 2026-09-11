package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class MySqlRestoreService {

    private static final String MYSQL_PATH =
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe";

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

        command.add(MYSQL_PATH);
        command.add("--host=" + config.host());
        command.add("--port=" + config.port());
        command.add("--user=" + config.username());
        command.add(config.database());

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        if (config.password() != null) {
            processBuilder.environment().put(
                    "MYSQL_PWD",
                    new String(config.password())
            );
        }

        Path errorFile =
                Files.createTempFile("mysql-restore-", ".log");

        processBuilder.redirectError(errorFile.toFile());

        Process process = processBuilder.start();

        try (
                InputStream input =
                        openBackupFile(backupFile);

                OutputStream mysqlInput =
                        process.getOutputStream()
        ) {

            input.transferTo(mysqlInput);
        }

        int exitCode = process.waitFor();

        try {

            if (exitCode != 0) {

                String error =
                        Files.readString(
                                errorFile,
                                StandardCharsets.UTF_8
                        );

                throw new RuntimeException(
                        "MySQL restore failed. "
                                + error.trim()
                );
            }

        } finally {

            Files.deleteIfExists(errorFile);

            processBuilder
                    .environment()
                    .remove("MYSQL_PWD");
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

        String databaseName = config.database();

        if (!databaseName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Invalid database name."
            );
        }

        String url =
                "jdbc:mysql://"
                        + config.host()
                        + ":"
                        + config.port()
                        + "/";

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

            statement.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `"
                            + databaseName
                            + "`"
            );
        }
    }
}