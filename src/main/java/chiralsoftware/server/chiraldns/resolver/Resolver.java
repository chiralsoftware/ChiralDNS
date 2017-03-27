package chiralsoftware.server.chiraldns.resolver;

import chiralsoftware.server.chiraldns.dns.ResourceRecord;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.dns.rdata.Rdata;
import chiralsoftware.server.chiraldns.name.DbName;
import chiralsoftware.server.chiraldns.name.Name;
import chiralsoftware.server.chiraldns.name.NameFactory;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Properties;
import java.util.Date;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** This class implements a DNS resolver by querying a db.<p>
 * Copyirght 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Resolver {

    private static final Logger LOG = Logger.getLogger(Resolver.class.getName());

    public static final int MAX_RECORDS = 13; // allow up to 13 records of a given type and zone in a result

    private final Connection db;
    private Database database = null;

    /** Holds the date when this resolver first started life. */
    private final Date startDate = new Date();

    /** Construct a new resolver.  This could include loading a config file and then making
     * a db connection, but for now it will be all hard-coded. */
    public Resolver(Properties resolverProperties) throws SQLException, ClassNotFoundException {
	// it's sql db connection openning time!
	Class.forName(resolverProperties.getProperty("driverClass"));
	//db = DriverManager.getConnection("jdbc:postgresql:domain", "hh", "");
	db = DriverManager.getConnection(resolverProperties.getProperty("connectionString"),
					 resolverProperties.getProperty("userName"),
					 resolverProperties.getProperty("userPassword"));
	if(db == null) { LOG.severe("Couldn't open db connection in init."); System.exit(1); }
	database = new Database(db);
    }

    public Resolver(Connection db) throws SQLException {
	if(db == null) throw new NullPointerException("Connection was null.");
	if(db.isClosed()) throw new IllegalArgumentException("Database was closed.");
	this.db = db;
	database = new Database(db);
    }

    /** Release resources associated with this resolver. */
    public void release() { 
	database.release();
	try { db.close(); } catch(SQLException e) { LOG.warning("Releasing db, caught: " + e); }
    }

    /** Return some stats about the resolver's database. */
    public ResolverStatistics getStatistics() throws SQLException {
	return database.getStatistics(startDate); }

    /** Given a DbName, return a set of nameserver ResourceRecords which are appropriate for it.
     * This is done by going up the labels (from more specific towards less specific) and 
     * looking for any NS records.
     * @throws NullPointerException if the name is null.
     * @throws SQLException */
    public Set getNSRecordSet(DbName name) throws SQLException {
	if(name == null) throw new NullPointerException("Can't resolve a null name.");
	// if the dbname is not fully resolved, we can start iterating up from the last label which is in the db.
	Set rdataSet = null;
	DbName fullyResolved = name.getFullyResolvedName();
	while((rdataSet == null) || rdataSet.isEmpty()) {
	    rdataSet = database.getRdata(fullyResolved, Type.NS);
	    if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, fullyResolved);
	    if(fullyResolved.isRoot()) return Collections.EMPTY_SET;
	    fullyResolved = fullyResolved.subName();
	}
	// this should never get there
	return new HashSet();
    }	

    /** Get the authority records for a given name. */
    public Set getAuthority(DbName name) throws SQLException { return getAuthority(name, false); }

    /** Return a set of Authority records for the given name.
     * Here is the algorithm:<p>
     * <ol>
     * <li>If the name is not fully resolved and has no star record, go up the zones.
     * For each zone, first check for an SOA record and then check for an NS record.
     * If an SOA is found, return the SOA and return a NXDOMAIN.  If an
     * NS is found, return the NS (this means this record is a cut record.)</li>
     * <li>If the name is fully resolved (including star records) it CAN'T be an NXDOM and so it must
     * have an NS record.  Keep going up the zones 'til an NS record is found.</li>
     * </ol><p>
     * @param noAnswer Set this to true if the query produced an empty answer set.
     * For some reason, BIND returns SOA records only on a label that does exist
     * if there are no records of the requested type for the zone, unless that zone has an NS record. */
    public Set getAuthority(DbName name, boolean noAnswer) throws SQLException {
	if(name == null) throw new NullPointerException("Can't getAuthority on a null name.");
	if(name.isStar() && noAnswer) {
	    // in this case, the name is fully resolved, so there must be an soa
	    // so go up the tree and look for that
	    name = name.getFullyResolvedName();
	    Set rdataSet;
	    while(!name.isRoot()) {
		rdataSet = database.getRdata(name, Type.SOA);
		if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, name);
		name = name.subName();
	    }
	    // we shouldn't get here but if we do...
	    LOG.warning("getAuthority in SOAOnly section shouldn't have gotten here.");
	    return Collections.EMPTY_SET;
	}

	if(noAnswer) {
	    name = name.getFullyResolvedName();
	    Set rdataSet;
	    rdataSet = database.getRdata(name, Type.NS);
	    if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, name);
	    while(!name.isRoot()) {
		rdataSet = database.getRdata(name, Type.SOA);
		if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, name);
		name = name.subName();
	    }
	    // we shouldn't get here but if we do...
	    LOG.warning("getAuthority in SOAOnly section shouldn't have gotten here.");
	    return Collections.EMPTY_SET;
	}
	Set rdataSet;
	if(name.getZoneNumberCount() == 0) // this hasn't been resolved at all, so return the root NS servers
	    return makeResourceRecordSet(database.getRdata(0, Type.NS), name);
	if(name.isFullyResolved() || name.isStar()) {
	    // in this case we must be looking for a NS record, not SOA.
	    // any name which fully exists in the db exists and has an NS record.
	    while(!name.isRoot()) {
		rdataSet = database.getRdata(name, Type.NS);
		if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, name);
		name = name.subName();
	    }
	    // we should never get here
	    LOG.warning("Warning! Went too far in Resolver.getAuthority.");
	    return makeResourceRecordSet(database.getRdata(0, Type.NS), name);
	} else {
	    // in this case the name is not fully resolved and has no star record,
	    // so go up the zones.  first look for an soa, then an ns record
	    name = name.getFullyResolvedName();
	    while(!name.isRoot()) {
		rdataSet = database.getRdata(name, Type.SOA);
		if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, name);
		rdataSet = database.getRdata(name, Type.NS);
		if(!rdataSet.isEmpty()) return makeResourceRecordSet(rdataSet, name);
		name = name.subName();
	    }
	    // should never get here
	    LOG.warning("Warning! Went too far in Resolver.getAuthority.");
	    return makeResourceRecordSet(database.getRdata(0, Type.NS), name);
	}
    }
		
    /** Given a name, find out if this server is a Source of Authority for that name.
     * Algorithm for this:<p>
     * If the name is fully-resolved or a star record, keep going up the resolved labels.
     * If a SOA record is found, this is authoritative.  If an NS record is found, this
     * is a cut, and so this answer is not authoritative. */
    public boolean isAuthoritative(DbName name) throws SQLException {
	if(name == null) throw new NullPointerException("Cannot find isAuthoritative() for a null name.");
	// if((! name.isFullyResolved()) && (! name.isStar())) return false;
	name = name.getFullyResolvedName();
	if(name.isRoot()) // if this is a root server, there should be an soa for the root record
	    return ! database.getRdata(0, Type.SOA).isEmpty();
	Set s = null;
	while(! name.isRoot()) {
	    s = database.getRdata(name, Type.SOA); // this should never return a null
	    if(! s.isEmpty()) return true;
	    s = database.getRdata(name, Type.NS); // this should never return null
	    if(! s.isEmpty()) return false;
	    name = name.subName();
	}
	// should never get here
	LOG.warning("Warning! Went too far in Resolver.isAuthoritative.");
	return false;
    }

    /** Given a Name, make it into a corresponding DbName.  It is efficient to do this once
     * and then do all the queries based on this name.
     * @throws NullPointerException if the specified Name is null. */
    public DbName getDbName(Name n) throws SQLException {
	if(n == null) throw new NullPointerException("Name was null.");
	if(n instanceof DbName) {
	    LOG.warning("The provided name is already a dbname: " + n + " so this doesn't make sense.");
	    return (DbName) n;
	}
	return database.getDbName(n);
    }

    /** Get exactly one resource record of the given type.  If there are more than
     * one of that type, one will be selected at random.  Some records, such as CNAME
     * records, should never have more than one for a given zone.  This method 
     * is particularly useful for those types.  This method returns null if no resource record
     * is found.
     * @see <a href="http://www.faqs.org/rfcs/rfc2181.html">RFC 2181</a> */
    public ResourceRecord getOneResourceRecord(DbName zone, int type) throws SQLException {
	if(zone == null) throw new NullPointerException("Can't getOneResourceRecord for a null zone.");
	if(type == Type.ANY) throw new IllegalArgumentException("Can't getOneResourceRecord for type = any");
	Set rdataSet = database.getRdata(zone.getNumber(), type, 1);
	if(rdataSet.isEmpty()) return null;
	Iterator it = rdataSet.iterator();
	if(! it.hasNext()) return null;
	return new ResourceRecord(zone, (Rdata) it.next());
    }

    public Set getResourceRecordSet(DbName zone, int type) throws SQLException {
	return getResourceRecordSet(zone, type, MAX_RECORDS);
    }

    /** Fully resolve a name into a Set of ResourceRourceRecords of the appropriate type
     * for the Answer section.  This method is the workhorse of the Resolver class.
     * If the query is for a type which is not supported, return an empty set,
     * which mimics the behavior of BIND. */
    public Set getResourceRecordSet(DbName zone, int type, int limit) throws SQLException {
	if(zone == null) throw new NullPointerException("Can't resolve a null zone.");
	if(type == Type.ANY) {
	    if(! zone.isFullyResolved()) return Collections.EMPTY_SET;
	    Set rdataSet = new HashSet();
	    rdataSet.addAll(database.getRdata(zone.getNumber(), Type.A, limit));
	    rdataSet.addAll(database.getRdata(zone.getNumber(), Type.MX, limit));	    
	    rdataSet.addAll(database.getRdata(zone.getNumber(), Type.NS, limit));	    
	    rdataSet.addAll(database.getRdata(zone.getNumber(), Type.CNAME, limit));	    
	    return makeResourceRecordSet(rdataSet, zone);
	}
	if(! Type.isSupported(type)) return Collections.EMPTY_SET;
	Set rdataSet;
	if(! zone.isFullyResolved()) {
	    if(database.isStarZone(zone.getNumber())) {
		rdataSet = database.getRdata(zone.getNumber(), type, limit);
	    } else {
		rdataSet = new HashSet();
	    }
	} else { // zone is fully resolved
	    rdataSet = database.getRdata(zone.getNumber(), type, limit);
	}
	return makeResourceRecordSet(rdataSet, zone);
    }

    /** Get the set of authorities (name servers) ResourceRecords for this zone.  This
     * will never return an empty set, because it should always be able to return the
     * root hints if nothing else. */
