package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MongoDbRestoreService {

    private static final String MONGORESTORE_PATH =
            "C:\\Users\\indhu\\mongodb-database-tools-windows-x86_64-100.18.0\\bin\\mongorestore.exe";

    public void restore(
            DatabaseConfig config,
            Path backupFile,
            String sourceDatabase
    ) throws Exception {

        if (!Files.exists(backupFile)) {
            throw new IllegalArgumentException(
                    "Backup file does not exist: " + backupFile
            );
        }

        List<String> command = new ArrayList<>();

        command.add(MONGORESTORE_PATH);

        command.add("--host=" + config.host());
        command.add("--port=" + config.port());

        command.add(
                "--archive=" + backupFile.toAbsolutePath()
        );

        if (backupFile.getFileName()
                .toString()
                .endsWith(".gz")) {

            command.add("--gzip");
        }

        command.add(
                "--nsFrom=" + sourceDatabase + ".*"
        );

        command.add(
                "--nsTo=" + config.database() + ".*"
        );

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        Path logFile = Files.createTempFile(
                "mongorestore-",
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
                throw new RuntimeException(
                        "mongorestore failed. "
                                + output.trim()
                );
            }

        } finally {

            Files.deleteIfExists(logFile);
        }
    }
}