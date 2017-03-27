package chiralsoftware.server.chiraldns.dns;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/** Constants for various record types in DNS.  These values are defined
 * in the RFC.  See also: <i>DNS and BIND</i> by Paul Albitz and Cricket Lieu.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Type {

    /** This class cannot be instantiated. */
    private Type() { }

    /** Address */
    public static final short A = 1;

    /** Name server */
    public static final short NS = 2;

    /** Mail destination */
    public static final short MD = 3;

    /** Mail forwarder */
    public static final short MF = 4;

    /** Canonical name (alias) */
    public static final short CNAME = 5;

    /** Start of authority */
    public static final short SOA = 6;

    /** Mailbox domain name */
    public static final short MB = 7;

    /** Mail group member */
    public static final short MG = 8;

    /** Mail rename name */
    public static final short MR = 9;

    /** Null record */
    public static final short NULL = 10;

    /** Well known services */
    public static final short WKS = 11;

    /** Domain name pointer */
    public static final short PTR = 12;

    /** Host information */
    public static final short HINFO = 13;

    /** Mailbox information */
    public static final short MINFO = 14;

    /** Mail routing information */
    public static final short MX = 15;

    /** Text strings */
    public static final short TXT = 16;

    /** Responsible person */
    public static final short RP = 17;

    /** AFS cell database */
    public static final short AFSDB = 18;

    /** X_25 calling address */
    public static final short X25 = 19;

    /** ISDN calling address */
    public static final short ISDN = 20;

    /** Router */
    public static final short RT = 21;

    /** NSAP address */
    public static final short NSAP = 22;

    /** Reverse NSAP address (deprecated) */
    public static final short NSAP_PTR = 23;

    /** Signature */
    public static final short SIG = 24;

    /** Key */
    public static final short KEY = 25;

    /** X.400 mail mapping */
    public static final short PX = 26;

    /** Geographical position (withdrawn) */
    public static final short GPOS = 27;

    /** IPv6 address (old) */
    public static final short AAAA = 28;

    /** Location */
    public static final short LOC = 29;

    /** Next valid name in zone */
    public static final short NXT = 30;

    /** Endpoint identifier */
    public static final short EID = 31;

    /** Nimrod locator */
    public static final short NIMLOC = 32;

    /** Server selection */
    public static final short SRV = 33;

    /** ATM address */
    public static final short ATMA = 34;

    /** Naming authority pointer */
    public static final short NAPTR = 35;

    /** Key exchange */
    public static final short KX = 36;

    /** Certificate */
    public static final short CERT = 37;

    /** IPv6 address */
    public static final short A6 = 38;

    /** Non-terminal name redirection */
    public static final short DNAME = 39;

    /** Options - contains EDNS metadata */
    public static final short OPT = 41;

    /** Transaction key - used to compute a shared secret or exchange a key */
    public static final short TKEY = 249;

    /** Transaction signature */
    public static final short TSIG = 250;

    /** Incremental zone transfer */
    public static final short IXFR = 251;

    /** Zone transfer */
    public static final short AXFR = 252;

    /** Transfer mailbox records */
    public static final short MAILB = 253;

    /** Transfer mail agent records */
    public static final short MAILA = 254;

    /** Matches any type */
    public static final short ANY = 255;

    /** A map from type numbers to string names.  This map is immutable. */
    public static Map types;

    /** The reverse: a map from type names to numbers.  This map is immutable. */
    public static Map reverseTypes;

    /** A list of the various real query types which ChiralDNS supports.  Only
     * a small subset of query types are actually in use, and ChiralDNS only supports
     * these types.  More types could easily be added if the need arises.  This set is immutable. */
    public static Set supportedQueryTypes;

    static {
	Map m = new HashMap(47);
	m.put(new Short(A), "A");
	m.put(new Short(NS), "NS");
	m.put(new Short(MD), "MD");
	m.put(new Short(MF), "MF");
	m.put(new Short(CNAME), "CNAME");
	m.put(new Short(SOA), "SOA");
	m.put(new Short(MB), "MB");
	m.put(new Short(MG), "MG");
	m.put(new Short(MR), "MR");
	m.put(new Short(NULL), "NULL");
	m.put(new Short(WKS), "WKS");
	m.put(new Short(PTR), "PTR");
	m.put(new Short(HINFO), "HINFO");
	m.put(new Short(MINFO), "MINFO");
	m.put(new Short(MX), "MX");
	m.put(new Short(TXT), "TXT");
	m.put(new Short(RP), "RP");
	m.put(new Short(AFSDB), "AFSDB");
	m.put(new Short(X25), "X25");
	m.put(new Short(ISDN), "ISDN");
	m.put(new Short(RT), "RT");
	m.put(new Short(NSAP), "NSAP");
	m.put(new Short(NSAP_PTR), "NSAP_PTR");
	m.put(new Short(SIG), "SIG");
	m.put(new Short(KEY), "KEY");
	m.put(new Short(PX), "PX");
	m.put(new Short(GPOS), "GPOS");
	m.put(new Short(AAAA), "AAAA");
	m.put(new Short(LOC), "LOC");
	m.put(new Short(NXT), "NXT");
	m.put(new Short(EID), "EID");
	m.put(new Short(NIMLOC), "NIMLOC");
	m.put(new Short(SRV), "SRV");
	m.put(new Short(ATMA), "ATMA");
	m.put(new Short(NAPTR), "NAPTR");
	m.put(new Short(KX), "KX");
	m.put(new Short(CERT), "CERT");
	m.put(new Short(A6), "A6");
	m.put(new Short(DNAME), "DNAME");
	m.put(new Short(OPT), "OPT");
	m.put(new Short(TKEY), "TKEY");
	m.put(new Short(TSIG), "TSIG");
	m.put(new Short(IXFR), "IXFR");
	m.put(new Short(AXFR), "AXFR");
	m.put(new Short(MAILB), "MAILB");
	m.put(new Short(MAILA), "MAILA");
	m.put(new Short(ANY), "ANY");
	types = Collections.unmodifiableMap(m);

	m = new HashMap(47);
	m.put("A", new Short(A));
	m.put("NS", new Short(NS));
	m.put("MD", new Short(MD));
	m.put("MF", new Short(MF));
	m.put("CNAME", new Short(CNAME));
	m.put("SOA", new Short(SOA));
	m.put("MB", new Short(MB));
	m.put("MG", new Short(MG));
	m.put("MR", new Short(MR));
	m.put("NULL", new Short(NULL));
	m.put("WKS", new Short(WKS));
	m.put("PTR", new Short(PTR));
	m.put("HINFO", new Short(HINFO));
	m.put("MINFO", new Short(MINFO));
	m.put("MX", new Short(MX));
	m.put("TXT", new Short(TXT));
	m.put("RP", new Short(RP));
	m.put("AFSDB", new Short(AFSDB));
	m.put("X25", new Short(X25));
	m.put("ISDN", new Short(ISDN));
	m.put("RT", new Short(RT));
	m.put("NSAP", new Short(NSAP));
	m.put("NSAP_PTR", new Short(NSAP_PTR));
	m.put("SIG", new Short(SIG));
	m.put("KEY", new Short(KEY));
	m.put("PX", new Short(PX));
	m.put("GPOS", new Short(GPOS));
	m.put("AAAA", new Short(AAAA));
	m.put("LOC", new Short(LOC));
	m.put("NXT", new Short(NXT));
	m.put("EID", new Short(EID));
	m.put("NIMLOC", new Short(NIMLOC));
	m.put("SRV", new Short(SRV));
	m.put("ATMA", new Short(ATMA));
	m.put("NAPTR", new Short(NAPTR));
	m.put("KX", new Short(KX));
	m.put("CERT", new Short(CERT));
	m.put("A6", new Short(A6));
	m.put("DNAME", new Short(DNAME));
	m.put("OPT", new Short(OPT));
	m.put("TKEY", new Short(TKEY));
	m.put("TSIG", new Short(TSIG));
	m.put("IXFR", new Short(IXFR));
	m.put("AXFR", new Short(AXFR));
	m.put("MAILB", new Short(MAILB));
	m.put("MAILA", new Short(MAILA));
	m.put("ANY", new Short(ANY));
	reverseTypes = Collections.unmodifiableMap(m);

	Set s = new HashSet(6);
	s.add(new Short(A));
	s.add(new Short(CNAME));
	// s.add(new Short(HINFO));
	s.add(new Short(MX));
	s.add(new Short(NS));
	// s.add(new Short(PTR));
	s.add(new Short(SOA));
	s.add(new Short(TXT));
	supportedQueryTypes = Collections.unmodifiableSet(s);
    }

    /** Get the string value of a type integer. */
    public static String string(short i) {
	if(types == null) return null; // should never happen
	String s = (String) types.get(new Short(i));
	return (s != null) ? s : ("TYPE " + i);
    }

    /** Lookup the numeric type value of a string. */
    public static short value(String s) {
	if(s == null) throw new NullPointerException("Can't get the value of a null string.");
	assert(reverseTypes != null);
	Short i = (Short) reverseTypes.get(s.toUpperCase());
	if(i == null) return -1;
	return i.shortValue();
    }

    /** Is this a supported type or not? */
    public static boolean isSupported(String s) {
	Short i = new Short(value(s));
	if(i.shortValue() == -1) return false;
	return supportedQueryTypes.contains(i);
    }

    /** Is this a supported type or not? */
    public static boolean isSupported(int i) {
	Short s = new Short((short) i);
	return supportedQueryTypes.contains(s);
    }
}
