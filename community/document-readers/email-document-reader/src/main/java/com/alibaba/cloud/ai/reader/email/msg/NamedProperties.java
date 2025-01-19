package com.alibaba.cloud.ai.reader.email.msg;

/** The named properties in a msg file
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/193c169b-0628-4392-aa51-83009be7d71f">MS-OXMSG Section 2.2.3: Named Property Mapping Storage</a>
*/
class NamedProperties
{
	/** The entry name for the GUID stream
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/e910b8f0-ab70-410b-bb3a-0fa236a55bfb">MS-OXMSG Section 2.2.3.1.1: GUID Stream</a>
	*/
	private static final String GUID_STREAM_NAME = "__substg1.0_00020102";

	/** The entry name for the entry stream
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/81159dd0-649e-4491-b216-877008b23f65">MS-OXMSG Section 2.2.3.1.2: Entry Stream</a>
	*/
	private static final String ENTRY_STREAM_NAME = "__substg1.0_00030102";

	/** The entry name for the string stream
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/14dd4e27-58e1-4d6c-b4c2-7a1fd19d819c">MS=OXMSG Section 2.2.3.1.3: String Stream</a>
	*/
	private static final String STRING_STREAM_NAME = "__substg1.0_00040102";

	/** The list of GUIDs */
	GUID[] guids;

	/** The list of entries from the Entry Stream */
	private EntryStreamEntry[] entries;

	/** The number of numeric named property entries */
	private int numNumericalNamedProperties;

	/** The list of strings in the string stream, stored by stream offset */
	java.util.HashMap<Integer, String> stringsByOffset;

	/** The list of strings in the string stream, in order found. */
	java.util.ArrayList<String> strings;

	/** The property ID to name mapping array */
	private EntryStreamEntry[] propertyNameMappings;

	/** Read in the named properties information
	*	@param	mbb	The CFB file to read from
	*	@param	header	The CFB header information
	*	@param	fat	The file allocation table
	*	@param	directory	The directory
	*	@param	miniFAT	The mini sector file allocation table.
	*/
	NamedProperties(java.nio.MappedByteBuffer mbb, Header header, FAT fat, Directory directory, MiniFAT miniFAT)
	{
		java.util.List<DirectoryEntry> children = directory.getChildren(directory.namedPropertiesMappingEntry);
		java.util.Iterator<DirectoryEntry> iter = children.iterator();

		// After accounting for the GUID, Entry, and String streams, the
		// remaining entries are for the property name / property ID mappings.
		int numPropertyNameMappings = children.size() - 3;
		propertyNameMappings = new EntryStreamEntry[numPropertyNameMappings];
		int pnmIndex = 0;

		while(iter.hasNext()){
			DirectoryEntry de = iter.next();

			// Read in all the data at once. This is overkill for the simple
			// case where all the data fits into one mini sector, but makes
			// it much easier to deal with an Entry stream which spans multiple
			// mini and non-mini sectors.
			byte[] data = de.getContent(mbb, header, fat, miniFAT);

			if (GUID_STREAM_NAME.equals(de.directoryEntryName)){
				setGUIDS(de, data);
			} else if (ENTRY_STREAM_NAME.equals(de.directoryEntryName)){
				setEntries(de, data);
			} else if (STRING_STREAM_NAME.equals(de.directoryEntryName)){
				setStrings(de, data);
			} else {
				propertyNameMappings[pnmIndex] = new EntryStreamEntry(data);
				if (propertyNameMappings[pnmIndex].propertyType == EntryStreamEntry.PropertyType.NUMERICAL_NAMED_PROPERTY)
					++numNumericalNamedProperties;
				++pnmIndex;
			}
		}
	}

