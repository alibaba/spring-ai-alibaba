package com.alibaba.cloud.ai.reader.email.msg;

/** The ObjectType class represents a CFB entry object type; it can be one of { {@link #UNKNOWN}, {@link #STORAGE}, {@link #STREAM}, {@link ROOT_STORAGE} }
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-CFB 2.6.1 Compound File Directory Entry</a>
*/
enum ObjectType {
	/** The object type for Unknown or Unallocated entries.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-CFB 2.6.1 Compound File Directory Entry</a>
	*/
	UNKNOWN(0x00, "Unknown or unallocated"),

	/** The object type for Storage Objects
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-CFB 2.6.1 Compound File Directory Entry</a>
	*/
	STORAGE(0x01, "Storage Object"),

	/** The object type for Stream Objects
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-CFB 2.6.1 Compound File Directory Entry</a>
	*/
	STREAM(0x02, "Stream Object"),

	/** The object type for Root Storage Objects
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-CFB 2.6.1 Compound File Directory Entry</a>
	*/
	ROOT_STORAGE(0x05, "Root Storage Object");

	/** The actual object type. */
	private final byte type;

	/** The name of the object type. */
	private final String name;
	
	/** Construct an ObjectType from the given byte read out of a directory entry block
	*	@param	type	The type as read
	*/
	ObjectType(int type, String name)
	{
		this.type = (byte) type;
		this.name = name;
	}

	/** Is the a Root Storage Object?
	*	@return	true if this is a Root Storage Object, false otherwise.
	*/
	boolean isRootStorage()
	{
		return this == ROOT_STORAGE;
	}

	/** Is the a Storage Object?
	*	@return	true if this is a Storage Object, false otherwise.
	*/
	boolean isStorage()
	{
		return this == STORAGE;
	}

	/** Is the a Stream Object?
	*	@return	true if this is a Stream Object, false otherwise.
	*/
	boolean isStream()
	{
		return this == STREAM;
	}

	public static ObjectType valueOf(byte type) throws UnknownStorageTypeException
	{
		for (ObjectType value : values()) {
			if (value.type == type) {
				return value;
			}
		}
		
		throw new UnknownStorageTypeException(type);
	}

	/** Create a String value describing this object type.
	*	@return	A String containing a description of this object type.
	*/
	@Override
	public String toString()
	{
		return name;
	}
}
