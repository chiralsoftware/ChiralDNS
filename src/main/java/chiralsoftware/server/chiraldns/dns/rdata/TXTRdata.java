package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.Type;
import java.util.Set;
import java.util.HashSet;
import java.nio.ByteBuffer;

/** Text records allow an administrator to include
 * informational text regarding a zone.  Text records are sent
 * as a series of text strings, each of up to 255 bytes, plus a 1-byte
 * length.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class TXTRdata extends Rdata {

    private final String[] messageArray;
    private final int len;

    public int length() {
	return len;
    }
    
    public TXTRdata(int ttl, String message) {
	super(ttl);
	if(message == null) throw new NullPointerException("Can't create a TXTRdata with a null message.");
	// break this message up into 255-byte chunks
	if(message.length() == 0) {
	    messageArray = new String[1];
	    messageArray[0] = message;
	    len = 1;
	    return;
	}
	int messageLength = message.length();
	int div = messageLength / 255;
	int mod = messageLength % 255;
	messageArray = new String[(mod == 0) ? div : (div + 1)];
	int segmentLength;
	for(int i = 0; i < messageArray.length; i++) {
	    segmentLength = messageLength - i * 255;
	    if(segmentLength > 255) segmentLength = 255;
	    messageArray[i] = message.substring(i * 255, i * 255 + segmentLength);
	}
	len = messageLength + messageArray.length;
    }

    public void send(ByteBuffer bb) {
	// This is safe to do because the constructor guarantees that the messageArray[]
	// always has at least one element, even if it is zero-length.
	for(int i = 0; i < messageArray.length; i++) {
	    bb.put((byte) (messageArray[i].length() & 0xff));
	    bb.put(messageArray[i].getBytes());
	}
    }

    /** TXT data are not compressable. */
    public int compress(Compressor compressor, int offset) {
	return offset + len;
    }

    public String toString() {
	StringBuffer result = new StringBuffer(len);
	for(int i = 0; i < messageArray.length; i++)
	    result.append(messageArray[i].length() + ": " + messageArray[i] + "; ");
	return "TXT record: (" + ttl +"): " + result;
    }

    public int getType() { return Type.TXT; }

    /** TXT records do not hold any names. */
    public Set getNames() { 
	return new HashSet();
    }

    public boolean equals(Object o) {
	if(!(o instanceof TXTRdata)) return false;
	String[] os = ((TXTRdata) o).messageArray;
	if(os.length != messageArray.length) return false;
	for(int i = 0; i < messageArray.length; i++) if(! os[i].equals(messageArray[i])) return false;
	return true;
    }

    public int hashCode() {
	int result = 69;
	for(int i = 0; i < messageArray.length; i++) result = result * 17 + messageArray[i].hashCode();
	return result;
    }
}
