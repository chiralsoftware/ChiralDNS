package chiralsoftware.server.chiraldns.dns;

import java.nio.ByteBuffer;

/** PacketDumper is a utility class which provides a static method which translates a binary
 * DNS packet into a human-readable decoded format.  This is useful to look for 
 * protocol violations, and also funny packets that could be security attacks.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class PacketDumper {

    /** Do not instantiate this class. */
    private PacketDumper() { }

    /** Which string is used to go to a new line.  Fix this so that it gets it from the System.properties. */
    private static final String nl = "\n";

    private static final String sixteenZeros = "0000000000000000";

    private static String shortToBinaryString(int s) {
	String r = Integer.toBinaryString(s & 0xffff);
	int x = r.length();
	if(x > 16) throw new IllegalStateException("Internal error in toBinaryString.");
	if(x == 16) return r;
	return sixteenZeros.substring(0, 16 - x) + r;
    }

    /** Decode a DNS packet contained in a byte array. */
    public static String decode(byte[] ba) { return decode(ba, ba.length); }

    private static StringBuffer decodeName(ByteBuffer bb) {
	assert(bb != null);
	StringBuffer sb = new StringBuffer();
	while(true) {
	    sb.append(bb.position() + ": ");
	    int labelByte = bb.get() & 0xff;
	    if(labelByte == 0) { // it is a length byte and the length is 0, so name over dude, name over!
		// sb.append("Null label");
		sb.append("Null label" + nl);
		return sb;
	    }
	    int labelType = (labelByte >> 6) & 3;
	    int labelLength = labelByte & 0x3f;
	    if(labelType == 0) {
		sb.append("Label: ");
		for(int i = 0; i < labelLength; i++) sb.append((char) bb.get());
		sb.append(nl);
	    } else if(labelType == 3) {
		int offset = (labelLength << 8) | (bb.get() & 0xff);
		sb.append("Pointer ==> " + offset);
		sb.append(nl);
		break;
	    } else {
		sb.append("Unknown label type: " + labelType + nl);
	    }
	}
	return sb;
    }

    /** Given a ResourceRecord type section from the packet decode it. */
    private static StringBuffer decodeSection(ByteBuffer bb) {
	assert(bb != null);
	int rdType; int rdLength; StringBuffer result = new StringBuffer();
	result.append("ResourceRecord:\n");
	result.append(decodeName(bb));
	result.append(bb.position() + ": TYPE: ");
	rdType = bb.getShort() & 0xffff;
	result.append(Type.string((short) rdType) + " (" + rdType + ")" + nl);
	result.append(bb.position() + ": CLASS: " + (bb.getShort() & 0xffff) + nl);
	result.append(bb.position() + ": TTL: " + bb.getInt() + nl);
	result.append(bb.position() + ": RDLENGTH: ");
	rdLength = bb.getShort() & 0xffff;
	result.append(rdLength + nl);
	if(rdType == Type.A) result.append(decodeAData(bb));
	else if(rdType == Type.NS) result.append(decodeNSData(bb));
	else if(rdType == Type.CNAME) result.append(decodeCNAMEData(bb));
	else for(int x = 0; x < rdLength; x++) bb.get();
	return result;
    }

    /** Given a ByteBuffer, decode the next section of it as an A record rdata. */
    private static StringBuffer decodeAData(ByteBuffer bb) {
	assert(bb != null);
	StringBuffer result = new StringBuffer();
	result.append(bb.position() + ": " + (bb.get() & 0xff) + "." +
		      + (bb.get() & 0xff) + "." +
		      + (bb.get() & 0xff) + "." +
		      + (bb.get() & 0xff) + nl);
	return result;
    }

    /** Given a ByteBuffer, decode the next rdata as an NS record. */
    private static StringBuffer decodeNSData(ByteBuffer bb) {
	assert(bb != null);
	StringBuffer result = new StringBuffer();
	// a NS record only contains one thing: a regular name
	result.append(decodeName(bb));
	return result;
    }

    /** Given a ByteBuffer, decode the next rdata as a CNAME record. */
    private static StringBuffer decodeCNAMEData(ByteBuffer bb) {
	assert(bb != null);
	StringBuffer result = new StringBuffer();
	// a CNAME record only contains one thing: a regular name
	result.append(decodeName(bb));
	return result;
    }

    /** Decode a DNS packet contained in a byte array, up to count bytes. */
    public static String decode(byte[] ba, int count) {
	return PacketDumper.decode(ByteBuffer.wrap(ba, 0, count));
    }

    /** Decode a DNS packet contained in a byte array, up to count bytes. */
    public static String decode(ByteBuffer bb) {
	if(bb == null) throw new NullPointerException("No bytebuffer specified in PacketDumper!");
	StringBuffer result = new StringBuffer();
	result.append("HEADER:" + nl);
	result.append(bb.position() + ": ID: " + (bb.getShort() & 0xffff) + nl);
	result.append(bb.position() + ": FLAGS: ");
	int flags = bb.getShort() & 0xffff;
	result.append("(" + shortToBinaryString(flags) + ") ");
	if(((flags >> 15) & 1) == 1) result.append("QR ");
	int opcode = (flags >> 11) & 0x0f;
	if(opcode == 0) result.append("QUERY ");
	else if(opcode == 1) result.append("IQUERY ");
	else if(opcode == 2) result.append("STATUS ");
	else result.append("OPCODE=" + opcode + "? ");
	if(((flags >> 10) & 1) == 1) result.append("AA ");
	if(((flags >> 9) & 1) == 1) result.append("TC ");
	if(((flags >> 8) & 1) == 1) result.append("RD ");
	if(((flags >> 7) & 1) == 1) result.append("RA ");
	result.append("Z=" + ((flags >> 4) & 7) + " "); // Z code is "reserved for future use"
	int rcode = flags & 0x0f;
	if(rcode == 0) result.append("NOERROR ");
	else if(rcode == 1) result.append("FORMATERROR ");
	else if(rcode == 2) result.append("SERVERFAILURE ");
	else if(rcode == 3) result.append("NXDOMAIN ");
	else if(rcode == 4) result.append("NOTIMPLEMENTED ");
	else if(rcode == 5) result.append("REFUSED ");
	else result.append("RCODE=" + rcode + "? ");
	result.append(nl);

	result.append(bb.position() + ": QDCOUNT: ");
	int qdcount = bb.getShort() & 0xffff;
	result.append(qdcount + nl);
	if(qdcount != 1) result.append("Error: qdcount must be 1." + nl);

	result.append(bb.position() + ": ANCOUNT: ");
	int ancount = bb.getShort() & 0xffff;
	result.append(ancount + nl);

	result.append(bb.position() + ": NSCOUNT: ");
	int nscount = bb.getShort() & 0xffff;
	result.append(nscount + nl);

	result.append(bb.position() + ": ARCOUNT: ");
	int arcount =  bb.getShort() & 0xffff;
	result.append(arcount + nl);

	// QUESTION SECTION
	result.append("QUESTION SECTION:" + nl);
	for(int i = 0; i < qdcount; i++) {
	    result.append(decodeName(bb));
	    result.append(bb.position() + ": QTYPE: " + (bb.getShort() & 0xffff) + nl);
	    result.append(bb.position() + ": QCLASS: " + (bb.getShort() & 0xffff) + nl);
	}

	// ANSWER SECTION
	result.append("ANSWER SECTION:" + nl);
	if(ancount == 0) result.append("(no entries in answer section)" + nl);
	else for(int i = 0; i < ancount; i++) result.append(decodeSection(bb));

	// NS SECTION
	result.append("NS SECTION:" + nl);
	if(nscount == 0) result.append("(no entries in ns section)" + nl);
	else for(int i = 0; i < nscount; i++) result.append(decodeSection(bb));

	// ADDITIONAL SECTION
	result.append("ADDITIONAL SECTION:" + nl);
	if(arcount == 0) result.append("(no entries in ar section)" + nl);
	else for(int i = 0; i < arcount; i++) result.append(decodeSection(bb));
	
	return result.toString();

    }
}
