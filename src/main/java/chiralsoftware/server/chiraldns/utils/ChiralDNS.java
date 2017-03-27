package chiralsoftware.server.chiraldns.utils;

import chiralsoftware.server.chiraldns.resolver.Resolver;
import chiralsoftware.server.chiraldns.resolver.Version;
import chiralsoftware.server.chiraldns.dns.Header;
import chiralsoftware.server.chiraldns.dns.Message;
import chiralsoftware.server.chiraldns.dns.PacketDumper;
import chiralsoftware.server.chiraldns.dns.ProtocolException;
import chiralsoftware.server.chiraldns.dns.QuestionSection;
import chiralsoftware.server.chiraldns.dns.ResourceRecord;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.dns.rdata.CNAMERdata;
import chiralsoftware.server.chiraldns.dns.rdata.Rdata;
import chiralsoftware.server.chiraldns.name.DbName;
import chiralsoftware.server.chiraldns.name.Name;
import chiralsoftware.server.chiraldns.name.NameFactory;
import chiralsoftware.server.chiraldns.name.QuestionKey;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/** This is the class that actually starts resolver and listens on the ports.<p>
 * Here is the url for the algorithm:<p>
 * This uses the following standard algorithm to fully resolve a name:<p>
 * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 * Fix this description.<p>
 * In the response packet:
 * <ol>
 * <li> Set or clear the RA (recursion available) flag.  In this server recursion is not available.</li>
 * <li> Search the available zones for the zone which is the nearest ancestor to QNAME.  If the zone
 * is found, go to step 3, otherwise go to step 4.</li>
 * <li> Start matching down, label by label, in the zone.  The matching process can terminate
 * in several ways:
 *    <ol>
 *     <li> If the whole QNAME is matched, we have found the node.  If the data at the node is a CNAME,
 *          and QTYPE doesn't match CNAME, copy the CNAME RR into the answer section of the response,
 *          change QNAME to canonical name in the CNAME RR, and go back to step 1.
 *          Otherwise copy all RRs which match QTYPE into the answer section and go to step 6.</li>
 *     <li> If a match would take us out of the authoritative data, we have a referral.
 *          This happens when we encounter a node with NS RRs marking cuts along the bottom of a zone.
 *          In other words we are going to a sub-zone.  In this case we need to give a referral
 *          to that sub-zone's authority.  To do this, copy NS RRs (which don't have A records)
 *          to the <b>authority</b> section of the reply.  Put whatever addresses are available or
 *          known which correspond to those NS RRs in the <b>additional</b> section.  In this special
 *          case use glue RRs if the addresses are not available from the authoritative data or the cache.
 *          Go to Step 4.</li>
 *     <li> If at some label, a match is impossible (ie, the corresponding label does not exist),
 *          see if a "*" label exists.  If the * label does not exist, check whether the name
 *          we are looking for is the original QNAME in the query or a name we have followed due
 *          to a CNAME.  If the name is original, set an authoritative name error in the response
 *          and exit.  Otherwise just exit.  (Exit means "return the packet.")
 *          If the * label does exist, match RRs at that node against QTYPE.  If any match,
 *          copy them into the answer section, but set the owner of the RR to be QNAME, and not
 *          the node with the * label.  Go to Step 6.</li>
 *    </ol></li>
 * <li> Start matching down the cache.  We are not currently using a cache in this.</li>
 * <li> Using the local resolver or a copy to anser the query.  We aren't using a special local resolver.</li>
 * <li> Using local data only, attempt to add other RRs which may be useful to the additional section
 *      of the query.  Exit (ie, send the packet.</li>
 * </ol><p>
 * To determine the authorities for a name:
 * <ol>
 * <li>If the name is not fully resolved and has no star record, go up the zones.
 * For each zone, first check for an SOA record and then check for an NS record.
 * If an SOA is found, return the SOA and return a NXDOMAIN.  If an
 * NS is found, return the NS (this means this record is a cut record.)</li>
 * <li>If the name is fully resolved (including star records) it CAN'T be an NXDOM and soit must
 * have an NS record.  Keep going up the zones 'til an NS record is found.</li>
 * </ol><p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander
 * @see <a href="http://www.faqs.org/rfcs/rfc1034.html">RFC 1034</a> */
public final class ChiralDNS {

    private static final Logger LOG = Logger.getLogger(ChiralDNS.class.getName());

    private final Resolver resolver;
    private final Properties resolverProperties;

    private final int PORT;
    private final String bindName;
    private final boolean dnsSpam;

    /** This defines the max length of a dns udp packet, which is defined by the protocol to be... */
    public static final int LENGTH = 512;

    /** Determine whether Named will use response caching.  Response caching
     * makes queries quicker, but the cache never expires entries, so stale
     * entries can remain in the cache. */
    public final static boolean defaultCacheP = false;
    private final boolean cacheP;

    private final DatagramChannel channel;

