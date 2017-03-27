package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.name.Name;
import java.util.Set;
import java.util.HashSet;
import java.nio.ByteBuffer;

/** SOA records define a Source of Authority.  A Source of Authority defines the limits
 * of a name server's authority.
 * The SOA record has these fields:
 * <ul>
 * <li><code>MNAME</code>: the domain name of the name server that was the original source of data for this zone</li>
 * <li><code>RNAME</code>: the responsible person, in domain name format, ie hh@mobile.mp is hh.mobile.mp.</li>
 * <li><code>SERIAL</code>: unsigned 32 bit int serial number for this zone.  It wraps and should be compared with
 *         sequence space arithmatic blah blah blah...</li>
 * <li><code>REFRESH</code>: 32 bit time interval before the zone should be refreshed.</li>
 * <li><code>RETRY</code>: A 32 bit time interval that should elapse before a failed refresh should be retried.</li>
 * <li><code>EXPIRE</code>: A 32 bit time value that specifies the upper limit on the time interval that can
 *         elapse before the zone is no longer authoritative.</li>
 * <li><code>MINIMUM</code>: The unsigned 32 bit minimum ttl,
 *         field that should be exported with any RR from this zone.</li>
 * </ul><p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class SOARdata extends Rdata {

    /** MNAME holds the source server. */
    private Name mname;

    /** Return the source server name. */
    public Name getMname() { return mname; }

    /** RNAME holds the responsible person, in labels format such as hh.mobile.mp. */
    private Name rname;
    /** Return the Responsible Person name, in dot notation, such as <code>hh.mobile.mp</code>
     * for <code>hh@mobile.mp</code>. */
    public Name getRname() { return rname; }

    /** A bunch of ints for keeping the zone up to date.  All of these are unsigned and so should
     * possibly be longs instead of ints. XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
    private final int serial;
    private final int refresh;
    private final int retry;
    private final int expire;
    private final int minimum;

    /** Getter methods for the numeric fields in a Source of Authority record. */
    public int getSerial() { return serial; }
    public int getRefresh() { return refresh; }
    public int getRetry() { return retry; }
    public int getExpire() { return expire; }
    public int getMinimum() { return minimum; }

    /** Return the length of this rdata field.  Length will depend on if it is compressed.
     * There are 5 32 bit fixed fields in the message, so that is added on. */
    public int length() { return mname.length() + rname.length() + 5 * 4; }

    /** Create a new SOARdata object, usually as a result of loading it from the db. */
    public SOARdata(Name mname, Name rname, int serial, int refresh, int retry, int expire,
		    int minimum) {
	super(minimum);
	if(mname == null) throw new NullPointerException("can't create soa if mname is null");
	if(rname == null) throw new NullPointerException("can't create soa if rname is null");
	this.mname = mname;
	this.rname = rname;
	this.serial = serial;
	this.refresh = refresh;
	this.retry = retry;
	this.expire = expire;
	this.minimum = minimum;
    }

    public void send (ByteBuffer bb) {
	mname.send(bb);
	rname.send(bb);
	bb.putInt(serial);
	bb.putInt(refresh);
	bb.putInt(retry);
	bb.putInt(expire);
	bb.putInt(minimum);
    }
	
    /** Given a compressor and a current offset compress the name fields and return a new offset. */
    public int compress(Compressor compressor, int offset) {
	if((compressor == null) || (offset < Compressor.HEADER_LENGTH) || (offset > Compressor.MAX_OFFSET))
	    throw new IllegalArgumentException("compressor was null or offset out of range");
	mname = compressor.compress(mname, offset);
	int mnameLength = mname.length();
	rname = compressor.compress(rname, offset + mnameLength);
	// the offset now is the end of the current name plus 20 bytes for the ints
	return mnameLength + rname.length() + 5 * 4;
    }

    /** The names in this record don't need to be looked up for the Additional section,
     * so this method always returns an empty Set. */
    public Set getNames() {
	return new HashSet();
    }

    /** Output this record as a string.  This should be updated so that it looks like the official
     * textual representation. */
    public String toString() {
	return "SOA: " + mname + ", " + rname + " " + serial + " " + refresh + " " + retry + " " +
	    expire + " " + minimum;
    }

    public int getType() { return Type.SOA; }
}
