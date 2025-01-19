package com.alibaba.cloud.ai.reader.email.msg;

/** Constants and classes for dealing with sectors
*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
*/
@SuppressWarnings("PMD.ClassNamingConventions")
class Sector {

	/** The maximum regular sector index
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
	*/
	private static final int MAXREGSEC = 0xfffffffa;

	/** A reserved sector
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
	*/
	private static final int RESERVED = 0xfffffffb;

	/** A DIFAT (Double Indirect File Allocation Table) sector
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
	*/
	static final int DIFSECT = 0xfffffffc;

	/** A FAT (File Allocation Table) sector
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
	*/
	static final int FATSECT = 0xfffffffd;

	/** The end of a chain of sectors
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
	*/
	static final int ENDOFCHAIN = 0xfffffffe;

	/** A free (unused) sector
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/9d33df18-7aee-4065-9121-4eabe41c29d4">MS-CFB Section 2.1: Compound Sector Numbers and Types</a>
	*/
	static final int FREESECT = 0xffffffff;

	/** Get a description of the given sector ID
	*	@param	sectorId	The sector to describe
	*	@return	A description of the passed sector ID
	*/
	static String getDescription(int sectorId)
	{
		switch (sectorId){
			case RESERVED: return "Reserved";
			case DIFSECT: return "DIFAT";
			case FATSECT: return "FAT";
			case ENDOFCHAIN: return "End of Chain";
			case FREESECT: return "Free";
			case MAXREGSEC: return "Max Regular Sector";
			default: return "Regular Sector " + String.format("%d", sectorId);
		}
	}

	/** Get the sector size given the sector shift.
	*	@param	sectorShift	The sector shift value (2 is shifted by this number to get the sector size)
	*	@return	The sector size corresponding to the given sectorShift.
	*	@see <a href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cfb/05060311-bfce-4b12-874d-71fd4ce63aea">MS-CFB Section 2.2: Compound File Header</a>
	*/
	static int sectorSize(short sectorShift)
	{
		return 2 << (sectorShift - 1);
	}
}
