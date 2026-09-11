package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PostgreSqlBackupService {

    private static final String PG_DUMP_PATH =
            "C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe";

    public Path backup(DatabaseConfig config) throws Exception {

        Path backupDirectory = Paths.get("backups");
        Files.createDirectories(backupDirectory);

        String safeDatabaseName = config.database()
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        String timestamp = LocalDateTime.now()
                .format(
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd_HH-mm-ss"
                        )
                );

        String fileName =
                safeDatabaseName
                        + "_"
                        + timestamp
                        + "_postgresql.sql";

        Path backupFile =
                backupDirectory.resolve(fileName);

        List<String> command =
                new ArrayList<>();

        command.add(PG_DUMP_PATH);

        command.add("--host=" + config.host());
        command.add("--port=" + config.port());
        command.add("--username=" + config.username());

        /*
         * Plain SQL format.
         * This makes later restore with psql simple.
         */
        command.add("--format=plain");

        command.add("--no-password");

        command.add(config.database());

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        /*
         * Don't put PostgreSQL password in command arguments.
         */
        if (config.password() != null) {
            processBuilder.environment().put(
                    "PGPASSWORD",
                    new String(config.password())
            );
        }

        processBuilder.redirectOutput(
                backupFile.toFile()
        );

        Process process =
                processBuilder.start();

        String errorOutput;

        try {

            errorOutput =
                    new String(
                            process
                                    .getErrorStream()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            int exitCode =
                    process.waitFor();

            if (exitCode != 0) {

                Files.deleteIfExists(
                        backupFile
                );

                throw new RuntimeException(
                        "pg_dump failed. "
                                + errorOutput.trim()
                );
            }

        } finally {

            processBuilder
                    .environment()
                    .remove("PGPASSWORD");
        }

        return backupFile.toAbsolutePath();
    }
}