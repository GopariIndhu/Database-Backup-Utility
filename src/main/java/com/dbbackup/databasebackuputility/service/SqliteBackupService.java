package com.dbbackup.databasebackuputility.service;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SqliteBackupService {

    public Path backup(DatabaseConfig config) throws Exception {

        if (config.database() == null
                || config.database().isBlank()
                || ":memory:".equals(config.database())) {

            throw new IllegalArgumentException(
                    "SQLite backup requires a database file."
            );
        }

        Path sourceFile =
                Paths.get(config.database())
                        .toAbsolutePath()
                        .normalize();

        if (!Files.exists(sourceFile)) {
            throw new IllegalArgumentException(
                    "SQLite database file does not exist: "
                            + sourceFile
            );
        }

        Path backupDirectory =
                Paths.get("backups");

        Files.createDirectories(
                backupDirectory
        );

        String sourceName =
                sourceFile.getFileName()
                        .toString();

        int dotIndex =
                sourceName.lastIndexOf('.');

        String databaseName =
                dotIndex > 0
                        ? sourceName.substring(0, dotIndex)
                        : sourceName;

        databaseName =
                databaseName.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        String timestamp =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd_HH-mm-ss"
                                )
                        );

        Path backupFile =
                backupDirectory.resolve(
                        databaseName
                                + "_"
                                + timestamp
                                + "_sqlite.db"
                ).toAbsolutePath();

        String jdbcUrl =
                "jdbc:sqlite:" + sourceFile;

        String escapedBackupPath =
                backupFile.toString()
                        .replace("'", "''");

        try (
                Connection connection =
                        DriverManager.getConnection(jdbcUrl);

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    "VACUUM INTO '"
                            + escapedBackupPath
                            + "'"
            );
        }

        return backupFile;
    }
}
