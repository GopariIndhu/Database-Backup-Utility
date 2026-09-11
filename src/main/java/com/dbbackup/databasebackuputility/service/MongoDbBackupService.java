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

public class MongoDbBackupService {

    private static final String MONGODUMP_PATH =
            "C:\\Users\\indhu\\mongodb-database-tools-windows-x86_64-100.18.0\\bin\\mongodump.exe";

    public Path backup(
            DatabaseConfig config,
            boolean compress
    ) throws Exception {

        Path backupDirectory = Paths.get("backups");
        Files.createDirectories(backupDirectory);

        String safeDatabaseName = config.database()
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd_HH-mm-ss"
                ));

        String extension = compress
                ? "_mongodb.archive.gz"
                : "_mongodb.archive";

        Path backupFile = backupDirectory.resolve(
                safeDatabaseName
                        + "_"
                        + timestamp
                        + extension
        );

        List<String> command = new ArrayList<>();

        command.add(MONGODUMP_PATH);

        command.add("--host=" + config.host());
        command.add("--port=" + config.port());
        command.add("--db=" + config.database());

        command.add(
                "--archive="
                        + backupFile.toAbsolutePath()
        );

        if (compress) {
            command.add("--gzip");
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        Path logFile = Files.createTempFile(
                "mongodump-",
                ".log"
        );

        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFile.toFile());

        try {

            Process process =
                    processBuilder.start();

            int exitCode =
                    process.waitFor();

            String output =
                    Files.readString(
                            logFile,
                            StandardCharsets.UTF_8
                    );

            if (exitCode != 0) {

                Files.deleteIfExists(backupFile);

                throw new RuntimeException(
                        "mongodump failed. "
                                + output.trim()
                );
            }

            return backupFile.toAbsolutePath();

        } finally {

            Files.deleteIfExists(logFile);
        }
    }
}