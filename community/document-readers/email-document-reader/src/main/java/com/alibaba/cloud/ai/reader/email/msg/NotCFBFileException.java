package com.alibaba.cloud.ai.reader.email.msg;

/** The NotCFBFileException is thrown when the first eight bytes of the file are not the CFB file signature bytes.
*	@see HeaderSignature
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/05060311-bfce-4b12-874d-71fd4ce63aea">MS-CFB Section 2.2: Compound File Header</a>
*/
public class NotCFBFileException extends Exception {

	/**	The serialVersionUID is required because the base class is serializable. */
	private static final long serialVersionUID = 1L;

	/**	Create a NotCFBFileException. */
	NotCFBFileException()
	{
		super();
	}
}

