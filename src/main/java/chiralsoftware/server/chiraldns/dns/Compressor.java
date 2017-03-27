package chiralsoftware.server.chiraldns.dns;

import chiralsoftware.server.chiraldns.name.CompressionEntry;
import chiralsoftware.server.chiraldns.name.Name;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;


/** This class is used to compress data transmission in DNS format.  It builds up
 * a table of offsets.<p>
 * The way this works is every time a name is written, it is sent to the compressor
 * object, along with its offset.  If the name is non-compressable,
 * its labels are added to the table with offsets.  If it is compressable,
 * it returns a truncated name with an offset.  Compressor is <em>not</em> threadafe.
 * It is mutable; it changes itself every time a name is added to the compression set.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Compressor {

    private static final Logger LOG = Logger.getLogger(Compressor.class.getName());

    /** This is the fixed length of a DNS message header.  There are 6 rows
     * of two bytes.  See page 541 of the DNS book. */
    public static final int HEADER_LENGTH = 2 * 6;

    /** This is the maximum magnitude of an offset.  An offset is an unsigned 14
     * bit number. */
    public static final int MAX_OFFSET = (1 << 14) - 1;

    private final Map map;

    /** A compressor is constructed with no arguments. */
    public Compressor() {
	map = new HashMap();
    }

    /** This method returns a compressed version of a name.  If the name is not comrpessable
     * with the current table it returns the name unchanged, and stores it in the table.
     * If the name is compressable it returns a compressed name which can then be used
     * during output. */
    public Name compress(Name compressMe, int offset) {
	if((offset < HEADER_LENGTH) || (offset > MAX_OFFSET))
	    throw new IllegalArgumentException("offset: " + offset + " out of range");
	if(compressMe == null) throw new NullPointerException("can't compress a null name");
	if(compressMe.equals(Name.ROOT)) return Name.ROOT; // root is not comrpessable
	// this could be made more efficient
	CompressionEntry ce = new CompressionEntry(compressMe);
	Name result = null;
	for(int i = 0, limit = ce.getCount() - 1; i < limit; i++) {
	    if(!map.containsKey(ce)) { // this compression entry is not already in the set so add it
		map.put(ce, new Integer(offset));
		offset += ce.getTopLabelLength() + 1;
		ce = ce.chop();
		continue;
	    }
	    // otherwise the compression set does contain this CompressionEntry, so we have found what we are looking for
	    // do not add any further entries, and make up a new CompressedName
	    Integer target = (Integer) map.get(ce);
	    result = compressMe.compress(ce.getCount(), target.intValue());
	    break;
	}
	if(result != null) return result;
	// otherwise the name couldn't be compressed, but it has been fully added to the table.
	return compressMe;
    }

    /** This method dumps the current compression table. */
    public String toString() {
	assert(map != null);
	StringBuffer sb = new StringBuffer("Compressor:\n");
	Set namesSet = map.entrySet();
	Iterator it = namesSet.iterator();
	Map.Entry e;
	CompressionEntry ce;
	Integer i;
	while(it.hasNext()) {
	    e = (Map.Entry) it.next();
	    ce = (CompressionEntry) e.getKey();
	    i = (Integer) e.getValue();
	    sb.append(i + ": " + ce + "\n");
	}
	return sb.toString();
    }
}


/*


How a compressor works:

Start with a name to be compressed, at a particular offset: 25 mail.servers.yahoo.com
Compression table:
12 www.yahoo.com
16 yahoo.com
22 com

Looking through the labels on mail.servers.yahoo.com:
25 mail.servers.yahoo.com - no match - uncompressed labels: mail
30 servers.yahoo.com - no match - uncompressed labels: mail.servers
38 yahoo.com - match - pointer to => 16

Add to compression table:
25 mail.servers.yahoo.com
30 servers.yahoo.com

Do not add yahoo.com because it is already there

Starting with the same table, to add www.mobile.mp:
www.mobile.mp - no match - add to table
mobile.mp - no match - add to table
mp - no match - add to table
We have reached the end, so return the Name as it was, with no compression.


*/

