package com.alibaba.cloud.ai.reader.email.msg;

/** The DataContainer class may be used to manage a set up DataDefinition objects, saving all values read in a map member.
*	It provides utility methods for retrieving various useful types.
*	Note that it is based on java.util.IdentityHashMap, so it is necessary to "get" a value using the identical argument used to
*	"put" it.
*
*	Changing from HashMap to IdentityHashMap reduced run-time by ~33% for the original test PST file.
*/
class DataContainer extends java.util.IdentityHashMap<String, Object> {

	/** The serialVersionUID is required because the base class is serializable. */
	private static final long serialVersionUID = 1L;

	/** Read in all descriptions from the given data stream.
	*	@param	byteBuffer	The input stream from which to read the data.
	*	@param	description	The list of descriptions of data to be read.
	*	@throws	java.io.IOException	An I/O problem was encountered while reading in the requested data.
	*/
	void read(java.nio.ByteBuffer byteBuffer, final DataDefinition... description)
	throws
		java.io.IOException
	{
		for (DataDefinition d : description)
			DataDefinition.read(d, byteBuffer, this);
	}

	/** Format and return a human-readable representation of this
	*   DataContainer with the property names and hex byte strings for each.
	*	@return	A String showing the contents of this DataContainer object.
	*/
	@Override
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		java.util.Iterator<Entry<String, Object>> iterator = entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();

			s.append('\n');
			s.append("\t");
			s.append(entry.getKey());
			s.append(':');
			s.append(' ');
			s.append(entry.getValue().toString());
		}
		return s.toString();
	}
}
