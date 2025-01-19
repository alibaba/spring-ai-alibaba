package com.alibaba.cloud.ai.reader.email.msg;

/** The DataType class represents data types within a MSG file */
abstract class DataType {

	/** The mask for getting the property type from the tag. */
	static final int PROPERTY_TYPE_MASK = 0xffff;

	/** PTypInteger32, 32-bit integer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	final static int INTEGER_32 = 0x0003;

	/** PTypBoolean, a 1-bit value restricted to 1 or 0
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	final static int BOOLEAN = 0x000b;

	/** PTypInteger64, 64-bit integer
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	final static int INTEGER_64 = 0x0014;

	/** PTypString, variable-sized Unicode character string represented in UTF-16LE (Little Endian)
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	final static int STRING = 0x001f;

	/** PTypTime, 64-bit integer representing the number of 100-nanosecond intervals since January 1, 1601
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	final static int TIME = 0x0040;

	/** PTypBinary, variable-sized, starting with a 2 or 4 byte count of bytes making up the variable
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	final static int BINARY = 0x0102;

	/** The number of bytes in an integer. */
	static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;

	/** The number of bytes in a long. */
	static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;

	/** The character encoding used for Unicode data.  */
	private static final String CHARSET_WIDE = "UTF-16LE";

	/** A reader/display object for GUIDs. */
	static final GUID classIdReader = new GUID();

	/** The reader/display object for 8-bit integers. */
	static final Integer8 integer8Reader = new Integer8();

	/** The reader/display object for 16-bit. */
	static final Integer16 integer16Reader = new Integer16();

	/** The reader/display object for 32-bit integers. */
	static final Integer32 integer32Reader = new Integer32();

	/** The reader/display object for 64-bit integers. */
	static final Integer64 integer64Reader = new Integer64();

	/** A reader/display object for time values */
	static final Time timeReader = new Time();

	/** Create an object of type DataType. */
	protected DataType()
	{
	}

	/** Create a String from the given array of bytes, assuming UTF-16LE
	*	@param	arr	The bytes
	*	@return	The UTF-16LE String consist of the bytes in arr.
	*/
	static String createString(byte[] arr)
	{
		try {
			return new String(arr, CHARSET_WIDE);
		} catch (final java.io.UnsupportedEncodingException e){
			// UTF-16 should be supported everywhere by now.
			return "";
		}
	}

	/** Create a String describing an object of the type read in by this class.
	*	@param	o	The object to create a String representation of.
	*	@return	A String describing the object.
	*/
	abstract String makeString(final Object o);

	/** Read in an object of the target type.
	*	@param	byteBuffer	The incoming data stream from which to read the object.
	*	@return	The object read from the data stream.
	*/
	abstract Object read(java.nio.ByteBuffer byteBuffer);

	/** Get the size of the object read in in this class.
	*	@return	The size, in bytes, of the object read in by this class, if fixed (constant), otherwise 0.
	*/
	abstract int size();

	/** The SizedObject class contains functionality shared by manipulators for objects with known client-defined sizes. */
	private abstract static class SizedObject extends DataType {

		/** The size of this object read in. */
		protected final int size;

		/** Base class constructor for dealing with objects of known size.
		*	@param	size	The number of bytes in this object.
		*/
		protected SizedObject(final int size)
		{
			super();
			this.size = size;
		}

		/** Obtain the size of this object
		*	@return	The size of this object in the file, in bytes.
		*/
		@Override
		int size()
		{
			return size;
		}
	}

	/** The GUID class describes how to read, display, and return the size of a GUID
	*	@see GUID
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxprops/cc9d955b-1492-47de-9dce-5bdea80a3323">MS-OXPROPS Section 1.3.2: Commonly Used Property Sets</a>
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	// Note that this class name clashes with another class name in the package.
	@SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
	private static class GUID extends DataType {

		/** The size of a GUID. */
		static final int SIZE = io.github.jmcleodfoss.msg.GUID.SIZE;

		/** Create a reader / display object for a GUID value */
		GUID()
		{
			super();
		}

		/** Create a String describing the passed GUID value
		*	@param	o	The GUID to display.
		*	@return	A String showing the GUID.
		*/
		@Override
		String makeString(final Object o)
		{
			return ((io.github.jmcleodfoss.msg.GUID)o).toString();
		}

