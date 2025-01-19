package com.alibaba.cloud.ai.reader.email.msg;

/** An entry from the Entry Stream, used in the entry stream of the property ID to property name mapping, and the property name to property ID mapping streams.
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/81159dd0-649e-4491-b216-877008b23f65">MS-OXMSG Section 2.2.3.1.2: Entry Stream</a>
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/a84c08f9-c93b-4620-9c96-5314c6fa3ccc">MS-OXMSG Section 2.2.3.2.4: Obtaining Stream Data</a>
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
*/
class EntryStreamEntry
{
	/** The type of object.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	static enum PropertyType {
		/** A numerical named property */
		NUMERICAL_NAMED_PROPERTY,

		/** A numerical named property
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/14dd4e27-58e1-4d6c-b4c2-7a1fd19d819c">MS-OXMSG Section 2.2.3.1.3: String Stream</a>
		*/
		STRING_NAMED_PROPERTY
	};

	/** The name identifier (for numerical named properties) or the string offset (for string named properties
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	final int nameIdentifierOrStringOffset;

	/** The sequentially increasing, 0-based property index
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	final short propertyIndex;

	/** The GUID index.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	final short guidIndex;

	/** The property type
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	final PropertyType propertyType;

	/** Create a EntryStreamEntry from a raw byte stream
	*	@param	rawData	The byte stream to read this entry from.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	EntryStreamEntry(byte[] rawData)
	{
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(rawData);
		bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);

		nameIdentifierOrStringOffset = bb.getInt();
		int temp = bb.getInt();
		propertyIndex = (short)(temp >>> 16);
		guidIndex = (short)((temp & 0xffff) >>> 1);
		propertyType = ((temp & 0x01) != 0) ? PropertyType.STRING_NAMED_PROPERTY : PropertyType.NUMERICAL_NAMED_PROPERTY;
	}

	/** Get a String representation of this object.
	*	@return	A string representing this object
	*/
	@Override
	public String toString()
	{
		String pt;
		if (propertyType == PropertyType.NUMERICAL_NAMED_PROPERTY)
			pt = "Numerical named property";
		else if (propertyType == PropertyType.STRING_NAMED_PROPERTY)
			pt = "String named property";
		else
			pt = "Unknown named property";
		return String.format("0x%08x property index 0x%04x GUID index 0x%04x %s",
			nameIdentifierOrStringOffset, propertyIndex, guidIndex, pt);
	}
}
