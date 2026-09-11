package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MySqlBackupService {

    private static final String MYSQLDUMP_PATH =
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe";

    public Path backup(DatabaseConfig config) throws Exception {

        Path backupDirectory = Paths.get("backups");

        Files.createDirectories(backupDirectory);

        String safeDatabaseName = config.database()
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String fileName =
                safeDatabaseName + "_" + timestamp + ".sql";

        Path backupFile = backupDirectory.resolve(fileName);

        List<String> command = new ArrayList<>();

        command.add(MYSQLDUMP_PATH);

        command.add("--host=" + config.host());
        command.add("--port=" + config.port());
        command.add("--user=" + config.username());

        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");

        command.add("--set-gtid-purged=OFF");

//        command.add("--databases");
        command.add(config.database());

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        /*
         * Do not put the password directly in the command.
         * That could expose it in process listings.
         */
        if (config.password() != null) {
            processBuilder.environment().put(
                    "MYSQL_PWD",
                    new String(config.password())
            );
        }

        processBuilder.redirectOutput(backupFile.toFile());

        Process process = processBuilder.start();

        String errorOutput;

        try {
            errorOutput = new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();

            if (exitCode != 0) {

                Files.deleteIfExists(backupFile);

                throw new RuntimeException(
                        "mysqldump failed. " + errorOutput.trim()
                );
            }

        } finally {

            // Remove password from the ProcessBuilder environment
            processBuilder.environment().remove("MYSQL_PWD");
        }

        return backupFile.toAbsolutePath();
    }
}