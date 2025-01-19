package com.alibaba.cloud.ai.reader.email.msg;

/** The DataDefinition class encapsulates definitions used for reading values from a data stream. */
class DataDefinition {

	/** The name under which the data for this field is to be stored. */
	private final String name;

	/** An object describing how to read and display the data for this field. */
	private final DataType description;

	/** Whether the data should be saved or discarded. */
	private final boolean fSave;

	/** Create an object to read in data with the given description, saving it under the given name if fSave is true.
	*	@param	name		The field name with which the data will be stored and retrieved.
	*	@param	description	The description of how to read in the field.
	*	@param	fSave		A flag indicating whether the data should be saved or skipped.
	*/
	DataDefinition(final String name, final DataType description, final boolean fSave)
	{
		this.name = name;
		this.description = description;
		this.fSave = fSave;
	}

	/** Create an object to skip over data with the given description.
	*	@param	name		The field name with which the data would be stored and retrieved (used for logging only in this
	*	@param	description	The description of how to read in the field.
	*/
	DataDefinition(final String name, final DataType description)
	{
		this(name, description, false);
	}

	/** Read in or skip a value described by description from stream, storing the result in data if necessary.
	*	@param	description	The description of how to read in the field.
	*	@param	byteBuffer	The input data stream from which to read the field.
	*	@param	data		The location in which to store the field.
	*	@throws	java.io.IOException	An I/O error was encountered while reading in the requested data.
	*/
	static void read(final DataDefinition description, java.nio.ByteBuffer byteBuffer, java.util.IdentityHashMap<String, Object> data)
	throws
		java.io.IOException
	{
		if (description.fSave) {
			final Object value = description.description.read(byteBuffer);
			if (description.fSave)
				data.put(description.name, value);
		} else {
			byteBuffer.position(byteBuffer.position() + description.description.size());
		}
	}

	/** Get the aggregate size in bytes of the data represented by the data array.
	*	@param	data	The list of data definitions describing the data for which to return the size.
	*	@return	The size, in bytes, of the data described by the given data description array.
	*/
	static int size(final DataDefinition[] data)
	{
		int s = 0;
		for (final DataDefinition d : data)
			s += d.description.size();
		return s;
	}

	/** Provide a text description of this object.
	*	@return	A String describing this data definition object.
	*/
	@Override
	public String toString()
	{
		return String.format("%s %d %ssaved", name, description.size(), fSave ? "" : "not ");
	}
}
