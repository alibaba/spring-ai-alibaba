package com.alibaba.cloud.ai.reader.email.msg;

/** Consolidated public interface for reading MSG files (this will probably work for other CFB
*   files but has special handling for some information found only in MSG files).
*/
public class MSG
{
	/** The file stream for the msg file */
	private java.io.FileInputStream stream;

	/** The FileChannel of the data stream, used to jump around the file. */
	private java.nio.channels.FileChannel fc;

	/** The file, as a memory-mapped byte file. */
	private java.nio.MappedByteBuffer mbb;

	/** The header */
	private Header header;

	/** The DIFAT */
	private DIFAT difat;

	/** The DAT */
	private FAT fat;

	/** The directory */
	private Directory directory;

	/** The Mini FAT */
	private MiniFAT miniFAT;

	/** The named properties */
	private NamedProperties namedProperties;

	/** Create a FileChannel for the given filename and read in the
	*	header, DIFAT, etc.
	*	@param	fn	The name of the file to read.
	*	@throws	NotCFBFileException	The input stream does not contain a PST file.
	*	@throws	UnknownStorageTypeException	The object type is not one of UNKNOWN, STORAGE, STREAM, or ROOT_STORAGE.
	* 	@throws	java.io.IOException	There was an I/O error reading the input stream.
	*/
	public MSG(String fn)
	throws
		NotCFBFileException,
		UnknownStorageTypeException,
		java.io.IOException
	{
		stream = new java.io.FileInputStream(fn);
		try {
			fc = stream.getChannel();
			try {
				mbb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fc.size());
				mbb.order(java.nio.ByteOrder.LITTLE_ENDIAN);

				header = new Header(mbb, fc.size());
				difat = new DIFAT(mbb, header);
				fat = new FAT(mbb, header, difat);
				directory = new Directory(mbb, header, fat);
				miniFAT = new MiniFAT(mbb, header, fat, directory);
				namedProperties = directory.namedPropertiesMappingEntry == null ? null : new NamedProperties(mbb, header, fat, directory, miniFAT);
			} catch (Exception e) {
				fc.close();
				throw e;
			}
		} catch (Exception e) {
			stream.close();
			throw e;
		}
	}

	/** Get an iterator through all attachments in the msg file
	*	@return	An iterator through the attachments found when reading in the directory
	*	@see	Directory#attachmentEntries
	*	@see	DirectoryEntryDataIterator
	*/
	public java.util.Iterator<DirectoryEntryData> attachments()
	{
		return new DirectoryEntryDataIterator(directory.attachmentEntries.iterator(), directory, namedProperties);
	}

	/** Close the file.
	* 	@throws	java.io.IOException	There was a problem closing the file.
	*/
	public void close()
	throws
		java.io.IOException
	{
		try {
			fc.close();
		} finally {
			stream.close();
		}
	}

	/** Create a string representation of the given bytes, assumed to be
	*   file content
	*	@param	ded	The entry to convert the data for.
	*	@param	data	The file contents
	*	@return	A string showing the file contents. This will be hex bytes if the field is not text.
	*/
	public String convertFileToString(DirectoryEntryData ded, byte[] data)
	{
		return ded.entry.getDataAsText(data);
	}

	/** Get the data from the DIFAT, as an array of key-value pairs.
	*	@return	A KVP array of the DIFAT entries, with the DIFAT indexas the key and the corresponding FAT sector as the value
	*/
	public KVPArray<Integer, Integer> difatData()
	{
		return difat.data();
	}

	/** Make FAT data available to client applications
	*	@return	An array of key-value pairs consisting of the stream names and the corresponding sector chains
	*/
	public KVPArray<String, String> fatData()
	{
		final String DIRECTORY_STREAM = "DirectoryStream";
		final String MINI_FAT_STREAM = "MiniFATStream";
		final String DIFAT_STREAM = "DIFATStream";
		final String APPLICATION_STREAM_FORMAT = "ApplicationDefinedStream%d";
		final String FREE_SECTORS = "FreeSectors";

		java.util.Map<String, Boolean> mandatoryEntries = new java.util.HashMap<String, Boolean>();
		mandatoryEntries.put(DIRECTORY_STREAM, false);
		mandatoryEntries.put(MINI_FAT_STREAM, false);
		mandatoryEntries.put(DIFAT_STREAM, false);

		KVPArray<String, String> l = new KVPArray<String, String>();

		java.util.Iterator<java.util.List<Integer>> chains = fat.getAllChains().iterator();
		int applicationChainIndex = 0;
		while (chains.hasNext()){
			java.util.List<Integer> chain = chains.next();
			int firstSector = chain.get(0);

			String entryName;
			if (firstSector == header.firstDirectorySectorLocation){
				entryName = DIRECTORY_STREAM;
				mandatoryEntries.put(entryName, true);
			} else if (firstSector == header.firstMiniFATSectorLocation){
				entryName = MINI_FAT_STREAM;
				mandatoryEntries.put(entryName, true);
			} else if (firstSector == header.firstDIFATSectorLocation){
				entryName = DIFAT_STREAM;
				mandatoryEntries.put(entryName, true);
			} else {
				entryName = String.format(APPLICATION_STREAM_FORMAT, applicationChainIndex++);
			}

			l.add(new KVPEntry<String, String>(entryName, getFATChainString(chain.iterator())));
		}

		java.util.Iterator<java.util.Map.Entry<String, Boolean>> iter = mandatoryEntries.entrySet().iterator();
		while (iter.hasNext()){
			java.util.Map.Entry<String, Boolean> entry = iter.next();
			if (!entry.getValue())
				l.add(new KVPEntry<String, String>(entry.getKey(), ""));
		}

		l.add(new KVPEntry<String, String>(FREE_SECTORS, getFATChainString(fat.freeSectorIterator())));
		return l;
	}

	/** Get an iterator through a DirectoryEntry's children via the proxy DirectoryEntryData object, and returning an iterator through DirectoryEntryData objects.
	*	@param	ded	The entry to get the child iterator for
	*	@return	An iterator through the entry's children, returning objects of type DirectoryEntryData for consumption by client applicaitons
	*/
	public java.util.Iterator<DirectoryEntryData> getChildIterator(DirectoryEntryData ded)
	{
		return ded.childIterator(directory, namedProperties);
	}

	/** Get the directory tree
	*	@return	A DirectoryEntryData structure for the root entry which links to its children
	*/
	public DirectoryEntryData getDirectoryTree()
	{
		return new DirectoryEntryData(directory.entries.get(0), directory, namedProperties);
	}

	/** Get the property entry for this entry's parent as a HashMap of properties indexed by the property tag.
	*	@param	ded	The entry to get the sibling properties entry of
	*	@return	A HashMap of {@link Property property values} read from the entry's parent's properties entry.
	*/
	public java.util.Map<Integer, Property> getParentPropertiesAsHashMap(DirectoryEntryData ded)
	{
		return getPropertiesAsHashMap(directory.parents.get(ded.entry));
	}

	/** Get the header for a property entry. The interpretation of the header changes depending on the type of the entry's parent.
	*	@param	ded	The directory entry to retrieve the header from
	*	@param	data	The bytes to read the header from
	*	@return	A KVPArray of header property field names and values, which will be empty for recipient objects and all attachment objects except embedded msg files.
	*/
	public KVPArray<String, Integer> getPropertiesHeader(DirectoryEntryData ded, byte[] data)
	{
		return directory.parents.get(ded.entry).getChildPropertiesHeader(data);
	}

	/** Parse the given data for a property entry and return a HashMap of properties indexed by the property tag.
	*	@param	ded	The entry to parse the data for
	*	@param	data	The content of the entry to be parsed
	*	@return	A HashMap of {@link Property property values} read from the entry.
	*/
	public java.util.Map<Integer, Property> parsePropertiesAsHashMap(DirectoryEntryData ded, byte[] data)
	{
		return ded.entry.propertiesAsHashMap(data, directory.parents.get(ded.entry), namedProperties);
	}

	/** Parse the given data for a property entry and return an ArrayList of properties.
	*	@param	ded	The entry to parse the data for
	*	@param	data	The content of the entry to be parsed
	*	@return	An ArrayList of {@link Property property values} read from the entry.
	*/
	public java.util.List<Property> parsePropertiesAsList(DirectoryEntryData ded, byte[] data)
	{
		return ded.entry.propertiesAsList(data, directory.parents.get(ded.entry), namedProperties);
	}

	/** Get the properties for a given Root Storage, Attachment, or Recipient entry as a HashMap indexed by the property tag
	*	@param	de	The entry to retrieve the properties for.
	*	@return	A HashMap of {@link Property property values} read from the entry.
	*/
	private java.util.Map<Integer, Property> getPropertiesAsHashMap(DirectoryEntry de)
	{
		java.util.Iterator<DirectoryEntry> iter = directory.propertyEntries.iterator();
		while (iter.hasNext())
		{
			DirectoryEntry propertiesEntry = iter.next();
			if (directory.parents.get(propertiesEntry).equals(de)) {
				byte[] data = propertiesEntry.getContent(mbb, header, fat, miniFAT);
				return propertiesEntry.propertiesAsHashMap(data, de, namedProperties);
			}
		}
		return new java.util.HashMap<Integer, Property>();
	}

	/** Get the properties for a given Root Storage, Attachment, or Recipient entry as a HashMap indexed by the property tag
	*	@param	ded	The entry to retrieve the properties for.
	*	@return	A HashMap of {@link Property property values} read from the entry.
	*/
	public java.util.Map<Integer, Property> getPropertiesAsHashMap(DirectoryEntryData ded)
	{
		return getPropertiesAsHashMap(ded.entry);
	}

	/** Get the properties for a given Root Storage, Attachment, or Recipient entry as an ArrayList
	*	@param	ded	The entry to retrieve the properties for.
	*	@return	An ArrayList of {@link Property property values} read from the entry.
	*/
	public java.util.List<Property> getPropertiesAsList(DirectoryEntryData ded)
	{
		java.util.Iterator<DirectoryEntry> iter = directory.propertyEntries.iterator();
		while (iter.hasNext())
		{
			DirectoryEntry propertiesEntry = iter.next();
			if (directory.parents.get(propertiesEntry).equals(ded.entry)) {
				byte[] data = propertiesEntry.getContent(mbb, header, fat, miniFAT);
				return propertiesEntry.propertiesAsList(data, ded.entry, namedProperties);
			}
		}
		return new java.util.ArrayList<Property>();
	}

	/** Retrieve the value for a property, as a String
	*	@param	property	The property to retrieve the value of
	*	@return	A String showing the property's value.
	*/
	public String getPropertyValue(Property property)
	{
		if (property.storedInProperty)
			return property.value();

		java.util.Iterator<DirectoryEntry> iter = directory.getChildren(property.parent).iterator();
		while (iter.hasNext()) {
			DirectoryEntry de = iter.next();
			if (de.getPropertyTag() == property.propertyTag) {
				byte[] data = de.getContent(mbb, header, fat, miniFAT);
				return de.getDataAsText(data);
			}
		}
		return null;
	}

	/** Is the given directory entry a Root Storage Object?
	*	@param	ded	The directory entry
	*	@return	true if this entry is a Root Storage Object, false otherwise.
	*/
	public boolean isRootStorageObject(DirectoryEntryData ded)
	{
		return ded.entry.objectType.isRootStorage();
	}

	/** Is the given directory entry a Storage Object?
	*	@param	ded	The directory entry
	*	@return	true if this entry is a Storage Object, false otherwise.
	*/
	public boolean isStorageObject(DirectoryEntryData ded)
	{
		return ded.entry.objectType.isStorage();
	}

	/** Is the given directory entry a Stream Object?
	*	@param	ded	The directory entry to check.
	*	@return	true if this entry is a Stream Object, false otherwise.
	*/
	public boolean isStreamObject(DirectoryEntryData ded)
	{
		return ded.entry.objectType.isStream();
	}

	/** Get the directory entry keys (this allows a table for display to be set up with the correct number of entries before we have any data)
	*	@return	A list of keys and values in the same order as
	*		getDirectoryEntryData but with empty strings for the values
	*/
	public static KVPArray<String, String> getDirectoryEntryKeys()
	{
		return DirectoryEntry.keys();
	}

	/** Get the FAT sector chain for the given iterator as a String
	*	@param	iterator	The iterator to create the chain
	*				description for
	*	@return	A String listing the sectors in the chain
	*/
	private String getFATChainString(java.util.Iterator<Integer> iterator)
	{
		StringBuilder chain = new StringBuilder();
		while (iterator.hasNext()){
			if (chain.length() > 0)
				chain.append(" ");
			chain.append(iterator.next());
		}
		return chain.toString();
	}

	/** Get the file pointed to by the given directory entry index
	*	@param	ded	The entry to retrieve the file for
	*	@return	An array of the bytes in the file.
	*/
	public byte[] getFile(DirectoryEntryData ded)
	{
		return ded.entry.getContent(mbb, header, fat, miniFAT);
	}

	/** Get the mini FAT data as a table consisting of the mini FAT sectors in the first column, and the data in the second.
	*	@return	An array of the mini FAT chains and data
	*/
	public KVPArray<java.util.List<Integer>, byte[]> miniFATData()
	{
		KVPArray<java.util.List<Integer>, byte[]> l = new KVPArray<java.util.List<Integer>, byte[]>();

		java.util.Iterator<java.util.List<Integer>> chains = miniFAT.getAllChains().iterator();
		while (chains.hasNext()){
			java.util.List<Integer> chain = chains.next();
			java.util.Iterator<Integer> iter = chain.iterator();
			int destOffset = 0;
			byte[] data = new byte[chain.size()*header.miniSectorSize];
			while (iter.hasNext()){
				mbb.position(miniFAT.fileOffset(iter.next()));
				mbb.get(data, destOffset, header.miniSectorSize);
				destOffset += header.miniSectorSize;
			}

			l.add(new KVPEntry<java.util.List<Integer>, byte[]>(chain, data));
		}

		return l;
	}

	/** Get the raw bytes for the requested directory entry
	*	@param	ded	The entry to retrieve data for
	*	@return	An array of the bytes in the directory entry.
	*/
	public byte[] getRawDirectoryEntry(DirectoryEntryData ded)
	{
		mbb.position(ded.entry.directoryEntryPosition);
		byte[] data = new byte[DirectoryEntry.SIZE];
		mbb.get(data);
		return data;
	}

	/** Retrieve the contents of the requested sector.
	*	@param	i	The 0-based sector to retrieve. Note that this is not a sector number (sector #0 is physical sector 1, etc).
	*	@return	An array of bytes holding the stream contents
	*/
	public byte[] getSector(int i)
	{
		mbb.position(i*header.sectorSize);
		byte[] data = new byte[header.sectorSize];
		mbb.get(data);
		return data;
	}

	/** Get the data from the header, as an array of key-value pairs.
	*	@return	A KVP array of the header field names and values
	*/
	public KVPArray<String, String> headerData()
	{
		return header.data();
	}

	/** Is the given entry a Property entry?
	*	@param	ded	The directory entry to check the type of
	*	@return	true if the entry is a Properties entry, false otherwise
	*/
	public boolean isProperty(DirectoryEntryData ded)
	{
		return directory.propertyEntries.contains(ded.entry);
	}

	/** Get a Named Property entry
	*	@param	mappingIndex	The index to the named property entry to retrieve
	*	@return	A KVP array of the information for the requested entry
	*/
	public KVPArray<String, String> namedPropertyEntry(int mappingIndex)
	{
		return namedProperties.getPropertyIdToPropertyNameMapping(mappingIndex);
	}

	/** Get the list of Named Properties GUIDs
	*	@return	The array of GUIDs as Strings
	*/
	public String[] namedPropertiesGUIDs()
	{
		String[] guidStrings = new String[namedProperties.guids.length];
		for (int i = 0; i < namedProperties.guids.length; ++i)
			guidStrings[i] = namedProperties.guids[i].toString();
		return guidStrings;
	}

	/** Get the numeric named properties entries
	*	@return	An ArrayList containing the named properties' numeric entries
	*/
	public java.util.List<EntryStreamEntryData> namedPropertiesNumericalEntries()
	{
		return namedProperties.getEntryStreamEntries(EntryStreamEntry.PropertyType.NUMERICAL_NAMED_PROPERTY);
	}

	/** Get the string named properties entries
	*	@return	An ArrayList containing the named properties' string entries
	*/
	public java.util.List<EntryStreamEntryData> namedPropertiesStringEntries()
	{
		return namedProperties.getEntryStreamEntries(EntryStreamEntry.PropertyType.STRING_NAMED_PROPERTY);
	}

	/** Get the named properties string stream as an array of key-value pairs.
	*	@return	A KVP array of the named property string stream entries as
	*/
	public KVPArray<Integer, String> namedPropertiesStrings()
	{
		KVPArray<Integer, String> a = new KVPArray<Integer, String>();
		for (java.util.Iterator<java.util.Map.Entry<Integer, String>> iter = namedProperties.stringsByOffset.entrySet().iterator(); iter.hasNext(); ){
			java.util.Map.Entry<Integer, String> entry = iter.next();
			a.add(entry.getKey(), entry.getValue());
		}
		return a;
	}

	/** Get the number of sectors in the file
	*	@return	The number of sectors in the file
	*/
	public int numberOfSectors()
	{
		return header.numberOfSectors();
	}

	/** Get an iterator through all recipients in the msg file
	*	@return	An iterator through the recipients found when reading in the directory
	*	@see	Directory#recipientEntries
	*	@see	DirectoryEntryDataIterator
	*/
	public java.util.Iterator<DirectoryEntryData> recipients()
	{
		return new DirectoryEntryDataIterator(directory.recipientEntries.iterator(), directory, namedProperties);
	}

	/** Get the ByteBuffer for this MSG file
	*   @return The ByteBuffer containing the MSG file data
	*/
	public java.nio.MappedByteBuffer getByteBuffer() {
		return mbb;
	}

	/** Get the header for this MSG file
	*   @return The MSG file header
	*/
	public Header getHeader() {
		return header;
	}

	/** Get the FAT for this MSG file
	*   @return The File Allocation Table
	*/
	public FAT getFAT() {
		return fat;
	}

	/** Get the Mini FAT for this MSG file
	*   @return The Mini File Allocation Table
	*/
	public MiniFAT getMiniFAT() {
		return miniFAT;
	}

	/** Get the directory for this MSG file
	*   @return The directory
	*/
	public Directory getDirectory() {
		return directory;
	}

	/** Get the named properties for this MSG file
	*   @return The named properties
	*/
	public NamedProperties getNamedProperties() {
		return namedProperties;
	}
}
