package chiralsoftware.server.chiraldns.utils;

import chiralsoftware.server.chiraldns.dns.Header;
import chiralsoftware.server.chiraldns.dns.Message;
import chiralsoftware.server.chiraldns.dns.PacketDumper;
import chiralsoftware.server.chiraldns.dns.ProtocolException;
import chiralsoftware.server.chiraldns.dns.QuestionSection;
import chiralsoftware.server.chiraldns.name.NameFactory;
import java.util.Set;
import java.util.HashSet;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;


/** A class that queries a DNS server and decodes the response packet.
 * This is useful to understand the particulars of the protocol the server is using.
 * It could also be used for fingerprinting.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Dig {

    /** Don't instantiate this class. */
    private Dig() { }
    
    /** Use this from the command line to send a query to the DNS server.
     * Usage:
     * <pre>dig servername querystring querytype [port] [opcode]</pre>
     * FIX THIS: Make it do different query types, and make it so that it randomizes query ids.
     * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
    public static void main(String args[]) {
	if(args.length < 3) {
	    System.out.println("Usage: dig servername querystring querytype [port]");
	    return;
	}
	String serverName = args[0];
	String queryString = args[1];
	String queryTypeString = args[2];
	String portString = null;
	if(args.length > 3) portString = args[3];
	if(portString == null) portString = "53";
	int portNumber = 0;
	try { portNumber = Integer.parseInt(portString); }
	catch(NumberFormatException nfe) {
	    System.out.println("Bad port number: " + nfe.getMessage());
	    return;
	}
	try {
	    ByteBuffer bb = ByteBuffer.allocate(512);
	    DatagramChannel channel = DatagramChannel.open(); // the channel can open before connecting
	    InetSocketAddress sa = new InetSocketAddress(serverName, portNumber);
	    // now compose a new message
	    Header header = new Header((short) 666, false, false, false, false, false, Header.RCODE_NOERROR,
				       Header.QUERY);
	    QuestionSection questionSection =
		new QuestionSection(NameFactory.nameFromString(queryString), 1, 1); // fix this
	    Set empty = new HashSet();
	    Message m = new Message(header, questionSection, empty, empty, empty);
	    m.updateCounts();
	    System.out.println("About to send a message.");
	    bb.clear();
	    m.send(bb);
	    System.out.println("About to send this packet:");
	    bb.flip();
	    System.out.println(PacketDumper.decode(bb));
	    bb.flip();
	    channel.send(bb, sa);
	    bb.clear();
	    sa = (InetSocketAddress) channel.receive(bb); // why is this cast here?
	    bb.flip();
	    System.out.println("Packet received:");
	    System.out.println(PacketDumper.decode(bb));
	}
	catch(ProtocolException pe) { System.out.println("Caught a protocol exception: " + pe); }
	catch(UnknownHostException uhe) { System.out.println("Unknown host exception: " + uhe); }
	catch(IOException ioe) { System.out.println("IOException: " + ioe); }
    }
}
