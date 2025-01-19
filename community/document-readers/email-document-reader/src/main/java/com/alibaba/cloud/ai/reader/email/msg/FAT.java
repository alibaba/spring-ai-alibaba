package com.alibaba.cloud.ai.reader.email.msg;

/** The File Allocation Table (FAT)
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/30e1013a-a0ff-4404-9ccf-d75d835ff404">MS-CFB Section 2.3: Compound File FAT Sectors</a>
*/
class FAT {
	/** The number of FAT entries
	*	@see	Header#numberOfFATSectors
	*	@see	Header#numberOfFATEntries
	*/
	final private int numEntries;

	/** The number of sectors in the file */
	final private int numSectors;

	/** The list of FAT index entries. */
	final private int[] fat;

	/** Iterator for FAT index entry chains */
	private class ChainIterator implements java.util.Iterator<Integer> {

		/** The next entry to be returned. */
		private int entry;

		/** Initialize the iterator through the FAT sector chains
		*	@param	firstSector	The first sector for the file's FAT
		*/
		private ChainIterator(int firstSector)
		{
			entry = firstSector;
		}

		/** Is there a new entry to return?
		*	@return	true if there are any more sectors in this chain,
		*		false otherwise
		*/
		@Override
		public boolean hasNext()
		{
			return entry != Sector.ENDOFCHAIN;
		}

		/** Return the next FAT index entry
		*	@return	the next entry in the chain as given by the FAT
		*/
		@Override
		public Integer next()
		{
			if (entry == Sector.ENDOFCHAIN)
				throw new java.util.NoSuchElementException();
			int retval = entry;
			entry = fat[entry];
			return retval;
		}
	}

	/** Iterator for free FAT entries */
	private class FreeSectorIterator implements java.util.Iterator<Integer> {

		/** The next entry to be returned. */
		private int entry;

		/** Create an iterator over the free sectors */
		private FreeSectorIterator()
		{
			while (entry < numSectors && fat[entry] != Sector.FREESECT)
				entry++;
		}

		/** Is there a new free entry to return?
		*	@return	true if there are any more free sectors to list,
		*		false otherwise
		*/
		@Override
		public boolean hasNext()
		{
			return entry < numSectors && fat[entry] == Sector.FREESECT;
		}

		/** Return the next FAT free entry
		*	@return	The next free sector according to the FAT
		*/
		@Override
		public Integer next()
		{
			int retval = entry;
			do {
				if (entry >= numSectors)
					throw new java.util.NoSuchElementException();
				++entry;
			} while (entry < numSectors && fat[entry] != Sector.FREESECT);
			return retval;
		}
	}

	/** Read in the entire FAT
	* 	@param	mbb	The data stream
	* 	@param	header	The CFB header structure
	* 	@param	difat	The double-indirect file allocation table structure.
	*/
	FAT(java.nio.MappedByteBuffer mbb, Header header, DIFAT difat)
	{
		// First index in a FAT sector is the FAT signature
		// and the last is either the index to the next sector, or the empty sector flag, 0xffffffff

		// Note that the number of entries in the FAT is usually greater
		// than the number of sectors in the file, since the number in
		// the FAT list is rounded up to fill an integral number of sectors.
		// Normally, a file will not end with a bunch of free sectors.
		numEntries = header.numberOfFATEntries();
		fat = new int[numEntries];

		numSectors = header.numberOfSectors();

		mbb.rewind();
		java.nio.IntBuffer al = mbb.asIntBuffer();

		int destIndex = 0;

		java.util.Iterator<Integer> difatIterator = difat.iterator();
		while (difatIterator.hasNext()){
			int currentSector = difatIterator.next();
			if (currentSector == Sector.FREESECT)
				continue;
			int readOffset = (currentSector + 1) * header.intsPerSector();
			al.position(readOffset);
			al.get(fat, destIndex, header.intsPerSector());
			destIndex += header.intsPerSector();
		}
	}

	/** Get an iterator for this file's FAT
	*	@param	firstSector	The first sector for the file's FAT
	*	@return	An Iterator through the FAT sectors
	*/
	java.util.Iterator<Integer> chainIterator(int firstSector)
	{
		return new ChainIterator(firstSector);
	}