		/** Read in GUID
		*	@param	byteBuffer	The incoming data stream to read the GUID from.
		*	@return	The GUID read in from the incoming data stream.
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			byte arr[] = new byte[SIZE];
			byteBuffer.get(arr);
			io.github.jmcleodfoss.msg.GUID classId = new io.github.jmcleodfoss.msg.GUID(arr);
			return classId;
		}

		/** Return the size of GUID object
		*	@return	The size of a GUID
		*/
		@Override
		int size()
		{
			return SIZE;
		}
	}

	/** The Integer8 class describes how to read, display, and return the size of an 8-bit integer.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	private static class Integer8 extends DataType {

		/** Create a String from the passed Byte value
		*	@param	o	The Byte object to display.
		*	@return	A String representation of the Byte object (in hexadecimal).
		*/
		@Override
		String makeString(final Object o)
		{
			return Integer.toHexString((Byte)o & 0xff);
		}

		/** Read in an 8-bit integer from the data stream.
		*	@param	byteBuffer	The incoming data stream from which to read the 8-bit integer.
		*	@return	A Byte object corresponding to the 8-bit integer read in from the data stream.
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			return (Byte)byteBuffer.get();
		}

		/** Obtain the size of an 8-bit integer
		*	@return	The size of an 8-bit integer, in bytes.
		*/
		@Override
		int size()
		{
			return 1;
		}
	}

	/** The Integer16 class describes how to read, display, and get the size of a 16-bit integer.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	private static class Integer16 extends DataType {

		/** Create a String from the passed Short value.
		*	@param	o	The Short object to display.
		*	@return	A String representation of the Short object (in hexadecimal).
		*/
		@Override
		String makeString(final Object o)
		{
			return Integer.toHexString((Short)o & 0xffff);
		}

		/** Read in a 16-bit integer from the data stream.
		*	@param	byteBuffer	The incoming data stream from which to read the 16-bit integer.
		*	@return	A Short object corresponding to the 16-bit integer read in from the data stream.
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			return (Short)byteBuffer.getShort();
		}

		/** Obtain the size of a 16-bit integer.
		*	@return	The size of a 16-bit integer in bytes.
		*/
		@Override
		int size()
		{
			return 2;
		}
	}

	/** The Integer32 class describes how to read, display, and get the size of a 32-bit integer.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	private static class Integer32 extends DataType {

		/** Create a reader / display object for a PtypInteger32 value */
		Integer32()
		{
			super();
		}

		/** Create a String from the passed Integer value
		*	@param	o	The Integer object to display.
		*	@return	A String representation of the Integer object (in hexadecimal).
		*/
		@Override
		String makeString(final Object o)
		{
			return Integer.toHexString((Integer)o);
		}

		/** Read in a 32-bit integer from the data stream.
		*	@param	byteBuffer	The incoming data stream from which to read the 32-bit integer.
		*	@return	An Integer object corresponding to the 32-bit integer read in from the data stream.
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			return (Integer)byteBuffer.getInt();
		}

		/** Obtain the size of a 32-bit integer
		*	@return	The size of a 32-bit integer in the
		*/
		@Override
		int size()
		{
			return 4;
		}
	}

	/** The Integer64 class describes how to read, display, and get the size of a 64-bit integer.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	private static class Integer64 extends DataType {

		/** Create a String from the passed Long value
		*	@param	o	The Long object to display.
		*	@return	A String representation of the Long object (in hexadecimal).
		*/
		@Override
		String makeString(final Object o)
		{
			return Long.toHexString((Long)o);
		}

		/** Read in a 64-bit integer from the data stream.
		*	@param	byteBuffer	The incoming data stream from which to read the 64-bit integer.
		*	@return	A Long object corresponding to the 64-bit integer read in from the data stream.
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			return (Long)byteBuffer.getLong();
		}

		/** Obtain the size of a 64-bit integer
		*	@return	The size of a 64-bit integer in bytes.
		*/
		@Override
		int size()
		{
			return 8;
		}
	}

	/** The SizedByteArray class describes how to read, display, and get the size of an array of bytes of known size
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	static class SizedByteArray extends SizedObject {

		/** Create a reader/display object for an array of bytes of known size.
		*	@param	size	The number of bytes in the array.
		*/
		SizedByteArray(final int size)
		{
			super(size);
		}

		/** Create a String describing the passed array of bytes.
		*	@param	o	The array of bytes to display.
		*	@return	A String showing the bytes in the array in hexadecimal.
		*/
		@Override
		String makeString(final Object o)
		{
			byte[] a = (byte[])o;
			return ByteUtil.createHexByteString(a);
		}

		/** Read in an array of bytes of the given size.
		*	@param	byteBuffer	The incoming data stream to read from. Note that this is entirely consumed.
		*	@param	size		The number of bytes to read in
		*	@return	The array of bytes read in from the incoming data stream.
		*/
		Object read(java.nio.ByteBuffer byteBuffer, final int size)
		{
			byte arr[] = new byte[size];
			byteBuffer.get(arr);
			return arr;
		}

		/** Read in an array of bytes.
		*	@param	byteBuffer	The incoming data stream to read from. Note that this is entirely consumed.
		*	@return	The array of bytes read in from the incoming data stream.
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			return read(byteBuffer, size);
		}
	}

	/** The Time class describes how to read, display, and get the size of an MS Time object. It is converted on input to a standard Java Date object.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	static class Time extends DataType {

		/** The base for MS time, which is measured in hundreds of nanoseconds since January 1, 1601. */
		private static final java.util.Date PST_BASE_TIME = initBaseTime();

		/** The format to use when converting time objects to strings. */
		private final java.text.SimpleDateFormat OUTPUT_FORMAT = new java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm:ss");

		/** Create a reader/display object for a PTypTime value */
		private Time()
		{
			super();
		}

		/** Initialize the base time; exit on exception.
		*	@return	A Date object for the base time
		*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
		*/
		@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
		private static java.util.Date initBaseTime()
		{
			try {
				final java.util.Locale DATE_LOCALE = new java.util.Locale("en", "US");
				final java.text.SimpleDateFormat PST_BASE_FORMAT = new java.text.SimpleDateFormat("MMMM dd, yyyy", DATE_LOCALE);
				return PST_BASE_FORMAT.parse("January 1, 1601");
			} catch (final java.text.ParseException e) {
				// If this happens, the format defined above no longer matches the given date or there is a problem with the locale.
				// This can't be handled by client code; it needs a change to the code above.
				throw new RuntimeException(e);
			}
		}

		/** Create a String representation of the passed Date value
		*	@param	o	The Date to display.
		*	@return	A String representation of the given object, formatted according to {@link #OUTPUT_FORMAT}.
		*	@see	#OUTPUT_FORMAT
		*/
		@Override
		String makeString(final Object o)
		{
			return OUTPUT_FORMAT.format((java.util.Date)o);
		}

		/** Read in an MS time from the data stream.
		*	@param	byteBuffer	The incoming data stream from which to read the time.
		*	@return	A Java Date object corresponding to the MS time read from the data stream.
		*/
		@Override
		@SuppressWarnings("JavaUtilDate") // Relaticely safe use of java.util.Date
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			long hundred_ns = byteBuffer.getLong();
			long ms = hundred_ns/10000;
			ms += PST_BASE_TIME.getTime();
			return new java.util.Date(ms);
		}

		/** Obtain the size in bytes of an MS time object
		*	@return	The size of an MS time object
		*/
		@Override
		int size()
		{
			return 8;
		}
	}

	/** The UnicodeString class describes how to read, display, and get the size of a UTF-16 string of known size.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxcdata/0c77892e-288e-435a-9c49-be1c20c7afdb">MS-OXCDATA Section 2.11.1: Property Data Types</a>
	*/
	static class UnicodeString extends SizedObject {
		/** Create a reader/display object for a PTypString object of known size
		*	@param	size	The number of bytes in the UTF-16 string.
		*/
		UnicodeString(final int size)
		{
			super(size);
		}

		/** Create a String representation of a String (to be consistent with other data types); this implementation is trivial.
		*	@param	o	The String to display.
		*	@return	The given String.
		*/
		@Override
		String makeString(final Object o)
		{
			return (String)o;
		}

		/** Read in a String from the data stream.
		*	@param	byteBuffer	The incoming data stream from which to read the data.
		*	@return	A String corresponding to the Boolean read in from the data stream.
		*/
		@Override
		Object read(java.nio.ByteBuffer byteBuffer)
		{
			byte arr[] = new byte[size];
			byteBuffer.get(arr);
			return createString(arr);
		}
	}
}
