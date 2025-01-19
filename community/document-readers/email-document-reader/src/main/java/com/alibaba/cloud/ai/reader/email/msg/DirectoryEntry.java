package com.alibaba.cloud.ai.reader.email.msg;

/** The Directory Entry object contains a Compound File Directory Sector
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/1a69e000-f391-4c03-9d43-32d5f554bca7">MS-OXMSG Section 2.3: Top Level Structure</a>
*/
public class DirectoryEntry {

	/** The directory top-level entry
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/026fde6e-143d-41bf-a7da-c08b2130d50e">MS-CFB Section 2.6.2: Root Directory Entry</a>
	*/
	private static final String ROOT_ENTRY = "Root Entry";

	/** The Named Property Mapping Storage
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/193c169b-0628-4392-aa51-83009be7d71f">MS-OXMSG Section 2.2.3: Named Property Mapping Storage</a>
	*/
	private static final String NAMEID = "__nameid_version1.0";

	/** String Stream entry name template
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/08185828-e9e9-4ef2-bcd2-f6e69c00891b">MS-OXMSG Section 2.1.3: Variable Length Properties</a>
	*/
	private static final java.util.regex.Pattern STRING_STREAM_PATTERN = java.util.regex.Pattern.compile("__substg1.0_(\\p{XDigit}{8})");

	/** Property Stream entries (One under the Root Entry, and one under each Recipient and each Attachment)
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/20c1125f-043d-42d9-b1dc-cb9b7e5198ef">MS-OXMSG Section 2.4: Property Stream</a>
	*/
	private static final String PROPERTIES = "__properties_version1.0";

	/** Recipient Object Storage entry name template
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/38a5cb3c-4454-48ba-b113-6de75321b67f">MS-OXMSG Section 2.2.1: Recipient Object Storage</a>
	*/
	private static final java.util.regex.Pattern RECIP_PATTERN = java.util.regex.Pattern.compile("__recip_version1.0_#\\p{XDigit}{8}");

	/** Attachment Object Storage entry name template
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/8590d60d-4173-4ca8-9cb2-190aae006fbd">MS-OXMSG Section 2.2.2: Attachment Object Storage</a>
	*/
	private static final java.util.regex.Pattern ATTACH_PATTERN = java.util.regex.Pattern.compile("__attach_version1.0_#\\p{XDigit}{8}");

	/** Unallocated directory entries
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/b37413bb-f3ef-4adc-b18e-29bddd62c26e">MS-CFG Section 2.6.3: Other Directory Entries</a>
	*/
	private static final String UNALLOCATED = "";

	/** KVP key for next recipient ID entry to use in a property header for a message object (either the root message or an embedded message)
	*/
	private static final String NEXT_RECIPIENT_ID = "next-recipient-id";

	/** KVP key for next attachment ID entry to use in a property header for a message object (either the root message or an embedded message)
	*/
	private static final String NEXT_ATTACHMENT_ID = "next-attachment-id";

	/** KVP key for the number of recipient entries in a property header for a message object (either the root message or an embedded message)
	*/
	private static final String RECIPIENT_COUNT = "recipient-count";

	/** KVP key for next attachment ID entry to use in a property headers for a message object (either the root message or an embedded message)
	*/
	private static final String ATTACHMENT_COUNT = "recipient-count";

	/** Property tags are only defined for string stream entries; 0x0000 is never used as a property tag, so we use it as a sentinel value to
	*   indicate no property ID exists for other classes
	*	@see #getPropertyTag
	*/
	private static final int NO_PROPERTY_TAG = 0x0000;