	/** Get the numerical or string entry contents of the Entry Stream
	*	@param	propertyType	The type of entry to return.
	*	@return	An array of EntryStreamEntryData objects for the numerical named properties.
	*/
	java.util.List<EntryStreamEntryData> getEntryStreamEntries(EntryStreamEntry.PropertyType propertyType)
	{
		java.util.List<EntryStreamEntryData> npEntries = new java.util.ArrayList<EntryStreamEntryData>();
		for (EntryStreamEntry entry: entries){
			if (entry.propertyType != propertyType)
				continue;
			npEntries.add(new EntryStreamEntryData(entry));
		}
		return npEntries;
	}

	/** Get the String name for the named given property index
	*	@param	propertyIndex	The 0-based property index
	*	@return	The name of the property if it has a string name,
	*		otherwise a string representation of the GUID
	*/
	String getPropertyName(int propertyIndex)
	{
		if (propertyIndex >= entries.length)
			return String.format("Out of bounds error (%d >= %d", propertyIndex, entries.length);

		if (entries[propertyIndex].propertyType == EntryStreamEntry.PropertyType.STRING_NAMED_PROPERTY)
			return stringsByOffset.get(entries[propertyIndex].nameIdentifierOrStringOffset);

		if (PropertyLIDs.lids.keySet().contains(entries[propertyIndex].nameIdentifierOrStringOffset)) {
			GUID guid = PropertyLIDs.guids.get(entries[propertyIndex].nameIdentifierOrStringOffset);
			if (guid != null && guid.equals(guids[propertyIndex])) {
				return PropertyLIDs.lids.get(entries[propertyIndex].nameIdentifierOrStringOffset);
			}
		}

		return String.format("Not found: 0x%04x (%s)", entries[propertyIndex].nameIdentifierOrStringOffset, indexToGUID(entries[propertyIndex].guidIndex));
	}

	/** Get the Property ID to Property Name Mapping entry for the given index
	*	@param	mappingIndex	The index of the Property Id to Property name Mapping entry to retrieve
	*	@return	A KVPArray object containing information about the requested entry
	*/
	KVPArray<String, String> getPropertyIdToPropertyNameMapping(int mappingIndex)
	{
		KVPArray<String, String> mapping = new KVPArray<String, String>();
		if (mappingIndex < 0 || mappingIndex > propertyNameMappings.length) {
			return mapping;
		}

		if (propertyNameMappings[mappingIndex].propertyType == EntryStreamEntry.PropertyType.NUMERICAL_NAMED_PROPERTY) {
			mapping.add("NameIdentifier", String.format("0x%04x", propertyNameMappings[mappingIndex].nameIdentifierOrStringOffset));
		} else {
			mapping.add("CRC-32 Checksum", String.format("0x%08x", propertyNameMappings[mappingIndex].nameIdentifierOrStringOffset));
		}

		mapping.add("PropertyIndex", Integer.toString(propertyNameMappings[mappingIndex].propertyIndex));
		mapping.add("GUIDIndex", Integer.toString(propertyNameMappings[mappingIndex].guidIndex));
		mapping.add("GUID", indexToGUID(propertyNameMappings[mappingIndex].guidIndex).toString());

		if (propertyNameMappings[mappingIndex].propertyType == EntryStreamEntry.PropertyType.STRING_NAMED_PROPERTY)
			mapping.add("PropertyName", strings.get(propertyNameMappings[mappingIndex].propertyIndex - numNumericalNamedProperties));
		return mapping;
	}

	/** Get the GUID from the GUID index
	*	@param	index	The GUID index
	*	@return	The GUID corresponding to the GUID index
	*/
	private GUID indexToGUID(int index)
	{
		if (index == 1)
			return GUID.PS_MAPI;
		if (index == 2)
			return GUID.PS_PUBLIC_STRINGS;
		return guids[index-3];
	}

