package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.name.Name;
import java.util.Set;
import java.util.Collections;
import java.nio.ByteBuffer;

/** NS records hold name servers.  An NS record allows a name server
 * to be defined for a domain.  For example, .mp has an NS record
 * which defines ns1.nic.mp and ns2.nic.mp as name servers for the .mp domain.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class NSRdata extends Rdata {

    /** Holds the zone which is the server. */
    private Name server;

    /** The length of an NS record is simply the length of the name it holds. */
    public int length() {
	assert(server != null);
	return server.length();
    }

    /** Create a new NSRdata object, used for loading from the db. */
    public NSRdata(int ttl, Name server) {
	super(ttl);
	this.server = server;
    }

    /** return the Exchange as a name.  Note that this might be compressed. */
    public Name getServer() { return server; }

    public void send(ByteBuffer bb) {
	server.send(bb);
    }

    /** Required from abstract class.  The offset is a pointer to the first byte of the Rdata
     * section.  The first two bytes are taken by the unsigned short preference, and then the
     * compressed name starts. */
    public int compress(Compressor compressor, int offset) {
	assert(server != null);
	server = compressor.compress(server, offset);
	return offset + server.length();
    }

    public String toString() { return "NS server: (" + ttl +"): " + server; }

    public int getType() { return Type.NS; }

    /** Return the name of the server. */
    public Set getNames() { 
	return Collections.singleton(server);
    }

    public boolean equals(Object o) {
	if(!(o instanceof NSRdata)) return false;
	return  server.equals(((NSRdata) o).server);
    }

    public int hashCode() { return server.hashCode(); }
}
