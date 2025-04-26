package com.alibaba.cloud.ai.graph.utils;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFileStorage {

    private static final Map<String, FileRecord> CACHE = new ConcurrentHashMap<>();

    public static class FileRecord {
        private final String id;
        private final String fileKey;
        private final String name;
        private final String mimetype;
        private final long size;
        private final byte[] content;

        public FileRecord(String id,
                          String fileKey,
                          String name,
                          String mimetype,
                          long size,
                          byte[] content) {
            this.id = id;
            this.fileKey = fileKey;
            this.name = name;
            this.mimetype = mimetype;
            this.size = size;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public String getFileKey() {
            return fileKey;
        }

        public String getName() {
            return name;
        }

        public String getMimetype() {
            return mimetype;
        }

        public long getSize() {
            return size;
        }

        public byte[] getContent() {
            return content;
        }
    }

    public static FileRecord save(byte[] content,
                                  String mimetype,
                                  String originalFilename) {
        String id = UUID.randomUUID().toString();
        String extension = Optional.of(
                org.springframework.http.MediaType.parseMediaType(mimetype).getSubtype()
        ).orElse("bin");
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : id + "." + extension;
        String key = String.format("inmem://%s", id);
        FileRecord record = new FileRecord(id, key, filename, mimetype, content.length, content);
        CACHE.put(id, record);
        return record;
    }

    public static FileRecord get(String id) {
        return CACHE.get(id);
    }

    public static void remove(String id) {
        CACHE.remove(id);
    }

    public static void clear() {
        CACHE.clear();
    }
}
