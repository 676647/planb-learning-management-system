package fr.skyframe.lms.storage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * Chooses the file store from the lms.storage.type property:
 * "gridfs" (MongoDB, the default) or "memory" (tests, or running without MongoDB).
 */
@Configuration
public class StorageConfig {

    @Bean
    FileStorage fileStorage(@Value("${lms.storage.type}") String type,
                            ObjectProvider<GridFsTemplate> gridFsTemplate) {
        return switch (type.trim().toLowerCase()) {
            // Spring Boot creates the GridFsTemplate from spring.data.mongodb.uri
            case "gridfs" -> new GridFsFileStorage(gridFsTemplate.getObject());
            case "memory" -> new InMemoryFileStorage();
            default -> throw new IllegalStateException("lms.storage.type must be 'gridfs' or 'memory', not " + type);
        };
    }
}