	/** Set the entries from the entry stream.
	*	@param	de	The String Stream containing the entries
	*	@param	data	The data for this entrym
	*/
	private void setEntries(DirectoryEntry de, byte[] data)
	{
		int numEntries = (int)de.streamSize / DataType.SIZEOF_LONG;
		entries = new EntryStreamEntry[numEntries];
		for (int i = 0; i < numEntries; ++i)
			entries[i] = new EntryStreamEntry(java.util.Arrays.copyOfRange(data, i*DataType.SIZEOF_LONG, (i+1)*DataType.SIZEOF_LONG));
	}

	/** Set the GUIDs from the GUID stream
	*	@param	de	The String Stream containing the GUIDs.
	*	@param	data	The data for this entrym
	*/
	private void setGUIDS(DirectoryEntry de, byte[] data)
	{
		int numGUIDS = (int)de.streamSize / GUID.SIZE;
		guids = new GUID[numGUIDS];
		for (int i = 0; i < numGUIDS; ++i)
			guids[i] = new GUID(java.util.Arrays.copyOfRange(data, i*GUID.SIZE, (i+1)*GUID.SIZE));
	}

	/** Set the strings from the string stream.
	*	@param	de	The String Stream containing the entries
	*	@param	data	The data for this entry
	*/
	private void setStrings(DirectoryEntry de, byte[] data)
	{
		java.nio.ByteBuffer thisStream = java.nio.ByteBuffer.wrap(data);
		thisStream.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		stringsByOffset = new java.util.HashMap<Integer, String>();
		strings = new java.util.ArrayList<String>();
		int nRemaining = (int)de.streamSize;
		while (nRemaining > 0) {
			// Retrieving UTF-16 characters
			int position = thisStream.position();
			int stringLen = thisStream.getInt();
			nRemaining -= 4;
			byte[] stringData = new byte[stringLen];
			thisStream.get(stringData);
			nRemaining -= stringLen;
			String propertyName = DataType.createString(stringData);
			strings.add(propertyName);
			stringsByOffset.put(position, propertyName);
			for (int i = 0; i < stringLen % 4 && nRemaining > 0; ++i){
				thisStream.get();
				--nRemaining;
			}
		}
	}

	/**	Test this class by printing out the GUID, entries, and strings.
	*	@param	args	The msg file(s) to print out the named properties information for.
	*/
	@SuppressWarnings("PMD.DoNotCallSystemExit")
	public static void main(String[] args)
	{
		if (args.length == 0) {
			System.out.println("use:\n\tjava io.github.jmcleodfoss.mst.NamedProperties msg-file [msg-file ...]");
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
					if (directory.namedPropertiesMappingEntry == null) {
						System.out.printf("%s does not containe a Named Properties Mapping Entry%n", a);
						continue;
					}
					NamedProperties namedPropertiesMapping = new NamedProperties(mbb, header, fat, directory, miniFAT);

					System.out.println("GUID stream");
					for (int i = 0; i < namedPropertiesMapping.guids.length; ++i)
						System.out.println(namedPropertiesMapping.guids[i]);

					System.out.println();
					System.out.println("Entry stream");
					for (int i = 0; i < namedPropertiesMapping.entries.length; ++i)
						System.out.println(namedPropertiesMapping.entries[i]);

					System.out.println();
					System.out.println("String stream");
					java.util.Iterator<String> iter_s = namedPropertiesMapping.strings.iterator();
					while (iter_s.hasNext())
						System.out.println(iter_s.next());
					System.out.println();
					java.util.Iterator<Integer> iter_i = namedPropertiesMapping.stringsByOffset.keySet().iterator();
					while (iter_i.hasNext()){
						int key = iter_i.next();
						System.out.printf("0x%04x: %s%n", key, namedPropertiesMapping.stringsByOffset.get(key));
					}

					System.out.println();
					System.out.println("Entries");
					for (int i = 0; i < namedPropertiesMapping.propertyNameMappings.length; ++i)
						System.out.printf("%s GUID %s%n",
							namedPropertiesMapping.propertyNameMappings[i],
							namedPropertiesMapping.indexToGUID(namedPropertiesMapping.propertyNameMappings[i].guidIndex)
							);
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
