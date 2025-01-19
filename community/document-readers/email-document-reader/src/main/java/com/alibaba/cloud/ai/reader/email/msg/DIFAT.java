package com.alibaba.cloud.ai.reader.email.msg;

/** The Double-Indirect File Allocation Table (DIFAT)
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/0afa4e43-b18f-432a-9917-4f276eca7a73">MS-CFB Section 2.5: Compound File DIFAT Sectors</a>
*/
class DIFAT {

	/** Offset of DIFAT entries in the header (in terms of ints).
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/05060311-bfce-4b12-874d-71fd4ce63aea">MS-CFB Section 2.2: Compound File Header</a>
	*/
	private static final int HEADER_DIFAT_OFFSET = 0x004c / DataType.SIZEOF_INT;

	/** The number of DIFAT entries in the header.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/05060311-bfce-4b12-874d-71fd4ce63aea">MS-CFB Section 2.2: Compound File Header</a>
	*/
	private static final int HEADER_DIFAT_ENTRIES = 109;

	/** The number of DIFAT sectors (from the header)
	*	@see	Header#numberOfDIFATSectors
	*/
	private final int numEntries;

	/** The list of DIFAT index entries. */
	private int[] difat;

	/** Iterator for DIFAT index entries */
	private class Iterator implements java.util.Iterator<Integer> {

		/** The next entry to be returned. */
		private int entry;

		/** Initialize the DIFAT iterator */
		private Iterator()
		{
			entry = 0;
		}

		/** Is there a new entry to return?
		*	@return	True if there is another DIFAT entry to return,
		*		false if there is not
		*/
		@Override
		public boolean hasNext()
		{
			return entry < numEntries;
		}

		/** Get the next DIFAT index entry
		*	@return	The next DIFAT index entry
		*/
		@Override
		public Integer next()
		{
			if (entry >= numEntries)
				throw new java.util.NoSuchElementException();
			return difat[entry++];
		}
	}

	/** Create the list of DIFAT entries
	*   @param	mbb	The data stream
	*   @param	header	The CFB header structur
	*/
	DIFAT(java.nio.MappedByteBuffer mbb, Header header)
	{
		// First index in a DIFAT sector is the DIFAT signature
		// and the last is either the index to the next sector, or the empty sector flag, 0xffffffff
		final int entriesPerSector = header.intsPerSector() - 2;
		numEntries = HEADER_DIFAT_ENTRIES + header.numberOfDIFATEntries();
		difat = new int[numEntries];

		mbb.rewind();
		java.nio.IntBuffer al = mbb.asIntBuffer();

		boolean fHeader = true;
		int nToRead;
		int currentSector = 0;
		int destIndex = 0;

		do {
			if (fHeader){
				nToRead = HEADER_DIFAT_ENTRIES;
				al.position(HEADER_DIFAT_OFFSET);
			} else {
				nToRead = entriesPerSector;
				al.position(currentSector*header.intsPerSector());
				final int sectorSignature = al.get();
				if (sectorSignature != Sector.DIFSECT){
					System.out.printf("Invalid sector signature for DIFAT: found 0x%08x expected 0x%08x%n", sectorSignature, Sector.DIFSECT);
				}
			}

			al.get(difat, destIndex, nToRead);

			currentSector = fHeader ? header.firstDIFATSectorLocation : al.get();

			destIndex += entriesPerSector;
		} while (currentSector != Sector.ENDOFCHAIN);
	}

	/** Make DIFAT data available to client applications
	*	@return	An array of key-value pairs consisting of a description of the data and the data itself
	*/
	KVPArray<Integer, Integer> data()
	{
		KVPArray<Integer, Integer> l = new KVPArray<Integer, Integer>();
		for (int iSrc = 0, iDest = 0; iSrc < numEntries; ++iSrc){
			if (difat[iSrc] != Sector.FREESECT){
				l.add((Integer)iDest++, (Integer)difat[iSrc]);
			}
		}
		return l;
	}

	/** Get an iterator for this DIFAT
	*	@return	An iterator through the DIFAT entries.
	*/
	java.util.Iterator<Integer> iterator()
	{
		return new Iterator();
	}

	/**	Test this class by reading in the DIFAT index table and printing it out.
	*	@param	args	The msg file(s) to print the DIFAT index table of
	*/
	@SuppressWarnings("PMD.DoNotCallSystemExit")
	public static void main(final String[] args)
	{
		if (args.length == 0) {
			System.out.println("use:\n\tjava io.github.jmcleodfoss.mst.FAT msg-file [msg-file ...]");
			System.exit(1);
		}
		for (String a: args) {
			System.out.println(a);
			try {
				java.io.File file = new java.io.File(a);
				java.io.FileInputStream stream = new java.io.FileInputStream(file);
				try {
					java.nio.channels.FileChannel fc = stream.getChannel();
					java.nio.MappedByteBuffer mbb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fc.size());
					mbb.order(java.nio.ByteOrder.LITTLE_ENDIAN);

					Header header = new Header(mbb, fc.size());
					DIFAT difat = new DIFAT(mbb, header);
					java.util.Iterator<Integer> iterator = difat.iterator();
					while (iterator.hasNext()) {
						System.out.println(Sector.getDescription(iterator.next()));
					}

					System.out.println();
					System.out.println("FAT sector chain description");
					KVPArray<Integer, Integer> data = difat.data();
					java.util.Iterator<KVPEntry<Integer, Integer>> fatchain = data.iterator();
					while (fatchain.hasNext()){
						KVPEntry<Integer, Integer> e = fatchain.next();
						System.out.printf("FAT sector %d index %d%n", e.getKey(), e.getValue());
					}
				} catch (final java.io.IOException e) {
					System.out.printf("There was a problem reading from file %s%n", a);
				} catch (final NotCFBFileException e) {
					e.printStackTrace(System.out);
				} finally {
					try {
						stream.close();
					} catch (final java.io.IOException e) {
						System.out.printf("There was a problem closing file %s%n", a);
					}
				}
			} catch (final java.io.FileNotFoundException e) {
				System.out.printf("File %s not found%n", a);
			}
		}
	}
}
