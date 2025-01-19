package com.alibaba.cloud.ai.reader.email.msg;

/** Expose data from EntryStreamEntry objects to client applications
*	@see EntryStreamEntry
*/
public class EntryStreamEntryData {

	/** The name identifier (for numerical named properties) or the string offset (for string named properties
	*	@see EntryStreamEntry#nameIdentifierOrStringOffset
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	public final int nameIdentifierOrStringOffset;

	/** The sequentially increasing, 0-based property index
	*	@see EntryStreamEntry#propertyIndex
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	public final short propertyIndex;

	/** The GUID index.
	*	@see EntryStreamEntry#guidIndex
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxmsg/abdc1a7d-5a44-4bb2-aa35-b241e4a3f0d9">MS-OXMSG Section 2.2.3.1.2.1: Index and Kind Information</a>
	*/
	public final short guidIndex;

	/** Create an EntryStreamEntryData object from the corresponding entry.
	*	@param	entry	The EntryStreamEntry object to expose
	*	@see EntryStreamEntry
	*/
	EntryStreamEntryData(EntryStreamEntry entry)
	{
		this.nameIdentifierOrStringOffset = entry.nameIdentifierOrStringOffset;
		this.propertyIndex = entry.propertyIndex;
		this.guidIndex = entry.guidIndex;
	}
}