	/** Data definition key and KVP key for the {@link #directoryEntryName}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_DirectoryEntryName = "DirectoryEntryName";

	/** Data definition key and KVP key for the directory entry name length. This value is not stored as a member in this class. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_DirectoryEntryNameLength = "DirectoryEntryNameLength";

	/** Data definition key and KVP key for the {@link #objectType}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_ObjectType = "ObjectType";

	/** Data definition key and KVP key for the color flag, which is not stored directly as a member variable but could be retrieved from {@link #dc}
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_ColorFlag = "ColorFlag";

	/** Data definition key and KVP key for the {@link #leftSiblingId}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_LeftSiblingId = "LeftSiblingId";

	/** Data definition key and KVP key for the {@link #rightSiblingId}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_RightSiblingId = "RightSiblingId";

	/** Data definition key and KVP key for the {@link #childId}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_ChildId = "ChildId";

	/** Data definition key and KVP key for the {@link #clsid}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_CLSID = "CLSID";

	/** Data definition key and KVP key for the state bits, which is not stored directly as a member variable but could be retrieved from {@link dc}
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_StateBits = "StateBits";

	/** Data definition key and KVP key for the {@link #creationTime}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_CreationTime = "CreationTime";

	/** Data definition key and KVP key for the {@link #modifiedTime}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_ModifiedTime = "ModifiedTime";

	/** Date definition key and KVP key for the {@link #startingSectorLocation}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_StartingSectorLocation = "StartingSectorLocation";

	/** Data definition key and KVP key for the {@link #streamSize}. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_StreamSize = "StreamSize";

	/** KVP key for the property name. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*/
	private static String nm_PropertyName = "PropertyName";

	/** KVP key for the property tag. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*/
	private static String nm_PropertyTag = "PropertyTag";

	/** Data definition key and KVP key for the property type. The intention is that client applications will use this to look up a localized description if needed.
	*	@see #data
	*	@see #keys
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static String nm_PropertyType = "PropertyType";

	/** The data definition describing each directory entry.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*	@see DataContainer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private static final DataDefinition[] fields = {
		new DataDefinition(nm_DirectoryEntryName, new DataType.UnicodeString(64), true),
		new DataDefinition(nm_DirectoryEntryNameLength, DataType.integer16Reader, true),
		new DataDefinition(nm_ObjectType, DataType.integer8Reader, true),
		new DataDefinition(nm_ColorFlag, DataType.integer8Reader, true),
		new DataDefinition(nm_LeftSiblingId, DataType.integer32Reader, true),
		new DataDefinition(nm_RightSiblingId, DataType.integer32Reader, true),
		new DataDefinition(nm_ChildId, DataType.integer32Reader, true),
		new DataDefinition(nm_CLSID, DataType.classIdReader, true),
		new DataDefinition(nm_StateBits, DataType.integer32Reader, true),
		new DataDefinition(nm_CreationTime, DataType.timeReader, true),
		new DataDefinition(nm_ModifiedTime, DataType.timeReader, true),
		new DataDefinition(nm_StartingSectorLocation, DataType.integer32Reader, true),
		new DataDefinition(nm_StreamSize, DataType.integer64Reader, true)
	};

	/** Size of the directory entry */
	static final int SIZE = DataDefinition.size(fields);

	/** The Directory Entry Name (64 bytes)
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final String directoryEntryName;

	/** The Directory Entry Name Length (2 bytes)
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final int directoryEntryPosition;

	/** The Object Type (1 byte). See also {@link ObjectType}
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final ObjectType objectType;

	/** The index of the left sibling, if any (4 bytes). {@link Sector#FREESECT} if there is no left sibling.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final int leftSiblingId;

	/** The index of the right sibling, if any (4 bytes). {@link Sector#FREESECT} if there is no right sibling.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final int rightSiblingId;

	/** The index of a child node, if there are any. The other children must be found by traversing the left and right siblings. {@link Sector#FREESECT} if there are no child nodes.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final int childId;

	/** The object GUID.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private final GUID clsid;

	/** The creation time of the storage object, or all 0's if the creation time was not recorded
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private final java.util.Date creationTime;

	/** The modification time of the storage object, or all 0's if the creation time was not recorded
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	private final java.util.Date modifiedTime;

	/** The first sector of a stream object, or, for the root storage entry only, the first sector of the mini stream.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final int startingSectorLocation;

	/** The size of the stream.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	final long streamSize;

	/** The data repository (preserved after constructor since we don't read everything from it that we might want to display). */
	private final DataContainer dc;

