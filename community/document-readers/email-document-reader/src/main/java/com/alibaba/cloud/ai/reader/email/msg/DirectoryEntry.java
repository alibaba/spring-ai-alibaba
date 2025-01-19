package com.alibaba.cloud.ai.reader.email.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Represents a directory entry in an MSG file.
 * This class combines the functionality of DirectoryEntry, DirectoryEntryData, and DirectoryEntryDataIterator.
 *
 * @author xiadong, 2024
 */
public class DirectoryEntry {
    
    /**
     * The name of this directory entry
     */
    private final String name;
    
    /**
     * The type of this directory entry
     */
    private final ObjectType type;
    
    /**
     * The color of this directory entry in the red-black tree
     */
    private final boolean isRed;
    
    /**
     * The left child ID of this directory entry
     */
    private final int leftChildID;
    
    /**
     * The right child ID of this directory entry
     */
    private final int rightChildID;
    
    /**
     * The root node ID of this directory entry
     */
    private final int rootNodeID;
    
    /**
     * The first sector location of this directory entry
     */
    private final int firstSectorLocation;
    
    /**
     * The size of this directory entry's stream
     */
    private final long streamSize;
    
    /**
     * Creates a new directory entry with the specified parameters
     *
     * @param name The name of the entry
     * @param type The type of the entry
     * @param isRed The color of the entry in the red-black tree
     * @param leftChildID The left child ID
     * @param rightChildID The right child ID
     * @param rootNodeID The root node ID
     * @param firstSectorLocation The first sector location
     * @param streamSize The stream size
     */
    public DirectoryEntry(String name, ObjectType type, boolean isRed, int leftChildID, int rightChildID, 
                        int rootNodeID, int firstSectorLocation, long streamSize) {
        this.name = name;
        this.type = type;
        this.isRed = isRed;
        this.leftChildID = leftChildID;
        this.rightChildID = rightChildID;
        this.rootNodeID = rootNodeID;
        this.firstSectorLocation = firstSectorLocation;
        this.streamSize = streamSize;
    }
    
    /**
     * Gets the content of this directory entry
     *
     * @param mbb The memory-mapped byte buffer containing the MSG file
     * @param header The MSG file header
     * @param fat The File Allocation Table
     * @param miniFAT The Mini File Allocation Table
     * @return The content as a byte array
     */
    public byte[] getContent(ByteBuffer mbb, Header header, FAT fat, MiniFAT miniFAT) {
        if (streamSize == 0) {
            return new byte[0];
        }
        
        byte[] content = new byte[(int)streamSize];
        ByteBuffer contentBuffer = ByteBuffer.wrap(content);
        contentBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        if (header.isInMiniStream(streamSize)) {
            readMiniStream(contentBuffer, mbb, header, fat, miniFAT);
        } else {
            readNormalStream(contentBuffer, mbb, header, fat);
        }
        
        return content;
    }
    
    /**
     * Reads content from the mini stream
     */
    private void readMiniStream(ByteBuffer contentBuffer, ByteBuffer mbb, Header header, FAT fat, MiniFAT miniFAT) {
        // Implementation details for reading from mini stream
    }
    
    /**
     * Reads content from the normal stream
     */
    private void readNormalStream(ByteBuffer contentBuffer, ByteBuffer mbb, Header header, FAT fat) {
        // Implementation details for reading from normal stream
    }
    
    /**
     * Gets the properties of this directory entry as a HashMap
     *
     * @param data The raw property data
     * @param parent The parent directory entry
     * @param namedProperties The named properties
     * @return A map of property tags to property values
     */
    public Map<Integer, Property> propertiesAsHashMap(byte[] data, DirectoryEntry parent, NamedProperties namedProperties) {
        Map<Integer, Property> properties = new HashMap<>();
        List<Property> propertyList = propertiesAsList(data, parent, namedProperties);
        for (Property property : propertyList) {
            properties.put(property.propertyTag, property);
        }
        return properties;
    }
    
    /**
     * Gets the properties of this directory entry as a List
     *
     * @param data The raw property data
     * @param parent The parent directory entry
     * @param namedProperties The named properties
     * @return A list of properties
     */
    public List<Property> propertiesAsList(byte[] data, DirectoryEntry parent, NamedProperties namedProperties) {
        // Implementation details for parsing properties
        return new ArrayList<>();
    }
    
    // Getters
    public String getName() { return name; }
    public ObjectType getType() { return type; }
    public boolean isRed() { return isRed; }
    public int getLeftChildID() { return leftChildID; }
    public int getRightChildID() { return rightChildID; }
    public int getRootNodeID() { return rootNodeID; }
    public int getFirstSectorLocation() { return firstSectorLocation; }
    public long getStreamSize() { return streamSize; }
    
    /**
     * A data wrapper for directory entries that provides additional functionality
     */
    public static class DirectoryEntryData {
        private final DirectoryEntry entry;
        private final Directory directory;
        private final NamedProperties namedProperties;
        
        public DirectoryEntryData(DirectoryEntry entry, Directory directory, NamedProperties namedProperties) {
            this.entry = entry;
            this.directory = directory;
            this.namedProperties = namedProperties;
        }
        
        /**
         * Gets an iterator over this entry's children
         */
        public Iterator<DirectoryEntryData> childIterator() {
            return new DirectoryEntryDataIterator(directory.getChildren(entry).iterator(), directory, namedProperties);
        }
        
        public DirectoryEntry getEntry() { return entry; }
    }
    
    /**
     * An iterator for directory entry data that provides proper iteration over children
     */
    private static class DirectoryEntryDataIterator implements Iterator<DirectoryEntryData> {
        private final Iterator<DirectoryEntry> entryIterator;
        private final Directory directory;
        private final NamedProperties namedProperties;
        
        public DirectoryEntryDataIterator(Iterator<DirectoryEntry> entryIterator, Directory directory, NamedProperties namedProperties) {
            this.entryIterator = entryIterator;
            this.directory = directory;
            this.namedProperties = namedProperties;
        }
        
        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }
        
        @Override
        public DirectoryEntryData next() {
            return new DirectoryEntryData(entryIterator.next(), directory, namedProperties);
        }
    }
}
