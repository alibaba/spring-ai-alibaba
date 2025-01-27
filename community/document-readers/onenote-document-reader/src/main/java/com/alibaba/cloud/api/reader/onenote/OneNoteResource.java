package com.alibaba.cloud.api.reader.onenote;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * @author sparkle6979l
 */
public class OneNoteResource implements Resource {
    public static final String SOURCE = "source";

    public enum ResourceType {
        NOTEBOOK, SECTION, PAGE

    }
    private final ResourceType resourceType;
    private final String resourceId;

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public OneNoteResource(String resourceId, ResourceType resourceType) {
        Assert.hasText(resourceId, "ResourceId must not be empty");
        Assert.notNull(resourceType, "ResourceType must not be null");

        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ResourceType resourceType;

        private String resourceId;

        public Builder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public OneNoteResource build() {
            Assert.hasText(resourceId, "ResourceId must not be empty");
            Assert.notNull(resourceType, "ResourceType must not be null");
            return new OneNoteResource(resourceId, resourceType);
        }

    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        return null;
    }

    @Override
    public URI getURI() throws IOException {
        return null;
    }

    @Override
    public File getFile() throws IOException {
        return null;
    }

    @Override
    public long contentLength() throws IOException {
        return 0;
    }

    @Override
    public long lastModified() throws IOException {
        return 0;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return null;
    }

    @Override
    public String getFilename() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }
}