/*    public Set getAuthority(DbName zone) throws SQLException {
	if(zone == null) throw new NullPointerException("Can't getAuthority for a null zone.");
	Set rdata = new HashSet();
	int[] zoneNumbers = zone.getZoneNumbers();
	int i;
	for(i = zoneNumbers.length - 1; i >= 0; i--) {
	    rdata = database.getRdata(zoneNumbers[i], Type.NS);
	    if(!rdata.isEmpty()) break;
	}
	// we need to make up a new name which contains only the labels that
	// were matched, to make up the authority RRs.
	Name newName = zone.subName(i);
	if(rdata.isEmpty()) { // no authorities were found, so get the NS records for "."
	    rdata = database.getRdata(0, Type.NS);
	    return makeResourceRecordSet(rdata, Name.ROOT);
	}
	return makeResourceRecordSet(rdata, newName);
	}*/

    /** Given a set of Rdata and a Name, return an equivalent set of resource records. */
    public Set makeResourceRecordSet(Set rdataSet, Name name) {
	if(rdataSet == null)
	    throw new NullPointerException("attempting to makeResourceRecordSet with null rdata set.");
	if(name == null)
	    throw new NullPointerException("attempting to make resourcerecord set with null name.");
	if(rdataSet.size() == 0) return new HashSet();
	Set result = new HashSet(rdataSet.size());
	Iterator it = rdataSet.iterator();
	while(it.hasNext())
	    result.add(new ResourceRecord(name, (Rdata) it.next()));
	return result;
    }		       

    /** This method allows a new record to be added to a particular zone. */
    public int addRdata(int zoneNumber, Rdata rdata) throws IllegalArgumentException, SQLException {
	if(zoneNumber < 0) throw new IllegalArgumentException("invalid zone number: " + zoneNumber);
	if(rdata == null) throw new NullPointerException("rdata is null.");
	return database.addRdata(zoneNumber, rdata);
    }

    /** Given a name, make sure it is present in the database, and if it is not, add
     * whichever labels are necessary, and return the result as a new DbName.
     * This method is only used by various administration utilities, for loading zone files.
     * The star argument specifies whether this is a star zone or not. */
    public DbName addName(Name n, boolean star) throws SQLException {
	if(n == null) throw new NullPointerException("Can't add a null name.");
	if(n.isRoot()) return DbName.ROOT;
	DbName existing;
	if(n instanceof DbName) existing = (DbName) n;
	else existing = getDbName(n);
	if(existing.isFullyResolved()) return existing;
	// find out how many labels we need to add here
	int zoneCount = existing.getZoneNumberCount();
	String[] labels = existing.getLabels();
	int[] newZones = new int[labels.length];
	boolean[] newStars = new boolean[labels.length];
	for(int i = 0; i < zoneCount; i++) newZones[i] = existing.getZoneNumber(i);
	// now go through and add these new labels
	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// make sure that existing.getNumber can return 0 if there is no stored previous node in the db....
	int lastNode = existing.getNumber();
	for(int i = zoneCount; i < labels.length; i++) {
	    newZones[i] = database.createNode(labels[i], lastNode, (i == labels.length - 1) & star);
	    lastNode = newZones[i];
	}
	for(int i = 0; i < zoneCount; i++) newStars[i] = existing.isStar(i);
	for(int i = zoneCount; i < newStars.length; i++) newStars[i] = false;
	if(newStars.length > 0) newStars[newStars.length - 1] = star;
	return NameFactory.getDbName(labels, newZones, newStars);
    }
}
