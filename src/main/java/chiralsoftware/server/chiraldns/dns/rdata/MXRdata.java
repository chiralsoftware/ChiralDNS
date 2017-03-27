package chiralsoftware.server.chiraldns.dns.rdata;

import chiralsoftware.server.chiraldns.dns.Compressor;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.name.DbName;
import chiralsoftware.server.chiraldns.name.Name;
import java.util.Set;
import java.util.Collections;
import java.nio.ByteBuffer;


/** MX records are mail exchange records, allowing a name server to delegate
 * a particular host as the mail receiving host for a domain.
 * For example, an MX record for yahoo.com might specify mail.yahoo.com
 * as the host to send anyone@yahoo.com to.  MX records contain the name
 * of the server, and also a priority number.  This way if one mail server
 * is down, mail can go to other servers of lower priority.  The 
 * lower the priority number (closer to zero), the higher the priority.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class MXRdata extends Rdata {

    /** This int holds the preference.  It is a 16-bit unsigned int,
     * which we represent as a 32 bit int. */
    private final int preference;

    /** Holds the zone which is the exchange. */
    private Name exchange;

    /** It is very important to send the correct length with the rdata.  For an MX record
     * the length is calculated like this: The exchange preference is always 2 bytes.
     * Then we add in the wire length of the name. */
    public int length() {
	if(exchange == null) return 0; // shouldn't happen
	return exchange.length() + 2;
    }

    /** Create a new MXRdata object with a name and a preference. */
    public MXRdata(int ttl, Name exchange, int preference) {
	super(ttl);
	this.exchange = exchange;
	this.preference = preference;
    }

    /** Get the preference value */
    public int getPreference() { return preference; }

    /** Get the zone number of the exchange. */
    public int getExchangeNumber() {
	if(exchange instanceof DbName) return ((DbName) exchange).getNumber();
	else return 0;
    }

    /** return the Exchange as a name.  Note that this might be compressed. */
    public Name getExchange() { return exchange; }

    public void send(ByteBuffer bb) {
	bb.putShort((short) preference);
	exchange.send(bb);
    }

    /** Required from abstract class.  The offset is a pointer to the first byte of the Rdata
     * section.  The first two bytes are taken by the unsigned short preference, and then the
     * compressed name starts. */
    public int compress(Compressor compressor, int offset) {
	assert(exchange != null);
	exchange = compressor.compress(exchange, offset + 2);
	return offset + exchange.length() + 2;
    }

    public String toString() { return "MX exchange: (" + ttl +"): " +
				   exchange + ", pref: " + preference; }

    public int getType() { return Type.MX; }

    /** Return the name of the exchange. */
    public Set getNames() { 
	return Collections.singleton(exchange);
    }

    public boolean equals(Object o) {
	if(o == null) return false;
	if(!(o instanceof MXRdata)) return false;
	return (preference == ((MXRdata) o).preference) &&
	    exchange.equals(((MXRdata) o).exchange);
    }

    public int hashCode() {
	return preference + exchange.hashCode();
    }
}
