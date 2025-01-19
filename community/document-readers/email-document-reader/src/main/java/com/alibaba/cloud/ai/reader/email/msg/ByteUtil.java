package com.alibaba.cloud.ai.reader.email.msg;

/** The ByteUtil class contains utility functions for dealing with bytes. */
class ByteUtil {

	/** The number of 8-bit bytes in a long. */
	private static final int LONG_BYTES = Long.SIZE/Byte.SIZE;

	/** This array of hex digits is used for conversion of values 0-15 to 0-9, A-F. */
	private final static char[] HEX_DIGIT = {
		'0',
		'1',
		'2',
		'3',
		'4',
		'5',
		'6',
		'7',
		'8',
		'9',
		'A',
		'B',
		'C',
		'D',
		'E',
		'F'
	};

	/** Create a string representation expressing the given sequence of bytes in hexadecimal.
	*	@param	bytes	The bytes to convert to a String of hexadecimal values.
	*	@return	The String containing the hexadecimal representation of the given bytes.
	*/
	static String createHexByteString(final byte[] bytes)
	{
		StringBuilder s = new StringBuilder (3*bytes.length);
		for (int i = 0; i < bytes.length; ++i) {
			if (i > 0)
				s.append(' ');
			s.append(HEX_DIGIT[(bytes[i] & 0xff)/16]);
			s.append(HEX_DIGIT[(bytes[i] & 0xff)%16]);
		}
		return s.toString();
	}

	/** Create a signed long from the given array of bytes, ordered from LSB to MSB.
	*	@param	rawData	The bytes to make the long value from.
	*	@return	A long value corresponding to the given array of bytes as a little-endian value.
	*/
	static long makeLongLE(byte[] rawData)
	{
		int n = LONG_BYTES;
		long val = 0;
		for (int i = n - 1; i >= 0; --i)
			val = (val << Byte.SIZE) | (rawData[i] & 0xff);
		return val;
	}

	/** This is a simplistic test for some of the functions in this class.
	*	@param	args	The command line arguments passed to the test application (ignored).
	*/
	public static void main(String[] args)
	{
		byte[][] a =
		{
			{ 0x12, 0x34, 0x56, 0x78, (byte)0x9a, (byte)0xbc, (byte)0xde, (byte)0xf0 },
			{ 0x12, 0x34, 0x56, 0x78, (byte)0x9a, (byte)0xbc, (byte)0xde, (byte)0x0f },
			{ 0x00, 0x7a, 0x16, 0x0b, 0x00, 0x00, 0x00, 0x00 }
		};

		for (int i = 0; i < a.length; ++i)
		{
			long le = makeLongLE(a[i]);
			System.out.println("Little Endian: " + Long.toHexString(le) + '\n');
		}

		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
			byte[] ba = { (byte)i };
			System.out.printf("%d: 0x%02x %s %s%n", i, i & 0xff, Integer.toHexString(i), ByteUtil.createHexByteString(ba));
		}
	}
}