	/** Base class constructor. Set member variables.
	*	@param	directoryEntryName	The name of the directory entry
	*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
	*	@param	objectType		The {@link ObjectType} of the entry
	*	@param	leftSiblingId		The index of the entry's left sibling
	*	@param	rightSiblingId		The index of the entry's right sibling
	*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
	*	@param	clsid			The {@link GUID} for the entry
	*	@param	creationTime		The entry's creation time
	*	@param	modifiedTime		The time of the entry's latest modification
	*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
	*	@param	streamSize		The size of the data
	*	@param	dc			The data container all the information was read from
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-OXCFB Section 2.6.1: Compound File Directory Entry</a>
	*/
	@SuppressWarnings("PMD.ExcessiveParameterList")
	private DirectoryEntry(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
	{
		this.directoryEntryName = directoryEntryName;
		this.directoryEntryPosition = directoryEntryPosition;
		this.objectType = objectType;
		this.leftSiblingId = leftSiblingId;
		this.rightSiblingId = rightSiblingId;
		this.childId = childId;
		this.clsid = clsid;
		this.creationTime = creationTime;
		this.modifiedTime = modifiedTime;
		this.startingSectorLocation = startingSectorLocation;
		this.streamSize = streamSize;
		this.dc = dc;
	}

	/** Get the properties from a Properties object as a HashMap indexed by the property tag.
	*	@param	data	The bytes in the entry
	*	@param	parent	The mapping of child nodes to their parents
	*	@param	namedProperties	The file's named properties object
	*	@return	An empty HashMap for base class
	*/
	java.util.Map<Integer, Property> propertiesAsHashMap(byte[] data, final DirectoryEntry parent, NamedProperties namedProperties)
	{
		return new java.util.HashMap<Integer, Property>();
	}

	/** Get the properties from a Properties object as an ArrayList, preserving the order in which they were read.
	*	@param	data	The bytes in the entry
	*	@param	parent	The mapping of child nodes to their parents
	*	@param	namedProperties	The file's named properties object
	*	@return	An empty ArrayList
	*/
	java.util.List<Property> propertiesAsList(byte[] data, final DirectoryEntry parent, NamedProperties namedProperties)
	{
		return new java.util.ArrayList<Property>();
	}

	/** Get the Property header information. The header is different for children of the Root, included emails, and Recipient/Attachment objects.
	*	@param	data	The bytes in the Properties object.
	*	@return	A KVPArray of the properties header data. For most classes this is empty.
	*/
	KVPArray<String, Integer> getChildPropertiesHeader(byte[] data)
	{
		return new KVPArray<String, Integer>();
	}

	/** Get the size of the Property header information. The size of the header is 32 for the Properties
	*   storage under Root, 24 for embedded  message storages, and 8 for Attachment and Recipient objects.
	*	@return	The size of the Properties header
	*/
	int getChildPropertiesHeaderSize()
	{
		return 0;
	}

	/** Get the entry's contents, if any.
	*	@param	mbb	The ByteBuffer to read the data from
	*	@param	header	The file's Header object
	*	@param	fat	The file's FAT
	*	@param	miniFAT	The file's Mini FAT
	*	@return	The bytes in the entry.
	*/
	byte[] getContent(java.nio.MappedByteBuffer mbb, Header header, FAT fat, MiniFAT miniFAT)
	{
		if (streamSize == 0)
			return null;
		if (header.isInMiniStream(streamSize))
			return miniFAT.read(startingSectorLocation, streamSize, mbb);
		return fat.read(startingSectorLocation, streamSize, mbb, header);
	}

	/** Return a String representation of the data bytes
	*	@param	data	The data to return a text representation of
	*	@return	A text representation of the data, or "Empty" if data is null. The representation is either Unicode or a string of bytes
	*/
	String getDataAsText(byte[] data)
	{
		if (data == null)
			return "Empty";

		if ((getPropertyTag() & DataType.PROPERTY_TYPE_MASK) == DataType.STRING)
			return DataType.createString(data);

		return ByteUtil.createHexByteString(data);
	}

	/** Get the header data for primary msg object (if it is a child of the root object) or for an embedded message
	*	@param	data	The contents of the Properties stream
	*	@return	An array of KVPs contianing the header information
	*/
	private KVPArray<String, Integer> getMessageProperties(byte[] data)
	{
		java.nio.ByteBuffer headerStream = java.nio.ByteBuffer.wrap(data);
		headerStream.order(java.nio.ByteOrder.LITTLE_ENDIAN);

		KVPArray<String, Integer> header = new KVPArray<String, Integer>();
		// Bytes 1-8: reserved
		headerStream.position(8);
		header.add(NEXT_RECIPIENT_ID, headerStream.getInt());
		header.add(NEXT_ATTACHMENT_ID, headerStream.getInt());
		header.add(RECIPIENT_COUNT, headerStream.getInt());
		header.add(ATTACHMENT_COUNT, headerStream.getInt());

		return header;
	}

	/** Get the property tag (ID and type code), if any.
	*	@return	The property tag. The default implementation, suitable for all classes except Substorage, returns
	*		{@link #NO_PROPERTY_TAG}, a sentinel value indicating that there is no property related to this object type.
	*/
	int getPropertyTag()
	{
		return NO_PROPERTY_TAG;
	}

	/** Return the property type, if any.
	*	@return	The property type. The default implementatation, suitable for all classes except Substorage, returns n/a
	*/
	String getPropertyType()
	{
		return "n/a";
	}

	/** Return a String for this entry
	*	@return	A String representation of this entry.
	*/
	@Override
	@SuppressWarnings("JavaUtilDate") // Relaticely safe use of java.util.Date
	public String toString()
	{
		return String.format("name %s%n" +
			"starting sector %d (0x%08x) size %d%n" +
			"object type %s%n" +
			"left sibling 0x%08x right sibling 0x%08x child 0x%08x%n" +
			"class ID %s%n" +
			"created %s modified %s%n",
		directoryEntryName,
		startingSectorLocation, startingSectorLocation, streamSize,
		objectType.toString(),
		leftSiblingId, rightSiblingId, childId,
		clsid.toString(),
		creationTime.toString(), modifiedTime.toString()
		);
	}

	/** Attachment object storage
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/8590d60d-4173-4ca8-9cb2-190aae006fbd">MS-OXMSG Section 2.2.2: Attachment Object Storage</a>
	*/
	private static class Attachment extends DirectoryEntry {

		/** Construct an Attachment directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private Attachment(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}

		/** Get the size of the Property header information for Attachment objects.
		*	@return	The size of the Properties header for Attachment object
		*/
		@Override
		int getChildPropertiesHeaderSize()
		{
			return 8;
		}
	}

	/** NamedPropertiesMapping entries have no siblings and no storage and null Class IDs.i
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/193c169b-0628-4392-aa51-83009be7d71f">MS-OXMSG Section 2.2.3: Named Property Mapping Storage</a>
	*/
	private static class NamedPropertiesMapping extends DirectoryEntry {

		/** Construct a NamedPropertiesMapping directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private NamedPropertiesMapping(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}
	}

	/** Properties have no siblings or children; Class ID and dates are always null, and Object Type is always Stream Object.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/20c1125f-043d-42d9-b1dc-cb9b7e5198ef">MS-OXMSG Section 2.4: Property Stream</a>
	*/
	private static class Properties extends DirectoryEntry {

		/** Construct a Properties directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private Properties(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}

		/** Get the properties from a Properties object as a HashMap indexed by the property tag.
		*	@param	data	The bytes in the entry
		*	@param	parent	The mapping of child nodes to their parents
		*	@param	namedProperties	The file's named properties object
		*	@return	A HashMap of Property objects containing the property data defined in the Properties entry.
		*/
		@Override
		java.util.Map<Integer, Property> propertiesAsHashMap(byte[] data, final DirectoryEntry parent, NamedProperties namedProperties)
		{
			java.util.Map<Integer, Property> properties = super.propertiesAsHashMap(data, parent, namedProperties);

			java.nio.ByteBuffer propertyStream = java.nio.ByteBuffer.wrap(data);
			propertyStream.order(java.nio.ByteOrder.LITTLE_ENDIAN);
			propertyStream.position(parent.getChildPropertiesHeaderSize());
			while (propertyStream.hasRemaining()){
				Property p = Property.factory(propertyStream, namedProperties, parent);
				if (p != null)
					properties.put(p.propertyTag, p);
			}
			return properties;
		}

		/** Get the properties from a Properties object as an ArrayList.
		*	@param	data	The bytes in the entry
		*	@param	parent	The mapping of child nodes to their parents
		*	@param	namedProperties	The file's named properties object
		*	@return	An array of Property objects, one for each of the properties listed in the Properties entry
		*/
		@Override
		java.util.List<Property> propertiesAsList(byte[] data, final DirectoryEntry parent, NamedProperties namedProperties)
		{
			java.util.List<Property> properties = super.propertiesAsList(data, parent, namedProperties);

			java.nio.ByteBuffer propertyStream = java.nio.ByteBuffer.wrap(data);
			propertyStream.order(java.nio.ByteOrder.LITTLE_ENDIAN);
			propertyStream.position(parent.getChildPropertiesHeaderSize());
			while (propertyStream.hasRemaining()){
				Property p = Property.factory(propertyStream, namedProperties, parent);
				if (p != null)
					properties.add(p);
			}
			return properties;
		}
	}

	/** Recipient Object Storage
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/38a5cb3c-4454-48ba-b113-6de75321b67f">MS-OXMSG Section 2.2.1: Recipient Object Storage</a>
	*/
	private static class Recipient extends DirectoryEntry {

		/** Construct a Recipient directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private Recipient(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}

		/** Get the size of the Property header information for Recipient objects.
		*	@return	The size of the Properties header for Recipient object
		*/
		@Override
		int getChildPropertiesHeaderSize()
		{
			return 8;
		}
	}

	/** The root entry
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/026fde6e-143d-41bf-a7da-c08b2130d50e">MS-CFB Section 2.6.2: Root Directory Entry</a>
	*/
	private static class RootEntry extends DirectoryEntry {

		/** Construct a RootEntry directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private RootEntry(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}

		/** Get the Property header information. The header is different for children of the Root, included emails, and Recipient/Attachment objects.
		*	@param	data	The bytes in the Properties object.
		*	@return	A KVPArray of the properties header data
		*/
		@Override
		KVPArray<String, Integer> getChildPropertiesHeader(byte[] data)
		{
			return super.getMessageProperties(data);
		}

		/** Get the size of the Property header information for the Root object.
		*	@return	The size of the Properties header for the root object
		*/
		@Override
		int getChildPropertiesHeaderSize()
		{
			return 32;
		}
	}

	/** A storage stream, containing a property value for a variable-length or large fixed-width (&gt; 8 bytes) property
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/08185828-e9e9-4ef2-bcd2-f6e69c00891b">MS-OXMSG Section 2.1.3: Variable Length Properties</a>
	*/
	private static class Substorage extends DirectoryEntry {

		/** Known data type names stored by type ID */
		private static final java.util.HashMap<Integer, String> dataTypeNames = new java.util.HashMap<Integer, String>();
		static {
			dataTypeNames.put(DataType.STRING, "String");
			dataTypeNames.put(DataType.BINARY, "Binary");
		}

		/** The property Tag
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/41bd4284-1064-4464-bcfa-10de3356daff">MS-OXMSG Section 2.1.1: Properties of a .msg File &amp; ff</a>
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcmsg/e6c44513-5f40-49d0-8611-99aa15e2817b">MS-OXMSG Section 2.2.1: Message Object Properties &amp; ff</a>
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcmsg/386ec4e1-87e5-4f7e-96b1-7dfc1cd23fc3">MS-OXMSG Section 2.2.2: Attachment Object Properties</a>
		*/
		int propertyTag;

		/** Construct a Substorage directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private Substorage(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, String propertyTag, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
			this.propertyTag = (int)Long.parseLong(propertyTag, 16);
		}

		/** Get the Property header information. The header is different for children of the Root, included emails, and Recipient/Attachment objects.
		*	@param	data	The bytes in the Properties object.
		*	@return	A KVPArray of the properties header data
		*/
		@Override
		KVPArray<String, Integer> getChildPropertiesHeader(byte[] data)
		{
			return super.getMessageProperties(data);
		}

		/** Get the size of the Property header information for embedded messages
		*	@return	The size of the Properties header for embedded messages.
		*	@see Substorage#getChildPropertiesHeader
		*/
		@Override
		int getChildPropertiesHeaderSize()
		{
			return 28;
		}

		/** Get the property tag (ID and type code), if any.
		*	@return	The property tag
		*/
		@Override
		int getPropertyTag()
		{
			return propertyTag;
		}

		/** Return a description of the data type for this object.
		*	@return	A description of the property type for display by client applications"
		*/
		@Override
		String getPropertyType()
		{
			int propertyType = propertyTag & DataType.PROPERTY_TYPE_MASK;
			if (!dataTypeNames.containsKey(propertyType))
				return "Unknown property type";

			return dataTypeNames.get(propertyType);
		}
	}

	/** An unallocated entry
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/b37413bb-f3ef-4adc-b18e-29bddd62c26e">MS-CFG Section 2.6.3: Other Directory Entries</a>
	*/
	private static class Unallocated extends DirectoryEntry {

		/** Construct an Unallocated directory entry from the directory entry data
		*	@param	directoryEntryName	The name of the directory entry
		*	@param	directoryEntryPosition	The byte offset into the file of the directory entry
		*	@param	objectType		The {@link ObjectType} of the entry
		*	@param	leftSiblingId		The index of the entry's left sibling
		*	@param	rightSiblingId		The index of the entry's right sibling
		*	@param	childId			The index of the entry's child (only one is referenced; siblings are found via left and right sibling IDs
		*	@param	clsid			The {@link GUID} for the entry
		*	@param	creationTime		The entry's creation time
		*	@param	modifiedTime		The time of the entry's latest modification
		*	@param	startingSectorLocation	The starting sector (or mini sector) of the file (depending on the streanSize)
		*	@param	streamSize		The size of the data
		*	@param	dc			The data container all the information was read from
		*/
		private Unallocated(String directoryEntryName, int directoryEntryPosition, ObjectType objectType, int leftSiblingId, int rightSiblingId, int childId, GUID clsid, java.util.Date creationTime, java.util.Date modifiedTime, int startingSectorLocation, long streamSize, DataContainer dc)
		{
			super(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}
	}

	/** Make full directory information data available to client applications
	*	@param	namedProperties	The file's named properties object
	*	@param	parents		The mapping of child nodes to their parents
	*	@return	An array of key-value pairs consisting of a description of the data and the data itself
	*/
	@SuppressWarnings("JavaUtilDate") // Relaticely safe use of java.util.Date
	KVPArray<String, String> data(final NamedProperties namedProperties, final java.util.Map<DirectoryEntry, DirectoryEntry> parents)
	{
		/* See MS-OXMSG Section 2.2.3: Named Property Storage */
		final int GUID_STREAM_PROPERTY_TAG = 0x00020102;
		final int ENTRY_STREAM_PROPERTY_TAG = 0x00030102;
		final int STRING_STREAM_PROPERTY_TAG = 0x00040102;

		KVPArray<String, String> l = new KVPArray<String, String>();

		int propertyTag = getPropertyTag();
		boolean hasPropertyTag = true;
		String propertyName;
		if (directoryEntryName.equals(ROOT_ENTRY)){
			hasPropertyTag = false;
			propertyName = "Root Entry";
		} else if (directoryEntryName.equals(NAMEID)){
			hasPropertyTag = false;
			propertyName = "Named Property Mapping Storage";
		} else if (parents.get(this).directoryEntryName.equals(NAMEID)){
			if (propertyTag == GUID_STREAM_PROPERTY_TAG) {
				propertyName = "GUID Stream";
			} else if (propertyTag == ENTRY_STREAM_PROPERTY_TAG) {
				propertyName = "Entry Stream";
			} else if (propertyTag == STRING_STREAM_PROPERTY_TAG) {
				propertyName = "String Stream";
			} else {
				propertyName = "Property Name to Property ID Mapping Stream";
			}
		} else if (propertyTag == NO_PROPERTY_TAG) {
			hasPropertyTag= false;
			propertyName = "n/a";
		} else if (PropertyTags.tags.keySet().contains(propertyTag)) {
			propertyName = PropertyTags.tags.get(propertyTag);
		} else if ((propertyTag & 0x80000000) != 0) {
			int propertyIndex = (propertyTag >> 16) & 0x7fff;
			propertyName = namedProperties.getPropertyName(propertyIndex);
		} else {
			propertyName = String.format("Unknown property 0x%08x", propertyTag);
		}
		l.add(nm_PropertyName, propertyName);

		if (hasPropertyTag){
			l.add(nm_PropertyTag, String.format("0x%08x", propertyTag));
		} else {
			l.add(nm_PropertyTag, "n/a");
		}
		l.add(nm_PropertyType, getPropertyType());
		l.add(nm_DirectoryEntryName, directoryEntryName);
		l.add(nm_DirectoryEntryNameLength, Short.toString((Short)dc.get(nm_DirectoryEntryNameLength)));
		l.add(nm_ObjectType, objectType.toString());
		l.add(nm_ColorFlag, Byte.toString((Byte)dc.get(nm_ColorFlag)));
		l.add(nm_LeftSiblingId, Integer.toString(leftSiblingId));
		l.add(nm_RightSiblingId, Integer.toString(rightSiblingId));
		l.add(nm_ChildId, Integer.toString(childId));
		l.add(nm_CLSID, clsid.toString());
		l.add(nm_StateBits, Integer.toString((Integer)dc.get(nm_StateBits)));
		l.add(nm_CreationTime, creationTime.toString());
		l.add(nm_ModifiedTime, modifiedTime.toString());
		l.add(nm_StartingSectorLocation, Integer.toString(startingSectorLocation));
		l.add(nm_StreamSize, Long.toString(streamSize));
		return l;
	}

	/** Create a directory entry of the required type based on the directory entry name.
	*	@param	byteBuffer	The data stream for the msg file
	*	@param	cd		The holder for information used to build the {link @Directory#Directory Directory constructor} after all entries have been read.
	*	@return	The DirectoryEntry object read from the byteBuffer
	*	@throws	UnknownStorageTypeException	The object type is not one of UNKNOWN, STORAGE, STREAM, or ROOT_STORAGE.
	*	@throws	java.io.IOException	If the file could not be read
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	static DirectoryEntry factory(java.nio.ByteBuffer byteBuffer, Directory.ConstructorData cd)
	throws
		UnknownStorageTypeException,
		java.io.IOException
	{
		DataContainer dc = new DataContainer();
		int directoryEntryPosition = byteBuffer.position();
		dc.read(byteBuffer, fields);

		/* The name length returned includes the terminating null. */
		int directoryEntryNameLength = (Short)dc.get(nm_DirectoryEntryNameLength) - 1;
		String directoryEntryName = ((String)dc.get(nm_DirectoryEntryName)).substring(0, directoryEntryNameLength/2);
		ObjectType objectType = ObjectType.valueOf((Byte)dc.get(nm_ObjectType));
		int leftSiblingId = (Integer)dc.get(nm_LeftSiblingId);
		int rightSiblingId = (Integer)dc.get(nm_RightSiblingId);
		int childId = (Integer)dc.get(nm_ChildId);
		GUID clsid = (GUID)dc.get(nm_CLSID);
		java.util.Date creationTime = (java.util.Date)dc.get(nm_CreationTime);
		java.util.Date modifiedTime = (java.util.Date)dc.get(nm_ModifiedTime);
		int startingSectorLocation = (Integer)dc.get(nm_StartingSectorLocation);
		long streamSize = (Long)dc.get(nm_StreamSize);

		java.util.regex.Matcher matcher;
		if (ROOT_ENTRY.equals(directoryEntryName)){
			return new RootEntry(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		} else if (NAMEID.equals(directoryEntryName)){
			DirectoryEntry de = new NamedPropertiesMapping(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
			cd.namedPropertiesMappingEntry = de;
			return de;
		} else if ((matcher = STRING_STREAM_PATTERN.matcher(directoryEntryName)).matches()){
			return new Substorage(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, matcher.group(1), dc);
		} else if (PROPERTIES.equals(directoryEntryName)){
			DirectoryEntry de = new Properties(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
			cd.propertyEntries.add(de);
			return de;
		} else if (RECIP_PATTERN.matcher(directoryEntryName).matches()){
			DirectoryEntry de = new Recipient(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
			cd.recipientEntries.add(de);
			return de;
		} else if (ATTACH_PATTERN.matcher(directoryEntryName).matches()){
			DirectoryEntry de = new Attachment(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
			cd.attachmentEntries.add(de);
			return de;
		} else if (UNALLOCATED.equals(directoryEntryName)){
			return new Unallocated(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		} else {
			System.out.println("Unrecognized directory entry name or template " + directoryEntryName);
			return new DirectoryEntry(directoryEntryName, directoryEntryPosition, objectType, leftSiblingId, rightSiblingId, childId, clsid, creationTime, modifiedTime, startingSectorLocation, streamSize, dc);
		}
	}

	/** Provide keys (with empty values) to allow tables to be set up with the correct length before we have any data.
	*	@return	An array of key-value pairs consisting of a description of the data and an empty string.
	*/
	static KVPArray<String, String> keys()
	{
		KVPArray<String, String> l = new KVPArray<String, String>();
		l.add(nm_PropertyName, "");
		l.add(nm_PropertyTag, "");
		l.add(nm_PropertyType, "");
		l.add(nm_DirectoryEntryName, "");
		l.add(nm_DirectoryEntryNameLength, "");
		l.add(nm_ObjectType, "");
		l.add(nm_ColorFlag, "");
		l.add(nm_LeftSiblingId, "");
		l.add(nm_RightSiblingId, "");
		l.add(nm_ChildId, "");
		l.add(nm_CLSID, "");
		l.add(nm_StateBits, "");
		l.add(nm_CreationTime, "");
		l.add(nm_ModifiedTime, "");
		l.add(nm_StartingSectorLocation, "");
		l.add(nm_StreamSize, "");
		return l;
	}

	/** Test this class by printing out the directory entries
	*	@param	args	The msg file(s) to display the directory entries of
	*/
	@SuppressWarnings("PMD.DoNotCallSystemExit")
	public static void main(String[] args)
	{
		if (args.length == 0) {
			System.out.println("use:\n\tjava io.github.jmcleodfoss.mst.Directory msg-file [msg-file ...]");
			System.exit(1);
		}

		for (String a: args) {
			System.out.println(a);
			try {
				java.io.File file = new java.io.File(a);
				java.io.FileInputStream stream = new java.io.FileInputStream(file);
				try {
					java.nio.channels.FileChannel fc = stream.getChannel();
					java.nio.MappedByteBuffer mbb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fc.size());
					mbb.order(java.nio.ByteOrder.LITTLE_ENDIAN);

					Header header = new Header(mbb, fc.size());
					DIFAT difat = new DIFAT(mbb, header);
					FAT fat = new FAT(mbb, header, difat);
					Directory directory = new Directory(mbb, header, fat);
					MiniFAT miniFAT = new MiniFAT(mbb, header, fat, directory);

					java.util.Iterator<DirectoryEntry> iterator = directory.entries.iterator();
					int i = 0;
					while (iterator.hasNext()){
						DirectoryEntry de = iterator.next();
						System.out.printf("0x%02x: left 0x%08x right 0x%08x child 0x%08x %s%n",
							i, de.leftSiblingId, de.rightSiblingId, de.childId, de.objectType.toString());
						byte[] data = de.getContent(mbb, header, fat, miniFAT);
						if (data != null)
							System.out.println(de.getDataAsText(data));
						System.out.println();
						++i;
					}
				} catch (final java.io.IOException e) {
					System.out.printf("There was a problem reading from file %s%n", a);
				} catch (final NotCFBFileException e) {
					e.printStackTrace(System.out);
				} catch (final UnknownStorageTypeException e) {
					e.printStackTrace(System.out);
				} finally {
					try {
						stream.close();
					} catch (final java.io.IOException e) {
						System.out.printf("There was a problem closing file %s%n", a);
					}
				}
			} catch (final java.io.FileNotFoundException e) {
				System.out.printf("File %s not found%n", a);
			}
		}
	}
}
