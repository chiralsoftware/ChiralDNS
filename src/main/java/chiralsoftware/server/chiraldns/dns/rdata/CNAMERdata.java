package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.name.Name;
import java.util.Set;
import java.util.Collections;
import java.nio.ByteBuffer;

/** CNAME records hold cannonical names, allowing a server to point traffic from
 * one name to another name.
 * A cannonical name allows a name server to forward traffic from one name
 * to another.  A CNAME data section contains only the name to be forwarded
 * to.  For example, if www.yahoo.com had a CNAME of serverfarm.yahoo.com,
 * queries for the A record of www.yahoo.com would receive a CNAME record
 * refering to serverfarm.yahoo.com, and also any A records associated
 * with serverfarm.yahoo.com, if any are known to the server.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class CNAMERdata extends Rdata {

    /** Holds the zone which is the server. */
    private Name server;

    /** Return the length of this Rdata.  The length of a CNAME is equal to the length of the name it holds. */
    public int length() {
	assert(server != null);
	return server.length();
    }

    /** Create a new CNAMERdata object, used for loading from the db. */
    public CNAMERdata(int ttl, Name server) {
	super(ttl);
	this.server = server;
    }

    /** return the name held in the cname. */
    public Name getServer() { return server; }

    public void send(ByteBuffer bb) {
	server.send(bb);
    }

    public int compress(Compressor compressor, int offset) {
	assert(server != null);
	server = compressor.compress(server, offset);
	return offset + server.length();
    }

    public String toString() { return "CNAME server: (" + ttl +"): " + server; }

    public int getType() { return Type.CNAME; }

    /** Return the name of the server. */
    public Set getNames() { 
	return Collections.singleton(server);
    }

    public boolean equals(Object o) {
	if(!(o instanceof CNAMERdata)) return false;
	return 
	    server.equals(((CNAMERdata) o).server);
    }

    public int hashCode() {
	return server.hashCode();
    }
}
