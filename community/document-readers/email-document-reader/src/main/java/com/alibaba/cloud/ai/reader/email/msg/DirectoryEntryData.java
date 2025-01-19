package com.alibaba.cloud.ai.reader.email.msg;

/** This class is used to publish directory entry info to a client application
*	@see DirectoryEntry
* 	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
*/
public class DirectoryEntryData {

	/** The directory entry index */
	final DirectoryEntry entry;

	/** The directory entry name
	*	@see DirectoryEntry#directoryEntryName
	* 	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/a94d7445-c4be-49cd-b6b9-2f4abc663817">MS-CFB Section 2.6: Compound File Directory Sectors</a>
	*/
	public final String name;

	/** The Property Tag, if any. */
	public final int propertyTag;

	/** All directory information for this entry
	*	@see DirectoryEntry#nm_DirectoryEntryName
	*	@see DirectoryEntry#nm_DirectoryEntryNameLength
	*	@see DirectoryEntry#nm_ObjectType
	*	@see DirectoryEntry#nm_ColorFlag
	*	@see DirectoryEntry#nm_LeftSiblingId
	*	@see DirectoryEntry#nm_RightSiblingId
	*	@see DirectoryEntry#nm_ChildId
	*	@see DirectoryEntry#nm_CLSID
	*	@see DirectoryEntry#nm_StateBits
	*	@see DirectoryEntry#nm_CreationTime
	*	@see DirectoryEntry#nm_ModifiedTime
	*	@see DirectoryEntry#nm_StartingSectorLocation
	*	@see DirectoryEntry#nm_StreamSize
	*	@see DirectoryEntry#nm_PropertyName
	*	@see DirectoryEntry#nm_PropertyType
	*	@see DirectoryEntry#data
	*/
	public final KVPArray<String, String> kvps;

	/** Create the external data object for the given directory entry
	*	@param	de		The directory entry to shadow
	*	@param	directory	The Directory object the entry is from
	*	@param	namedProperties	The file's NamedProperties object to look up non-standard properties
	*	@see DirectoryEntry
	*/
	DirectoryEntryData(DirectoryEntry de, Directory directory, NamedProperties namedProperties)
	{
		entry = de;
		name = de.directoryEntryName;
		propertyTag = de.getPropertyTag();
		kvps = de.data(namedProperties, directory.parents);
	}

	/** Create an iterator through this entry's children
	* 	@param	directory	The directory this entry is in
	*	@param	namedProperties	The file's named properties list
	*	@return	An iterator through the entry's children as DirectoryEntryData objects
	*/
	java.util.Iterator<DirectoryEntryData> childIterator(Directory directory, NamedProperties namedProperties)
	{
		return new DirectoryEntryDataIterator(directory.getChildren(entry).iterator(), directory, namedProperties);
	}

	/** Is this entry a text property
	*	@return	True if the property type is string, false otherwise.
	*/
	public boolean isText()
	{
		return (propertyTag & DataType.PROPERTY_TYPE_MASK) == DataType.STRING;
	}

	/** Create a string representing this directory entry
	*	@return	The name of the entry
	*/
	@Override
	public String toString()
	{
		return name;
	}
}