    /** Release the resources associated with this chiralDNS. */
    public void release() {
	resolver.release();
	try { channel.close(); } catch(IOException ioe) { LOG.warning("Caught ioexception: " + ioe); }
    }

    /** Given a message, compose a response message which answers the query.
     * If we get a null query, return null, because we can't send anything.
     * If there is a format error in the message, try to send an error packet back.
     * If the result is not found, send back an soa packet if possible. */
    private Message makeResponse(Message m, Resolver resolver) throws SQLException, ProtocolException {
	if(m == null) throw new NullPointerException("No message found in makeResponse");
	if(resolver == null) throw new NullPointerException("No resolver found in makeResponse.");
	if(m.getHeader().isQueryResponse()) return null;
	// for now we assume that all queries are QUERY opcode (ie, not reverse)
	// and all are looking only for one A record.  Later add other opcodes
	// and record types.
	Header queryHeader = m.getHeader();
	QuestionSection questionSection = m.getQuestion();
	Name questionName = questionSection.getName();
	// Turn the questioname into a DbName.  This is a win for efficiency, but it does
	// somewhat break the abstraction of the named.  But the speedup is worth it.
	questionName = resolver.getDbName(questionName);
	Name newQuestionName = null;
	int type = questionSection.getType();
	Set answer = resolver.getResourceRecordSet((DbName) questionName, type);
	if(answer.isEmpty()) {
	    ResourceRecord cnameRecord = resolver.getOneResourceRecord((DbName) questionName, Type.CNAME);
	    if(cnameRecord != null) {
		answer.add(cnameRecord);
		Rdata r = cnameRecord.getRdata();
		newQuestionName = ((CNAMERdata) r).getServer();
		if(! (newQuestionName instanceof DbName)) newQuestionName = resolver.getDbName(newQuestionName);
		answer.addAll(resolver.getResourceRecordSet((DbName) newQuestionName, type));
	    }
	}
	Set authorityRecords = resolver.getAuthority((DbName) questionName, answer.isEmpty());
	boolean isAuthoritative = resolver.isAuthoritative((DbName) questionName);
	byte rCode = (((DbName) questionName).isFullyResolved() ||
		      ((DbName) questionName).isStar() ||
		       (! isAuthoritative)) ? Header.RCODE_NOERROR : Header.RCODE_NAMEERROR;
	Header responseHeader = new Header(queryHeader.getID(), true, isAuthoritative, false, false, false,
					   rCode, Header.QUERY);
	// now we need additional records, which basically consist of A records
	// for any name
	Set nameSet = getNonARecordNames(answer);
	nameSet.addAll(getNonARecordNames(authorityRecords));
	Set additionalRecords = new HashSet();
	Iterator it = nameSet.iterator();
	DbName n;
	while(it.hasNext()) {
	    n = (DbName) it.next();
	    additionalRecords.addAll(resolver.getResourceRecordSet(n, Type.A));
	}
	if(dnsSpam & (m.getQuestion().getType() == Type.NS))
	    additionalRecords.addAll(resolver.getResourceRecordSet((DbName) questionName, Type.TXT));
	Message result = new Message(responseHeader, questionSection, answer,
				     authorityRecords, additionalRecords);
	result.updateCounts();
	return result;
    }

    /** Given a set of ResourceRecords, display them all.  */
    public static String showResourceRecordSet(Set s) {
	if(s == null) return "Null set";
	if(s.size() == 0) return "Empty set";
	Iterator it = s.iterator();
	StringBuffer sb = new StringBuffer();
	sb.append("ResourceRecord set of " + s.size() + " elements: ");
	while(it.hasNext())
	    sb.append(it.next() + ";  ");
	return sb.toString();
    }

    /** This method generates a Set of Names which can be used to look up additional records.
     * The additional records section contains A records for all the non-A records in all the 
     * other sections.
     * @param resourceRecordSet contains a set of only ResourceRecords.  No other class should be present. */
    private Set getNonARecordNames(Set resourceRecordSet) {
	if(resourceRecordSet == null) throw new NullPointerException("No resource record set found.");
	if(resourceRecordSet.size() == 0) return new HashSet(); // nothing to do here
	Set result = new HashSet(resourceRecordSet.size());
	Iterator it = resourceRecordSet.iterator();
	Object o; ResourceRecord rr; Name n; Rdata rdata;
	while(it.hasNext()) {
	    o = it.next();
	    if(!(o instanceof ResourceRecord))
		throw new IllegalArgumentException("A member of the ResourceRecord Set wasn't a ResourceRecord: " + o);
	    rr = (ResourceRecord) o;
	    if(rr.getType() == Type.A) continue; // nothing to do if it's already an A record
	    if(rr.getType() == Type.CNAME) continue; // also don't provide an additional record for a CNAME
	    // now we add the names... but which names we add depends on which type of ResourceRecord it is
	    rdata = rr.getRdata();
	    result.addAll(rdata.getNames());
	}
	return result;
    }

