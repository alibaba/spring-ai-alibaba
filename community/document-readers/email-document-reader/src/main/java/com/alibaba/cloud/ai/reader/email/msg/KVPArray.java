package com.alibaba.cloud.ai.reader.email.msg;

/** Convenience class for propagating ordered KVP data to client applications. This is a wrapper for an {@link java.util.ArrayList ArrayList} of {@link KVPEntry} values.
*	@param	<K>	The type of the keys in the key-value pair
*	@param	<V>	The type of the value in the key-value pair
*/
public class KVPArray<K, V> extends java.util.ArrayList<KVPEntry<K, V>>
{
	/**	The serialVersionUID is required because the base class is serializable. */
	private static final long serialVersionUID = 1L;

	/** Constructor for the KVPArray object */
	public KVPArray()
	{
		super();
	}

	/** Add a new KVP pair to the end of the list.
	*	@param	k	The key for the entry
	*	@param	v	The value of the entry
	*/
	void add(K k, V v)
	{
		super.add(new KVPEntry<K, V>(k, v));
	}
}
