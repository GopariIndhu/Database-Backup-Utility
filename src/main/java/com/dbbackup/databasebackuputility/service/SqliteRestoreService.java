package com.dbbackup.databasebackuputility.service;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class SqliteRestoreService {

    public void restore(
            Path backupFile,
            String targetDatabase
    ) throws Exception {

        if (!Files.exists(backupFile)) {
            throw new IllegalArgumentException(
                    "Backup file does not exist: "
                            + backupFile
            );
        }

        if (targetDatabase == null
                || targetDatabase.isBlank()
                || ":memory:".equals(targetDatabase)) {

            throw new IllegalArgumentException(
                    "SQLite restore requires a target database file."
            );
        }

        Path source =
                backupFile.toAbsolutePath()
                        .normalize();

        Path target =
                Paths.get(targetDatabase)
                        .toAbsolutePath()
                        .normalize();

        if (source.equals(target)) {
            throw new IllegalArgumentException(
                    "Backup file and target database cannot be the same file."
            );
        }

        Path parent = target.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile =
                Files.createTempFile(
                        parent,
                        "sqlite-restore-",
                        ".tmp"
                );

        try {

            if (source.getFileName()
                    .toString()
                    .endsWith(".gz")) {

                try (
                        InputStream fileInput =
                                Files.newInputStream(source);

                        GZIPInputStream gzipInput =
                                new GZIPInputStream(fileInput);

                        OutputStream output =
                                Files.newOutputStream(tempFile)
                ) {

                    gzipInput.transferTo(output);
                }

            } else {

                Files.copy(
                        source,
                        tempFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            try {

                Files.move(
                        tempFile,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException e) {

                Files.move(
                        tempFile,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } finally {

            Files.deleteIfExists(tempFile);
        }
    }
}