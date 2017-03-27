package chiralsoftware.server.chiraldns.dns;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.nio.ByteBuffer;

/** This class implements a DNS message header.
 * A message header is always exactly 6 * 16 bits long, with a format as described below.<p>
 * This class is immutable and thread-safe.<p>
 * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 * Fix some of the constructors in this!<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Header {

    /** All messages have a unique id, 16 bits. */
    private final short ID;
    
    /** All messages are either queries or responses.  If this bit is true, then it is a response.
     * If it is false the message is a query. */
    private final boolean QR;
    
    /** OPCODE specifies what kind of operation this is going to be.  It is a four-bit field in the final
     * message. */
    private final byte OPCODE;

    /** Opcode for a QUERY type. */
    public final static byte QUERY = 0;

    /** Opcode for an IQUERY.  Inverse queries are not implemented. */
    public final static byte IQUERY = 1;

    /** Opcode for a STATUS query. */
    public final static byte STATUS = 2;

    /** A map mapping opcodes, as Integer objects, to the opcode names.  This map is
     * immutable. */
    public static Map opcodeNames;
    static {
	Map m = new HashMap(3);
	m.put(new Integer(QUERY), "QUERY");
	m.put(new Integer(IQUERY), "IQUERY");
	m.put(new Integer(STATUS), "STATUS");
	opcodeNames = Collections.unmodifiableMap(m);
    }

    /** values 3-15 are reserved for future use. */

    /** AA is Auhoritative Answer.  If this is true, this response contains an authoritative answer
     * to the query. */
    private final boolean AA;

    /** TC is TrunCation.  If this is true it means that the message had to be truncated because
     * the response was longer than the permitted size. */
    private final boolean TC;

    /** RD is Recursion Desired.  This means that the host making the query wants the server
     * to recurse to answer the query.  */
    private final boolean RD;

    /** RA is Recursion Available.  A server can tell a querying host if recursion is possible. */
    private final boolean RA;

    /** Z is reserved for future use.  Must always be 0. */
    private final byte Z = 0;

    /** RCODE is the Response Code.  It is a 4-bit field set as part of the response. */
    private final byte RCODE;
    /** These are the valid possible response codes:<br>
     * No error:*/
    public static final byte RCODE_NOERROR = 0;
    /** The name server was unable to interpret the query. */
    public static final byte RCODE_FORMATERROR = 1;
    /** Server failure (internal error) */
    public static final byte RCODE_SERVERFAILURE = 2;
    /** Name Error: Meaningful only for responses from an authoritative name server, this code
     * signifies that the domain name referenced in the query does not exist. */
    public static final byte RCODE_NAMEERROR = 3;
    /** This means that the name server does not support the requested kind of query. */
    public static final byte RCODE_NOTIMPLEMENTED = 4;
    /** Refused: The server refuses to perform the specified operation for policy reasons.
     * Use this in response to a request for a zone xfer, for instance. */
    public static final byte RCODE_REFUSED = 5;
    /** Codes 6-15 are reserved for future use. */

    /** Note that all these below are 16 bit (short) but they are unsigned so
     * we represent them with a 32-bit signed int and use BitBuffer to read and write them. */
    /** A 16 bit unsigned int specifying the number of entries in the question section. */
    private final int QDCOUNT;

    /** A 16 bit unsigned int specifying the number of Resource Records in the answer section. */
    private final int ANCOUNT;

    /** A 16-bit unsigned int specifying the number of name server RRs in the authority records section. */
    private final int NSCOUNT;

    /** A 16-bit unsigned int specifying the number of resource records in the additional records
     * section. */
    private final int ARCOUNT;

    // And that is all there is to a header.

    /** Returns true if this message is a response, and false if it is a query. */
    public boolean isQueryResponse() { return QR; }

    /** What is the operation code for this query? */
    public byte getOpcode() { return OPCODE; }

    public short getID() { return ID; }

    /** This method loads a Header from an InputStream. */
    public Header(ByteBuffer bb) throws ProtocolException {
	if(bb == null) throw new NullPointerException("can't read from a null buffer");
	ID = bb.getShort();
	int flags = bb.getShort() & 0xffff;
	QR = ((flags >> 15) & 1) == 1;
	OPCODE = (byte) ((flags >> 11) & 0x0f);
	AA = ((flags >> 10) & 1) == 1;
	TC = ((flags >> 9) & 1) == 1;
	RD = ((flags >> 8) & 1) == 1;
	RA = ((flags >> 7) & 1) == 1;
	// We don't need to mess with Z because it's unused
	RCODE = (byte) (flags & 0x0f);
	// now we read off these unsigned shorts as ints
	QDCOUNT = bb.getShort() & 0xffff;
	ANCOUNT = bb.getShort() & 0xffff;
	NSCOUNT = bb.getShort() & 0xffff;
	ARCOUNT = bb.getShort() & 0xffff;
	if(QDCOUNT != 1)
	    throw new ProtocolException("Wrong question count in header: " + QDCOUNT);
    }

    /** This constructor is used to construct a header in a response.  Fix this
     * to take counts, also. */
    public Header(short id, boolean QR, boolean AA, boolean TC, boolean RD, boolean RA,
		  byte RCODE, byte OPCODE) {
	if((RCODE < RCODE_NOERROR) || (RCODE > RCODE_REFUSED))
	    throw new IllegalArgumentException("rcode out of range: " + RCODE);
	if((OPCODE < QUERY) || (OPCODE > STATUS))
	    throw new IllegalArgumentException("opcode out of range: " + OPCODE);
	this.ID = id;
	this.QR = QR;
	this.AA = AA;
	this.TC = TC;
	this.RD = RD;
	this.RA = RA;
	this.RCODE = RCODE;
	this.OPCODE = OPCODE;
	QDCOUNT = 1;
	ANCOUNT = 0; NSCOUNT = 0; ARCOUNT = 0; // this is wrong!!!!
    }

    /** Returns how many questions there are in the question section.  Should always be 1. */
    public int getQDCOUNT() { return QDCOUNT; }

    /** How many answers are there in the answer section. */
    public int getANCOUNT() { return ANCOUNT; }

    public int getNSCOUNT() { return NSCOUNT; }

    public int getARCOUNT() { return ARCOUNT; }

    /** Given an existing Header, create a new Header with updates to the counts of the different
     * sections. */
    public Header(Header h, int newQDCount, int newANCount, int newNSCount, int newARCount) {
	if(h == null) throw new NullPointerException("Can't create a new header from a null header.");
	if(newQDCount != 1)
	    throw new IllegalArgumentException("Can't have a question count other than 1.");
	if(newANCount < 0)
	    throw new IllegalArgumentException("Can't have less than 0 answers.");
	if(newNSCount < 0)
	    throw new IllegalArgumentException("Can't have less than 0 authorities.");
	if(newARCount < 0)
	    throw new IllegalArgumentException("Can't have less than 0 additional records.");
	this.ID = h.ID;
	this.QR = h.QR;
	this.OPCODE = h.OPCODE;
	this.AA = h.AA;
	this.TC = h.TC;
	this.RD = h.RD;
	this.RA = h.RA;
	this.RCODE = h.RCODE;
	QDCOUNT = newQDCount;
	ANCOUNT = newANCount;
	NSCOUNT = newNSCount;
	ARCOUNT = newARCount;
    }

    /** Send this header off to the ByteBuffer in the correct wire format.  Normally
     * this is called by the Message object. */
    public void send(ByteBuffer bb) {
	bb.putShort(ID);
	// now make up flags
	//  0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15
	// +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	// |QR|   OPCODE  |AA|TC|RD|RA|    Z   |  RCODE    |
	// +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	int flags = 0;
	if(QR) flags |= (1 << 15);
	flags |= ((OPCODE & 15) << 11);
	if(AA) flags |= (1 << 10);
	if(TC) flags |= (1 << 9);
	if(RD) flags |= (1 << 8);
	if(RA) flags |= (1 << 7);
	// don't do anything for Z, which will always be 0
	flags |= (RCODE & 15);
	bb.putShort((short) flags);
	// now send the counts of the different sections
	bb.putShort((short) QDCOUNT);
	bb.putShort((short) ANCOUNT);
	bb.putShort((short) NSCOUNT);
	bb.putShort((short) ARCOUNT);
    }

    public String toString() { 
	return "Header flags: Id: " + ID + "; " +
	    "QR: " + QR + "; " +
	    "Opcode: " + OPCODE + ", " + opcodeNames.get(new Integer(OPCODE)) + "; " +
	    "AA: " + AA + "; " +
	    "TC: " + TC + "; " +
	    "RD: " + RD + "; " +
	    "RA: " + RA + "; " +
	    "RCODE: " + RCODE + "; " +
	    "QDCOUNT: " + QDCOUNT + "; " +
	    "ANCOUNT: " + ANCOUNT + "; " +
	    "NSCOUNT: " + NSCOUNT + "; " +
	    "ARCOUNT: " + ARCOUNT;
    }
}
