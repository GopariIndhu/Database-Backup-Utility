package com.dbbackup.databasebackuputility.command;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;
import com.dbbackup.databasebackuputility.model.DatabaseType;
import com.dbbackup.databasebackuputility.service.BackupVerificationService;
import com.dbbackup.databasebackuputility.service.CompressionService;
import com.dbbackup.databasebackuputility.service.MongoDbBackupService;
import com.dbbackup.databasebackuputility.service.MySqlBackupService;
import com.dbbackup.databasebackuputility.service.PostgreSqlBackupService;
import com.dbbackup.databasebackuputility.service.SqliteBackupService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(
        name = "backup",
        description = "Create a database backup",
        mixinStandardHelpOptions = true
)
public class BackupCommand implements Callable<Integer> {

    @Option(
            names = "--db",
            required = true,
            description = "Database type"
    )
    private DatabaseType databaseType;

    @Option(
            names = "--host",
            defaultValue = "localhost",
            description = "Database host"
    )
    private String host;

    @Option(
            names = "--port",
            description = "Database port"
    )
    private Integer port;

    @Option(
            names = "--username",
            description = "Database username"
    )
    private String username;

    @Option(
            names = "--password",
            interactive = true,
            arity = "0..1",
            description = "Database password"
    )
    private char[] password;

    @Option(
            names = "--database",
            required = true,
            description = "Database name or SQLite database file"
    )
    private String database;

    @Option(
            names = "--compress",
            description = "Compress the backup using GZIP"
    )
    private boolean compress;

    @Option(
            names = "--verify",
            description = "Restore the backup to a temporary database and verify it"
    )
    private boolean verify;

    @Override
    public Integer call() {

        int resolvedPort = resolvePort();

        /*
         * MySQL and PostgreSQL require a username
         * in the current implementation.
         *
         * MongoDB and SQLite do not.
         */
        if ((databaseType == DatabaseType.MYSQL
                || databaseType == DatabaseType.POSTGRESQL)
                && (username == null || username.isBlank())) {

            System.err.println(
                    "--username is required for " + databaseType
            );

            return 2;
        }

        DatabaseConfig config = new DatabaseConfig(
                databaseType,
                host,
                resolvedPort,
                username,
                password,
                database
        );

        long startTime = System.currentTimeMillis();

        try {

            System.out.println(
                    "Database type: " + databaseType
            );

            System.out.println(
                    "Database: " + database
            );

            /*
             * Host and port are not meaningful
             * for a local SQLite database file.
             */
            if (databaseType != DatabaseType.SQLITE) {

                System.out.println(
                        "Host: " + host
                );

                System.out.println(
                        "Port: " + resolvedPort
                );
            }

            System.out.println();

            Path backupFile;

            /*
             * Select the correct backup implementation.
             */
            switch (databaseType) {

                case MYSQL -> {

                    System.out.println(
                            "Starting MySQL backup..."
                    );

                    MySqlBackupService backupService =
                            new MySqlBackupService();

                    backupFile =
                            backupService.backup(config);
                }

                case POSTGRESQL -> {

                    System.out.println(
                            "Starting PostgreSQL backup..."
                    );

                    PostgreSqlBackupService backupService =
                            new PostgreSqlBackupService();

                    backupFile =
                            backupService.backup(config);
                }

                case MONGODB -> {

                    System.out.println(
                            "Starting MongoDB backup..."
                    );

                    MongoDbBackupService backupService =
                            new MongoDbBackupService();

                    /*
                     * MongoDB handles its own gzip compression
                     * through mongodump --gzip.
                     */
                    backupFile =
                            backupService.backup(
                                    config,
                                    compress
                            );
                }

                case SQLITE -> {

                    System.out.println(
                            "Starting SQLite backup..."
                    );

                    SqliteBackupService backupService =
                            new SqliteBackupService();

                    backupFile =
                            backupService.backup(config);
                }

                default -> throw new IllegalStateException(
                        "Unsupported database type: "
                                + databaseType
                );
            }

            /*
             * MySQL, PostgreSQL and SQLite use our
             * Java CompressionService.
             *
             * MongoDB is excluded because mongodump
             * already performs gzip compression.
             */
            if (compress
                    && databaseType != DatabaseType.MONGODB) {

                System.out.println(
                        "Compressing backup..."
                );

                CompressionService compressionService =
                        new CompressionService();

                Path compressedFile =
                        compressionService.compress(
                                backupFile
                        );

                /*
                 * Delete the uncompressed backup only
                 * after compression succeeds.
                 */
                Files.deleteIfExists(
                        backupFile
                );

                backupFile =
                        compressedFile;
            }

            System.out.println();

            System.out.println(
                    "Backup completed successfully."
            );

            System.out.println(
                    "Backup file: "
                            + backupFile.toAbsolutePath()
            );

            /*
             * Optional automatic verification.
             */
            if (verify) {

                System.out.println();

                System.out.println(
                        "Verifying backup..."
                );

                BackupVerificationService verificationService =
                        new BackupVerificationService();

                verificationService.verify(
                        config,
                        backupFile
                );

                System.out.println(
                        "Backup verification successful."
                );
            }

            long elapsed =
                    System.currentTimeMillis()
                            - startTime;

            System.out.println();

            System.out.println(
                    "Time taken: "
                            + elapsed
                            + " ms"
            );

            return 0;

        } catch (Exception e) {

            System.err.println();

            System.err.println(
                    "Backup or verification failed."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            return 1;

        } finally {

            /*
             * Clear the password from memory when possible.
             */
            if (password != null) {

                Arrays.fill(
                        password,
                        '\0'
                );
            }
        }
    }

    private int resolvePort() {

        if (port != null) {
            return port;
        }

        return switch (databaseType) {

            case MYSQL -> 3306;

            case POSTGRESQL -> 5432;

            case MONGODB -> 27017;

            case SQLITE -> 0;
        };
    }
}