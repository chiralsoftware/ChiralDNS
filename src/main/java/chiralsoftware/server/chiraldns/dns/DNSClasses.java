package chiralsoftware.server.chiraldns.dns;

/** Definition of DNS classes.  The four possible DNS classes are Internet and three others.
 * Only Internet is in use, and only Internet is supported by chiralDNS(tm).<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class DNSClasses {

    // don't instantiate this class
    private DNSClasses() { }

    /** The Internet class. */
    public static final short IN = 1;

    /** The Chaosnet class.  This class is obsolete except for server version queries. */
    public static final short CH = 3;

    /** The Chaosnet class.  This class is obsolete except for server version queries. */
    public static final short CHAOS = 3;

    /** The Hesiod class.  This class is obsolete. */
    public static final short HESIOD = 4;

}
