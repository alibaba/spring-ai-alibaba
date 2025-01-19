/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.reader.email.msg;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A document reader for Outlook MSG files.
 * This class reads MSG files and extracts their content into Document objects.
 * 
 * @author xiadong
 * @since 0.8.0
 */
public class MsgEmailDocumentReader implements DocumentReader {

    private final Resource resource;

    public MsgEmailDocumentReader(Resource resource) {
        this.resource = resource;
    }

    @Override
    public List<Document> get() {
        List<Document> documents = new ArrayList<>();
        MSG msg = null;
        
        try {
            // Create MSG object to parse the file
            msg = new MSG(resource.getFile().getAbsolutePath());
            
            // Get the root directory entry which contains all message data
            DirectoryEntryData rootEntry = msg.getDirectoryTree();
            
            // Get properties of the root entry
            Map<Integer, Property> properties = msg.getPropertiesAsHashMap(rootEntry);
            
            // Extract relevant properties
            String subject = getPropertyValue(properties, PropertyTags.PidTagSubject);
            String body = getPropertyValue(properties, PropertyTags.PidTagBody);
            String sender = getPropertyValue(properties, PropertyTags.PidTagSenderName);
            String recipients = getPropertyValue(properties, PropertyTags.PidTagDisplayTo);
            String receivedTime = getPropertyValue(properties, PropertyTags.PidTagMessageDeliveryTime);
            String importance = getPropertyValue(properties, PropertyTags.PidTagImportance);
            
            // Create metadata map
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("subject", subject != null ? subject : "");
            metadata.put("sender", sender != null ? sender : "");
            metadata.put("recipients", recipients != null ? recipients : "");
            metadata.put("received_time", receivedTime != null ? receivedTime : "");
            metadata.put("importance", importance != null ? importance : "");
            metadata.put("type", "email/msg");
            metadata.put("source", resource.getURI().toString());
            
            // Create document with body text and metadata
            Document document = new Document(body != null ? body : "", metadata);
            documents.add(document);
            
            // Process attachments
            java.util.Iterator<DirectoryEntryData> attachments = msg.attachments();
            while (attachments.hasNext()) {
                DirectoryEntryData attachment = attachments.next();
                Map<Integer, Property> attachmentProps = msg.getPropertiesAsHashMap(attachment);
                
                String fileName = getPropertyValue(attachmentProps, PropertyTags.PidTagAttachLongFilename);
                if (fileName == null) {
                    fileName = getPropertyValue(attachmentProps, PropertyTags.PidTagAttachFilename);
                }
                
                String contentType = getPropertyValue(attachmentProps, PropertyTags.PidTagAttachMimeTag);
                
                // Add attachment metadata to document
                if (fileName != null) {
                    Map<String, Object> attachmentMetadata = new HashMap<>();
                    attachmentMetadata.put("filename", fileName);
                    attachmentMetadata.put("content_type", contentType != null ? contentType : "application/octet-stream");
                    document.getMetadata().put("attachment_" + fileName, attachmentMetadata);
                }
            }
            
        } catch (NotCFBFileException | UnknownStorageTypeException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (msg != null) {
                try {
                    msg.close();
                } catch (IOException e) {
                    // Log warning about failure to close the file
                }
            }
        }
        
        return documents;
    }
    
    /**
     * Helper method to get property value as string
     */
    private String getPropertyValue(Map<Integer, Property> properties, int propertyTag) {
        Property property = properties.get(propertyTag);
        return property != null ? property.toString() : null;
    }

}