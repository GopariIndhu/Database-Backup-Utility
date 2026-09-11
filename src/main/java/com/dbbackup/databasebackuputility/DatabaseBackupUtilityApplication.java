package com.dbbackup.databasebackuputility;

import com.dbbackup.databasebackuputility.command.RootCommand;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class DatabaseBackupUtilityApplication {

	public static void main(String[] args) {

		int exitCode = new CommandLine(new RootCommand())
				.execute(args);

		System.exit(exitCode);
	}
}