package com.dbbackup.databasebackuputility.command;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;
import com.dbbackup.databasebackuputility.model.DatabaseType;
import com.dbbackup.databasebackuputility.service.MongoDbRestoreService;
import com.dbbackup.databasebackuputility.service.MySqlRestoreService;
import com.dbbackup.databasebackuputility.service.PostgreSqlRestoreService;
import com.dbbackup.databasebackuputility.service.SqliteRestoreService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(
        name = "restore",
        description = "Restore a database backup",
        mixinStandardHelpOptions = true
)
public class RestoreCommand implements Callable<Integer> {

    @Option(
            names = "--db",
            required = true
    )
    private DatabaseType databaseType;

    @Option(
            names = "--host",
            defaultValue = "localhost"
    )
    private String host;

    @Option(
            names = "--port",
            description = "Database port"
    )
    private Integer port;

    @Option(
            names = "--username"
    )
    private String username;

    @Option(
            names = "--password",
            interactive = true,
            arity = "0..1"
    )
    private char[] password;

    @Option(
            names = "--database",
            required = true,
            description = "Target database"
    )
    private String database;

    @Option(
            names = "--file",
            required = true,
            description = "Backup file"
    )
    private Path backupFile;

    @Option(
            names = "--source-database",
            description = "Original MongoDB database name stored in backup"
    )
    private String sourceDatabase;

    @Override
    public Integer call() {

        int resolvedPort = resolvePort();

        if ((databaseType == DatabaseType.MYSQL
                || databaseType == DatabaseType.POSTGRESQL)
                && (username == null || username.isBlank())) {

            System.err.println(
                    "--username is required for " + databaseType
            );

            return 2;
        }

        DatabaseConfig config =
                new DatabaseConfig(
                        databaseType,
                        host,
                        resolvedPort,
                        username,
                        password,
                        database
                );

        long start =
                System.currentTimeMillis();

        try {

            System.out.println(
                    "Database type: " + databaseType
            );

            System.out.println(
                    "Target database: " + database
            );

            if (databaseType != DatabaseType.SQLITE) {

                System.out.println(
                        "Host: " + host
                );

                System.out.println(
                        "Port: " + resolvedPort
                );
            }

            System.out.println(
                    "Backup file: "
                            + backupFile.toAbsolutePath()
            );

            System.out.println();

            switch (databaseType) {

                case MYSQL -> {

                    System.out.println(
                            "Starting MySQL restore..."
                    );

                    MySqlRestoreService service =
                            new MySqlRestoreService();

                    service.restore(
                            config,
                            backupFile
                    );
                }

                case POSTGRESQL -> {

                    System.out.println(
                            "Starting PostgreSQL restore..."
                    );

                    PostgreSqlRestoreService service =
                            new PostgreSqlRestoreService();

                    service.restore(
                            config,
                            backupFile
                    );
                }

                case MONGODB -> {

                    if (sourceDatabase == null
                            || sourceDatabase.isBlank()) {

                        System.err.println(
                                "--source-database is required "
                                        + "for MongoDB restore."
                        );

                        return 2;
                    }

                    System.out.println(
                            "Starting MongoDB restore..."
                    );

                    MongoDbRestoreService service =
                            new MongoDbRestoreService();

                    service.restore(
                            config,
                            backupFile,
                            sourceDatabase
                    );
                }

                case SQLITE -> {

                    System.out.println(
                            "Starting SQLite restore..."
                    );

                    SqliteRestoreService service =
                            new SqliteRestoreService();

                    service.restore(
                            backupFile,
                            database
                    );
                }
            }

            long elapsed =
                    System.currentTimeMillis()
                            - start;

            System.out.println();

            System.out.println(
                    "Restore completed successfully."
            );

            System.out.println(
                    "Time taken: "
                            + elapsed
                            + " ms"
            );

            return 0;

        } catch (Exception e) {

            System.err.println();

            System.err.println(
                    "Restore failed."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            return 1;

        } finally {

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