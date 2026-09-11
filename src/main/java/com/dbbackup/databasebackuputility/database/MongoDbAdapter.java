package com.dbbackup.databasebackuputility.database;

import com.dbbackup.databasebackuputility.model.DatabaseConfig;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.util.List;

public class MongoDbAdapter implements DatabaseAdapter {

    @Override
    public void testConnection(DatabaseConfig config) throws Exception {

        MongoClientSettings.Builder settingsBuilder =
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(
                                        List.of(
                                                new ServerAddress(
                                                        config.host(),
                                                        config.port()
                                                )
                                        )
                                )
                        );

        // Authentication is optional for local MongoDB installations
        if (config.username() != null
                && !config.username().isBlank()
                && config.password() != null) {

            MongoCredential credential =
                    MongoCredential.createCredential(
                            config.username(),
                            config.database(),
                            config.password()
                    );

            settingsBuilder.credential(credential);
        }

        try (MongoClient mongoClient =
                     MongoClients.create(settingsBuilder.build())) {

            mongoClient
                    .getDatabase(config.database())
                    .runCommand(new Document("ping", 1));
        }
    }
}