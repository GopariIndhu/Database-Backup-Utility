package com.dbbackup.databasebackuputility.command;

import picocli.CommandLine.Command;

@Command(
        name = "dbbackup",
        description = "Backup and restore multiple database systems",
        version = "Database Backup Utility 1.0",
        mixinStandardHelpOptions = true,
        subcommands = {
                TestConnectionCommand.class,
                BackupCommand.class,
                RestoreCommand.class
        }
)
public class RootCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Database Backup Utility");
        System.out.println("Use --help to see available commands.");
    }
}