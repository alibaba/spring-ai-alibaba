package com.alibaba.cloud.ai.reader.email.msg;

/** Convenience wrapper for a key-value pair. This is a wrapper for a {@link java.util.AbstractMap.SimpleImmutableEntry SimpleImmutableArray hashmap}  entry.
*	@param	<K>	The type of the key in the key-value pair
*	@param	<V>	The type of the valu in the key-value pair
*/
public class KVPEntry<K, V> extends java.util.AbstractMap.SimpleImmutableEntry<K, V>
{
	/**	The serialVersionUID is required because the base class is serializable. */
	private static final long serialVersionUID = 1L;

	/** Construct a new KVP pair.
	*	@param	k	The key for the entry
	*	@param	v	The value of the entry
	*/
	KVPEntry(K k, V v)
	{
		super(k, v);
	}
}
