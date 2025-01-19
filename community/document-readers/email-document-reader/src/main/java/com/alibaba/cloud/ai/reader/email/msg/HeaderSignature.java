package com.alibaba.cloud.ai.reader.email.msg;

/** The file signature.
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/05060311-bfce-4b12-874d-71fd4ce63aea">MS-CFB Section 2.2: Compound File Header</a>
*/
@SuppressWarnings("PMD.ClassNamingConventions")
class HeaderSignature {

	/** The bytes which form the signature, in the order in which they are documented.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/05060311-bfce-4b12-874d-71fd4ce63aea">MS-CFB Section 2.2: Compound File Header</a>
	*/
	private static final byte[] SIGNATURE_BYTES = {(byte)0xd0, (byte)0xcf, (byte)0x11, (byte)0xe0, (byte)0xa1, (byte)0xb1, (byte)0x1a, (byte)0xe1};

	/** The header signature as an 8-byte (long) value. */
	private static final long SIGNATURE = ByteUtil.makeLongLE(SIGNATURE_BYTES);

	/** Validate that the passed signature is a valid header signature.
	*	@param	signature	The signature to be checked
	*	@throws	NotCFBFileException	The signature does not match the expected value.
	*/
	static void validate(long signature)
	throws
		NotCFBFileException
	{
		if (signature != SIGNATURE){
			throw new NotCFBFileException();
		}
	}
}
