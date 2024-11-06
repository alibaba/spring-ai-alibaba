package org.bsc.langgraph4j;


import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public interface DotEnvConfig {

    static void load()  {

        // Search for .env file
        Path path = Paths.get(".").toAbsolutePath();

        Path filePath = Paths.get( path.toString(), ".env");
        System.out.println(filePath);

        for( int i=0; !filePath.toFile().exists(); ++i ) {
            path = path.getParent();

            filePath = Paths.get(path.toString(), ".env");

            if (i == 3) {
                throw new RuntimeException("no .env file found!"+filePath);
            }
        }

        // load .env contents in System.properties
        try {
            final java.util.Properties properties = new java.util.Properties();

            try( Reader r = new FileReader(filePath.toFile())) {
                properties.load(r);
            }
            System.getProperties().putAll(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    static Optional<String> valueOf(String key ) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return ofNullable(value);
    }


}
