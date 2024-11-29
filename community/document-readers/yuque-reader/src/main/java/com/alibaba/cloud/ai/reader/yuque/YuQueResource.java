package com.alibaba.cloud.ai.reader.yuque;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YunLong
 */
public class YuQueResource implements Resource {

    private static final String baseUrl = "https://www.yuque.com";

    private static final String infoPath = "/api/v2/hello";

    private static final String docDetailPath = "/api/v2/repos/%s/%s/docs/%s";

    private final HttpClient httpClient;

    private final String content;

    public static final String SOURCE = "source";
    private String groupLogin;
    private String bookSlug;
    private String id;

    public YuQueResource(String yuQueToken, String resourcePath) {

        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

        judgePathRule(resourcePath);
        judgeToken(yuQueToken);

        URI uri = URI.create(baseUrl + docDetailPath.formatted(groupLogin, bookSlug, id));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .header("X-Auth-Token", yuQueToken)
                .uri(uri).GET().build();

        try {
            HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            // Parse the JSON response using FastJSON
            JSONObject jsonObject = JSON.parseObject(body);
            JSONObject dataObject = jsonObject.getJSONObject("data");

            if (dataObject != null) {
                content = dataObject.getString("body_html");
            } else {
                throw new IllegalArgumentException("Invalid response format: 'data' is not an object");
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String getContent() {
        return content;
    }

    /**
     * Judge resource path rule
     * Official online doc https://www.yuque.com/yuque/developer/openapi
     *
     * @param resourcePath
     */
    private void judgePathRule(String resourcePath) {

        // Determine if the path conforms to this format： https://xx.xxx.com/aa/bb/cc
        String regex = "^https://[a-zA-Z0-9.-]+/([a-zA-Z0-9.-]+)/([a-zA-Z0-9.-]+)/([a-zA-Z0-9.-]+)$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(resourcePath);
        Assert.isTrue(matcher.matches(), "Invalid resource path");

        // Extract the captured groups
        this.groupLogin = matcher.group(1);
        this.bookSlug = matcher.group(2);
        this.id = matcher.group(3);
        Assert.isTrue(StringUtils.hasText(this.groupLogin), "Invalid resource path");
        Assert.isTrue(StringUtils.hasText(this.bookSlug), "Invalid resource path");
        Assert.isTrue(StringUtils.hasText(this.id), "Invalid resource path");
    }

    /**
     * judge yuQue token
     *
     * @param yuQueToken User/Team token
     */
    private void judgeToken(String yuQueToken) {
        URI uri = URI.create(baseUrl + infoPath);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .header("X-Auth-Token", yuQueToken)
                .uri(uri).GET().build();

        try {
            HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            Assert.isTrue(statusCode == 200, "Failed to auth YuQueToken");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String yuQueToken;

        private String resourcePath;

        public Builder yuQueToken(String yuQueToken) {
            this.yuQueToken = yuQueToken;
            return this;
        }

        public Builder resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }


        public YuQueResource build() {
            Assert.notNull(yuQueToken, "YuQueToken must not be null");
            Assert.notNull(resourcePath, "ResourcePath must not be null");
            return new YuQueResource(yuQueToken, resourcePath);
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
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

}
