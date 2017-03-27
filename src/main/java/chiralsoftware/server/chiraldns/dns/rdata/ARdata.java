package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.resolver.StringUtils;
import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.Type;
import java.util.Set;
import java.util.Collections;
import java.nio.ByteBuffer;

/** A records are IP4 Address records.
 * A records hold a 32-bit IP4 address.  These addresses can be represented
 * in dotted-quad format, such as 128.32.43.201.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class ARdata extends Rdata {

    /** This int holds the 32 bit IP addr. */
    private final int addr;

    /** An A record is always 4 bytes. */
    public int length() { return 4; } 

    public String toString() {
	return "A RRDATA (" + ttl + "): " + StringUtils.ipIntToString(addr);
    }

    public ARdata(int ttl, int addr) { super(ttl); this.addr = addr; }
 
    public int getType() { return Type.A; }

    /** This retrieves the ip addr held by this record as a plain old 32 bit int.
     * This is used by the Resolver for storing a new ARecord. */
    public int getAddress() { return addr; }

    /** Required for implementation of Rdata.  This is simple: just send the 32 bits as an int. */
    public void send(ByteBuffer bb) { bb.putInt(addr); }

    /** Nothing needs to be done to compress ARdata, but we do need to update the offset. */
    public int compress(Compressor compressor, int offset) {
	return offset + 4;
    }

    /** ARdata objects don't contain any names. */
    public Set getNames() { return Collections.EMPTY_SET; }

    /** All rdata must implement a correct equals method. */
    public boolean equals(Object o) {
	if(!(o instanceof ARdata)) return false;
	return (addr == ((ARdata) o).addr) && (ttl == ((ARdata) o).ttl);
    }

    public int hashCode() {
	return ttl * 17 + addr;
    }
}
