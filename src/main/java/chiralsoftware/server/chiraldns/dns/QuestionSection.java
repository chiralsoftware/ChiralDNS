package chiralsoftware.server.chiraldns.dns;

import chiralsoftware.server.chiraldns.resolver.OutOfRangeException;
import chiralsoftware.server.chiraldns.name.Name;
import chiralsoftware.server.chiraldns.name.NameFactory;
import java.nio.ByteBuffer;

/** This class defines a question section object.<p>
 * The compress method of this class is not thread-safe.<p>
 * The RFC seems to suggest that there could be one or more questions in
 * the question section, because there is a parameter for query count.
 * This parameter must ALWAYS be 1, because the rest of the protocol
 * only supports one query per question.
 * If the query count is other than 1, this class throws exceptions. <p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class QuestionSection {

    /** Qname holds the name being queried about. */
    private Name name;

    /** This holds the type of the query; ie, A, MX, CNAME, etc. */
    private final int qType;

    /** qClass holds the class of the query.  This should always be  DNSClasses.IN, which
     * is the only class still in use, and the only class ChiralDNS supports. */
    private final int qClass;

    /** This holds the offset of the QuestionSection.  This value should be the same always
     * because the QuestionSection is always right after the header. */
    private int offset;

    /** Return the Name that is being asked for in the question section.  Query count is
     * always one, so only one name needs to be returned. */
    public Name getName() { return name; }

    /** Return the question type. */
    public int getType() { return qType; }

    /** Return the query class.  It will always be IN. */
    public int getDNSClass() { return qClass; }

    /** The number of questions in the QuestionSection could in theory be any number,
     * but nothing works if it is any number othat than 1. */
    public int getCount() { return 1; }

    /** Given a compressor and an offset, compress the QuestionSection.  This does not
     * result in any actual compression, but it enters the question name in the compression table. */
    public int compress(Compressor compressor, int offset) {
	if(compressor == null) throw new NullPointerException("null compressor");
	if((offset < Compressor.HEADER_LENGTH) | (offset > Compressor.MAX_OFFSET))
	    throw new IllegalArgumentException("Offset " + offset + " out of range.");
	this.offset = offset;
	name = compressor.compress(name, offset);
	// it's offset + 4 because we have a short qtype and a short qclass, so 4 bytes
	offset += name.length() + 4;
	return offset;
    }

    /** Given the appropriate parameters, construct a new QuestionSection.  This is used by Dig
     * to form a new query packet. */
    public QuestionSection(Name name, int qType, int qClass) {
	if(name == null) throw new NullPointerException("Name was null.");
	this.name = name;
	this.qType = qType;
	this.qClass = qClass;
    }
    
    /** Given a ByteBuffer, load count questions from the stream.  We assume there is
     * exactly one question.  Otherwise it is an error. */
    public QuestionSection(ByteBuffer bb)
	throws InvalidNameFormatException, OutOfRangeException, ProtocolException {
	if(bb == null) throw new NullPointerException("byte buffer cannot be null");
	name = NameFactory.nameFromByteBuffer(bb);
	qType = bb.getShort() & 0xffff;
	qClass = bb.getShort() & 0xffff;
	if(qClass != DNSClasses.IN) throw new ProtocolException("Cannot support queries of class: " + qClass);
    }

    /** Send this question section to the given ByteBuffer. */
    public void send(ByteBuffer bb) {
	name.send(bb);
	bb.putShort((short) qType);
	bb.putShort((short) qClass);
    }

    public String toString() {
	return "QuestionSection " + name + ": Type: " + Type.types.get(new Short((short) qType)) +
	    "(" + qType + ") " +
	    "Class: " + qClass;
    }

}
