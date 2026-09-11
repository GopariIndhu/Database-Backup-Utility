package com.dbbackup.databasebackuputility.command;

import com.dbbackup.databasebackuputility.database.DatabaseAdapter;
import com.dbbackup.databasebackuputility.database.MySqlAdapter;
import com.dbbackup.databasebackuputility.model.DatabaseConfig;
import com.dbbackup.databasebackuputility.model.DatabaseType;
import com.dbbackup.databasebackuputility.database.PostgreSqlAdapter;
import com.dbbackup.databasebackuputility.database.MongoDbAdapter;
import com.dbbackup.databasebackuputility.database.SqliteAdapter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Arrays;

@Command(
        name = "test",
        description = "Test a database connection",
        mixinStandardHelpOptions = true
)
public class TestConnectionCommand implements Runnable {

    @Option(
            names = "--db",
            required = true,
            description = "Database type: MYSQL, POSTGRESQL, MONGODB, SQLITE"
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
            defaultValue = "3306",
            description = "Database port"
    )
    private int port;

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
            description = "Database name"
    )
    private String database;

    @Override
    public void run() {

        DatabaseConfig config = new DatabaseConfig(
                databaseType,
                host,
                port,
                username,
                password,
                database
        );

        try {

            DatabaseAdapter adapter = getAdapter(databaseType);

            System.out.println("Testing " + databaseType + " connection...");

            if (databaseType == DatabaseType.SQLITE) {

                System.out.println("Database file: " + database);

            } else {

                System.out.println("Host: " + host);
                System.out.println("Port: " + port);
                System.out.println("Database: " + database);
            }

            adapter.testConnection(config);

            System.out.println();
            System.out.println("Connection successful.");

        } catch (Exception e) {

            System.err.println();
            System.err.println("Connection failed.");
            System.err.println("Reason: " + e.getMessage());

        } finally {

            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private DatabaseAdapter getAdapter(DatabaseType databaseType) {

        return switch (databaseType) {

            case MYSQL -> new MySqlAdapter();

            case POSTGRESQL -> new PostgreSqlAdapter();

            case MONGODB -> new MongoDbAdapter();

            case SQLITE -> new SqliteAdapter();
        };
    }
}