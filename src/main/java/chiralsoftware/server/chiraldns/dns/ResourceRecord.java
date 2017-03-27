package chiralsoftware.server.chiraldns.dns;

import chiralsoftware.server.chiraldns.dns.rdata.Rdata;
import chiralsoftware.server.chiraldns.name.Name;
import java.util.Set;
import java.util.HashSet;
import java.nio.ByteBuffer;


/** This class defines the Answer, Authority, and Additional array elements.
 * These sections all have the same format:
 * Name, type, class, ttl, rdlength, and rdata.
 * The Message keeps arrays of these sections, and when it comes time
 * to compress and output the whole thing it does that.<p>
 * This class is not thread-safe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class ResourceRecord {

    /** Construct a ResourceRecord with a name and an Rdata */
    public ResourceRecord(Name name, Rdata rdata) {
	if(name == null) throw new NullPointerException("can't create a resource record without a name.");
	if(rdata == null)
	    throw new IllegalArgumentException("can't create a resource record without a rdata.");
	this.name = name;
	this.rdata = rdata;
	this.ttl = rdata.getTtl();
    }
    
    /** Construct a ResourceRecord, usually this is used with data from the db.
     * @deprecated Don't use this. */
    public ResourceRecord(Name name, Rdata rdata, int ttl) {
	if(name == null) throw new NullPointerException("Name cannot be null in ResourceRecord constructor.");
	if(rdata == null) throw new NullPointerException("Rdata cannot be null in ResourceRecord constructor.");
	this.name = name;
	this.rdata = rdata;
	this.ttl = ttl;
    }

    /** Every ResourceRecord has an associated name. */
    private final Name name;

    /** Every name could possibly be compressed.  The actual object here
     * could be either a regular name (if it is not compressable) or a CompressedName, if it
     * was.  Either way the length returns correctly. */
    private Name compressedName = null;

    /** This will have a problem with ttls greater than Interger.MAX_VALUE.
     * Those don't come up too often, but it should be fixed to handle that. */
    private int ttl = 60 * 60 * 24; // 24 hour ttl is set for now
    // 532 4400
    
    /** This class does not have fields for type, class, or data length, because
     * all those can be determined from the ResourceRecord. */
    private Rdata rdata;

    /** Hold a pointer to the start of this ResourceRecord.  It is used during
     * compression, and for debugging. */
    private int offset = 0;

    public Name getName() { assert(name != null); return name; }

    /** Return a set of all Names that this particular ResourceRecord has.
     * That includes its name and then all the names held in the Rdata. */
    public Set getNames() {
	// we know that every resource record has at least one name.
	Set result = new HashSet(1);
	result.add(name);
	result.addAll(rdata.getNames());
	return result;
    }

    public int getTtl() { return ttl; }

    public int getType() { return rdata.getType(); }

    public int getDNSClass() { return rdata.getDNSClass(); }

    public int getRdataLength() { return rdata.length(); }

    public Rdata getRdata() { return rdata; }

    /** This method changes the ResourceRecord into compressed format, ready for sending
     * down the wire. */
    public int compress(Compressor compressor, int offset) {
	// this.offset will record the offset of the start of this resource record, but we
	// return the offset which is the end of this record
	this.offset = offset;
	compressedName = compressor.compress(name, offset);
	// after the name section, there are ten bytes of type, class, ttl, rdlength
	offset += (compressedName.length() + 10);
	offset = rdata.compress(compressor, offset);
	return offset;
    }

    public void send(ByteBuffer bb) {
	if(bb == null) throw new NullPointerException("no ByteBuffer provided");
	if(compressedName == null)
	    name.send(bb);
	else compressedName.send(bb);
	bb.putShort((short) rdata.getType());
	bb.putShort((short) rdata.getDNSClass());
	bb.putInt(ttl);
	bb.putShort((short) rdata.length());
	rdata.send(bb);
    }

    /** Override the equals method so we can compare objects and make sure that no two equal
     * objects end up in a Set. */
    public boolean equals(Object o) {
	if(!(o instanceof ResourceRecord)) return false;
	return (((ResourceRecord) o).rdata.equals(rdata)) &&
	    (((ResourceRecord) o).name.equals(name));
    }

    /** Override the hashcode method so that sets will work correctly. */
    public int hashCode() { return rdata.hashCode() + name.hashCode(); }

    /** Override toString, for debugging */
    public String toString() {
	assert(name != null);
	assert(rdata != null);
	return "ResourceRecord at offset: " + offset + "; Name: " + name + ": " + rdata;
    }
}
