package com.alibaba.cloud.ai.reader.email.msg;

/** The UnknownStorageTypeException is thrown when a object with an unrecognized storage type is encountered
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/60fe8611-66c3-496b-b70d-a504c94c9ace">MS-CFB 2.6.1 Compound File Directory Entry</a>
*/
public class UnknownStorageTypeException extends Exception
{
	/**	The serialVersionUID is required because the base class is serializable. */
	private static final long serialVersionUID = 1L;

	/**	Create an UnknownStorageTypeException. */
	UnknownStorageTypeException(byte type)
	{
		super("Unknown storage type " + type);
	}
}