    /** Construct a new Named by loading in configuration information. */
    public ChiralDNS() throws IOException, NumberFormatException, SQLException, ClassNotFoundException {
	String fileName = System.getProperty("chiralDNS.NamedSettings");
	if(fileName == null) fileName = "NamedSettings";
	LOG.finest("Loading named settings from file: " + fileName);
	FileInputStream fis = new FileInputStream(fileName);
	Properties properties = new Properties();
	properties.load(fis);
	fis.close();
	String portString = properties.getProperty("portNumber");
	if(portString == null) portString = "53";
	PORT = Integer.parseInt(portString);
	String cacheString = properties.getProperty("cache");
	if(cacheString == null) cacheP = defaultCacheP;
	else cacheP = cacheString.equalsIgnoreCase("yes");
	fileName = System.getProperty("chiralDNS.ResolverSettings");
	if(fileName == null) fileName = "ResolverSettings";
	LOG.finest("Loading resolver settings from this file: " + fileName);
	fis = new FileInputStream(fileName);
	resolverProperties = new Properties();
	resolverProperties.load(fis);
	fis.close();
	resolver = new Resolver(resolverProperties);
	if(resolver == null) throw new IllegalStateException("Couldn't create a resolver.");
	if(PORT == 0) throw new IllegalStateException("Port number couldn't be found.");
	bindName = properties.getProperty("bindName");
	String spamString = properties.getProperty("dnsSpam");
	dnsSpam = ((spamString != null) && spamString.equalsIgnoreCase("yes"));
	channel = DatagramChannel.open();
	InetSocketAddress isa;
	if(bindName == null) isa = new InetSocketAddress(PORT);
	else isa = new InetSocketAddress(InetAddress.getByName(bindName), PORT);
	channel.socket().bind(isa);
	LOG.finest("Datagram channel is open.");
    }

    /** Run a Named thread.  After the loop starts, it will not throw any exceptions,
     * because it should continue resolving even if an exception has occured.  */
    public void run() throws IOException, SocketException {
	// A direct buffer offers the highest IO performance
	ByteBuffer bb = ByteBuffer.allocateDirect(LENGTH);
	SocketAddress sa = null;
	Message m;
	QuestionKey q;
	Map cache = new HashMap();
	byte[] cached;

	while(true) {
	    bb.clear();
	    sa = channel.receive(bb);
	    if(false) { // dump the packets for debugging
		bb.flip();
		LOG.finest("Received a packet from this sa: " + sa);
		LOG.finest("here is the dump:");
		LOG.finest(PacketDumper.decode(bb));
	    }
	    bb.flip();
	    bb.mark();
	    q = NameFactory.getQuestionKey(bb);
	    bb.reset();
	    if(cacheP) {
		if((q != null) && cache.containsKey(q)) {
		    cached = (byte[]) cache.get(q);
		    bb.position(2); // preserve the id (short) in the query
		    bb.limit(cached.length + 2); // there should be a more natural way to do this, right?
		    bb.put(cached);
		    bb.flip();
		    channel.send(bb, sa);
		    continue;
		}
	    }
	    m = Message.getInstance(bb);
	    LOG.finest("Query: " + q.asLogString() +
		       " / " + sa);
	    if(m == null) {
		LOG.finest("Couldn't construct a correct message object for this query; dropping.");
		continue; }
	    if(m.getHeader().isQueryResponse()) {
		LOG.fine("Got a query response instead of a query.");
		continue; }
	    try { m = makeResponse(m, resolver); }
	    catch(SQLException sqe) { LOG.warning("Caught sqlexception: " + sqe); return; }
	    catch(ProtocolException pe) { LOG.warning("Caught protocol exception: " + pe); continue; }
	    if(m == null) continue;
	    m.compress();
	    bb.clear();
	    m.send(bb);
	    bb.flip();
	    if(cacheP) {
		if(q != null) { // add this message result to the cache
		    cached = new byte[bb.limit() - 2];
		    bb.position(2);
		    bb.get(cached);
		    cache.put(q, cached);
		    bb.rewind();
		}
	    }
	    channel.send(bb, sa);
	}
    }
    
    public static void main(String[] args) {
	System.out.println("chiralDNS(tm)");
	System.out.println("Copyright 2001-2017, Eric Hollander.  All rights reserved.");
	for(int i = 0; i < args.length; i++) if(args[i].equalsIgnoreCase("--version")) {
	    System.out.println("Version: " + Version.getVersionString());
	    System.exit(0);
	}
	LOG.info("chiralDNS starting up.");
	ChiralDNS n = null;
	    while(true) {
		try {
		    n = new ChiralDNS();
		    n.run();
		}
		catch(Exception e) {
		    try { n.release(); }
		    catch(Exception ee) {
			LOG.warning("Caught this exception while releasing chiralDNS: " + ee);
		    }
		    LOG.warning("Caught an exception: " + e);
		}
	    }
    }
}
