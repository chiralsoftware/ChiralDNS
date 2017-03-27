package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.DNSClasses;
import chiralsoftware.server.chiraldns.dns.Type;
import java.util.Set;
import java.nio.ByteBuffer;

/** This abstract class implements a resource record data section.  All resource record
 * data have a ttl and a type, but subclasses must hold additional information specific
 * to the type.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public abstract class Rdata {

    protected final int ttl;

    /** Return the type of the Rdata.
     * @see Type */
    public abstract int getType();

    /** Return the ttl for this rdata.
     * A note about ttl: it is a 32 bit <b>SIGNED</b> int... so only 31 bits can be used.
     * this is unlike ALL other ints in dns, which are unsigned. */
    public int getTtl() { return ttl; }

    /** Always returns IN (1). */
    public int getDNSClass() { return DNSClasses.IN; }

    /** Return the length of this data field. */
    public abstract int length();

    /** Nothing outiside this package should subclass Rdata. */
    Rdata(int ttl) { this.ttl = ttl; }

    /** All Rdata subclasses must be able to put their data in a ByteBuffer in wire-format. */
    public abstract void send(ByteBuffer bb);

    /** All implementors must implement a compression method. */
    public abstract int compress(Compressor compressor, int offset);

    /** All implementors must be able to return a set of all the names they contain.
     * This is needed so we can look up authority or additional records for these names
     * if necessary.  For instance, and MX record would return the name it points to
     * so that the resolver can add those names to the message. */
    public abstract Set getNames();

    /** Subclasses should override this. */
    public String toString() { return "Rdata: ttl: " + ttl; }
}