	/** Get an iterator for free sectors in this file's FAT
	*	@return	An iterator through the free sectors in this file's FAT
	*/
	java.util.Iterator<Integer> freeSectorIterator()
	{
		return new FreeSectorIterator();
	}

	/** Get all the sector chains
	*	@return	An ArrayList of ArrayLists containing the sector chains
	*/
	java.util.List<java.util.List<Integer>> getAllChains()
	{
		java.util.List<java.util.List<Integer>> chains = new java.util.ArrayList<java.util.List<Integer>>();

		boolean[] shown = new boolean[numEntries];
		for (int i = 0; i < numEntries; ++i){
			if (shown[i])
				continue;

			/* FAT sector chains are defined in the DIFAT.
			*  DIFAT sector chains are defined in the DIFAT.
			*  Free sectors are not chained.
			*/
			if (fat[i] == Sector.FATSECT || fat[i] == Sector.DIFSECT || fat[i] == Sector.FREESECT){
				shown[i] = true;
				continue;
			}

			/* Found a new chain */
			java.util.List<Integer> thisChain = new java.util.ArrayList<Integer>();

			int sector = i;
			do {
				thisChain.add(sector);
				shown[sector] = true;
				sector = fat[sector];
			} while (sector != Sector.ENDOFCHAIN);

			chains.add(thisChain);
		}

		return chains;
	}

	/** Get a String representation of all the sector chains in the FAT, one chain per line.
	*	@return	A string listing all the chains in the FAT
	*/
	private String getChainsAsString()
	{
		java.util.Iterator<java.util.List<Integer>> chainsIterator = getAllChains().iterator();

		StringBuilder s = new StringBuilder();
		while(chainsIterator.hasNext()){
			if (s.length() > 0)
				s.append("\n");
			java.util.Iterator<Integer> thisChain = chainsIterator.next().iterator();
			boolean first = true;
			while (thisChain.hasNext()){
				if (first)
					first = false;
				else
					s.append(" ");
				s.append(thisChain.next());
			}
		}
		return s.toString();
	}

	/** Retrieve the content of a chain of sectors
	*	@param	startingSector	The starting sector in the chain
	*	@param	size		The size of the chain, in bytes
	*	@param	mbb		The byte buffer to read from
	*	@param	header		The file header
	*	@return	An array of bytes holding the contents of the sector chain.
	*/
	byte[] read(int startingSector, long size, java.nio.MappedByteBuffer mbb, Header header)
	{
		int nRemaining = (int)size;
		byte[] data = new byte[nRemaining];
		java.util.Iterator<Integer> iter = chainIterator(startingSector);
		int destOffset = 0;
		while (iter.hasNext()){
			int sector = iter.next();
			mbb.position(header.offset(sector));
			int nToRead = Math.min(nRemaining, header.sectorSize);
			mbb.get(data, destOffset, nToRead);
			destOffset += nToRead;
			nRemaining -= nToRead;
		}
		return data;
	}

	/**	Test this class by reading in the FAT index table and printing it out.
	*	@param	args	The msg file(s) to display the FAT for.
	*/
	@SuppressWarnings("PMD.DoNotCallSystemExit")
	public static void main(final String[] args)
	{
		if (args.length == 0) {
			System.out.println("use:\n\tjava io.github.jmcleodfoss.mst.FAT msg-file [msg-file ,,,]");
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
					FAT fat = new FAT(mbb, header, difat);

					System.out.println("FAT contents");
					for (int i = 0; i < fat.numEntries; ++i)
						System.out.printf("%d: %s%n", i, Sector.getDescription(fat.fat[i]));

					System.out.println("\nFAT sector chains");
					System.out.println(fat.getChainsAsString());

					System.out.println("\nFAT free sectors");
					StringBuilder s = new StringBuilder();
					java.util.Iterator<Integer> iter = fat.freeSectorIterator();
					while (iter.hasNext()){
						if (s.length() > 0)
							s.append(" ");
						s.append(iter.next());
					}
					System.out.println(s);
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
