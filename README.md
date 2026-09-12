# Database Backup Utility

A command-line tool for backing up and restoring databases across multiple database systems — **MySQL**, **PostgreSQL**, **MongoDB**, and **SQLite**. Built with Spring Boot and Picocli.

## Features

- Test connectivity to a database before running a backup or restore
- Back up MySQL, PostgreSQL, MongoDB, and SQLite databases to a local file
- Restore a previous backup to a target database
- Optional GZIP compression of backup files (MongoDB backups are compressed natively via `mongodump --gzip`)
- Optional automatic backup verification (`--verify`): restores the backup to a temporary database and confirms it, right after the backup completes
- Simple, single-binary CLI built on top of native database tools (`mysqldump`, `pg_dump`, `mongodump`) plus a pure-JDBC path for SQLite (`VACUUM INTO`)

## Prerequisites

- **Java 21** or later
- **Maven** (or use the included `mvnw` / `mvnw.cmd` wrapper)
- The native command-line tools for whichever database(s) you plan to use, installed locally:
  - **MySQL**: `mysqldump` and `mysql` (from MySQL Server / MySQL Shell)
  - **PostgreSQL**: `pg_dump` and `psql` (from the PostgreSQL client tools)
  - **MongoDB**: `mongodump` and `mongorestore` (from the [MongoDB Database Tools](https://www.mongodb.com/try/download/database-tools))
  - **SQLite**: no external tool needed — backups use the JDBC driver's `VACUUM INTO` command directly

> **Note:** The paths to these native tools are currently hardcoded in each service class (e.g. `MongoDbBackupService`, `MySqlBackupService`, `PostgreSqlBackupService`, and their restore counterparts). Before running the utility, update those constants to match the install locations on your machine. For example, after installing MongoDB Database Tools with the default install location on Windows, `mongodump.exe` and `mongorestore.exe` are typically found at:
>
> ```
> C:\Program Files\MongoDB\Tools\100\bin\mongodump.exe
> C:\Program Files\MongoDB\Tools\100\bin\mongorestore.exe
> ```

## Building

```bash
./mvnw clean package
```

This produces a runnable Spring Boot jar in `target/`.

## Usage

Run the jar directly with Java, or via the Maven wrapper during development:

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar [COMMAND] [OPTIONS]
```

The tool exposes three subcommands: `test`, `backup`, and `restore`.

### Test a connection

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar test \
  --db MONGODB \
  --host localhost \
  --port 27017 \
  --database mydb
```

### Back up a database

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --db MONGODB \
  --host localhost \
  --port 27017 \
  --database mydb \
  --compress
```

Backups are written to a `backups/` directory (created automatically) with a timestamped filename, e.g.:

```
backups/mydb_2026-09-11_09-29-39_mongodb.archive.gz
```

For SQLite, pass the path to the `.db` file itself in `--database` (host/port are ignored):

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --db SQLITE \
  --database ./sqlite_test.db \
  --compress
```

Add `--verify` to any backup command to automatically restore the resulting backup to a temporary database and confirm it right away, e.g.:

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --db MONGODB \
  --host localhost \
  --port 27017 \
  --database mydb \
  --compress \
  --verify
```

### Restore a database

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar restore \
  --db MONGODB \
  --host localhost \
  --port 27017 \
  --database mydb_restored \
  --file backups/mydb_2026-09-11_09-29-39_mongodb.archive.gz \
  --source-database mydb
```

> **Note:** `--source-database` is required for MongoDB restores — it should match the original database name that was captured inside the archive when it was backed up.

### Common options

| Option              | Description                                              |
|----------------------|------------------------------------------------------------|
| `--db`               | Database type: `MYSQL`, `POSTGRESQL`, `MONGODB`, `SQLITE`  |
| `--host`             | Database host (default: `localhost`)                      |
| `--port`             | Database port (defaults to the standard port per DB type)  |
| `--username`         | Database username (required for MySQL/PostgreSQL)          |
| `--password`         | Database password (prompted interactively if omitted)      |
| `--database`         | Database name to back up, or target database for restore  |
| `--compress`         | (backup only) Compress the output with GZIP                |
| `--verify`           | (backup only) Restore the backup to a temporary database and verify it |
| `--file`             | (restore only) Path to the backup file to restore from     |
| `--source-database`  | (restore only, MongoDB) Original database name in the archive |

## Project Structure

```
src/main/java/com/dbbackup/databasebackuputility/
├── DatabaseBackupUtilityApplication.java   # Spring Boot entry point
├── command/                                 # Picocli CLI commands (test, backup, restore)
├── database/                                 # Per-database connection/test adapters
├── model/                                     # DatabaseConfig, DatabaseType
└── service/                                   # Backup/restore logic per database
    ├── MySqlBackupService / MySqlRestoreService
    ├── PostgreSqlBackupService / PostgreSqlRestoreService
    ├── MongoDbBackupService / MongoDbRestoreService
    ├── SqliteBackupService / SqliteRestoreService
    ├── CompressionService                       # GZIP compress/decompress
    └── BackupVerificationService                # Powers the --verify flag
```

## Status

- ✅ MySQL backup & restore
- ✅ PostgreSQL backup & restore
- ✅ MongoDB backup & restore
- ✅ SQLite backup & restore
- ✅ Optional post-backup verification (`--verify`)

## License

No license has been specified yet for this project.
