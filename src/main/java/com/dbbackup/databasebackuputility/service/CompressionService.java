package com.dbbackup.databasebackuputility.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class CompressionService {

    public Path compress(Path sourceFile) throws IOException {

        Path compressedFile =
                Path.of(sourceFile.toString() + ".gz");

        try (
                InputStream input = Files.newInputStream(sourceFile);
                OutputStream output = Files.newOutputStream(compressedFile);
                GZIPOutputStream gzipOutputStream =
                        new GZIPOutputStream(output)
        ) {

            input.transferTo(gzipOutputStream);
        }

        return compressedFile;
    }
}