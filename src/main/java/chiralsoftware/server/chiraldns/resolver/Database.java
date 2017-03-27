package chiralsoftware.server.chiraldns.resolver;

import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.dns.rdata.ARdata;
import chiralsoftware.server.chiraldns.dns.rdata.CNAMERdata;
import chiralsoftware.server.chiraldns.dns.rdata.MXRdata;
import chiralsoftware.server.chiraldns.dns.rdata.NSRdata;
import chiralsoftware.server.chiraldns.dns.rdata.Rdata;
import chiralsoftware.server.chiraldns.dns.rdata.SOARdata;
import chiralsoftware.server.chiraldns.dns.rdata.TXTRdata;
import chiralsoftware.server.chiraldns.name.DbName;
import chiralsoftware.server.chiraldns.name.Name;
import chiralsoftware.server.chiraldns.name.NameFactory;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;

/** This class implements the database connection for the dns server.<p>
 * This class is not thread-safe.  It has private members which are PreparedStatements
 * which cannot be used by more than one thread at a time.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private final Connection db;

    private final PreparedStatement findNextNodePreparedStatement;
    private final PreparedStatement findDbNamePreparedStatement;
    private final Map getResultSetStatements;

    /** Create a new Database given a connection. */
    public Database(Connection db) throws SQLException {
	if(db == null) throw new NullPointerException("Can't create a new database with a null link.");
	this.db = db;
	String qstring = "SELECT number, star FROM zone WHERE name = ? AND previous = ?";
	findNextNodePreparedStatement = db.prepareStatement(qstring);
	qstring = "SELECT name, previous, star FROM zone WHERE number = ?";
	findDbNamePreparedStatement = db.prepareStatement(qstring);

	int limit = 10;

	// now set up the map of prepared statements for getResultSet
	Map m = new HashMap(6);
/*	m.put(new Integer(Type.A),
	      db.prepareStatement("SELECT ttl, addr FROM arecord WHERE zone = ? ORDER BY RANDOM() LIMIT " + limit));
	m.put(new Integer(Type.MX),
	      db.prepareStatement("SELECT ttl, preference, exchange FROM mxrecord " +
				  "WHERE zone = ? ORDER BY RANDOM() LIMIT " + limit));
	m.put(new Integer(Type.NS),
	      db.prepareStatement("SELECT ttl, name FROM nsrecord WHERE zone = ? ORDER BY RANDOM() LIMIT " + limit));
	m.put(new Integer(Type.CNAME),
	      db.prepareStatement("SELECT ttl, name FROM cnamerecord WHERE zone = ? ORDER BY RANDOM() LIMIT " + limit));
	m.put(new Integer(Type.TXT),
	      db.prepareStatement("SELECT ttl, text FROM txtrecord WHERE zone = ? ORDER BY RANDOM() LIMIT " + limit));
	m.put(new Integer(Type.SOA),
	      db.prepareStatement("SELECT mname, rname, ser, refresh, retry, expire, minimum FROM soarecord " +
				  "WHERE zone = ? ORDER BY RANDOM() LIMIT " + limit));
*/

	m.put(new Integer(Type.A),
	      db.prepareStatement("SELECT ttl, addr FROM arecord WHERE zone = ? LIMIT " + limit));
	m.put(new Integer(Type.MX),
	      db.prepareStatement("SELECT ttl, preference, exchange FROM mxrecord " +
				  "WHERE zone = ? LIMIT " + limit));
	m.put(new Integer(Type.NS),
	      db.prepareStatement("SELECT ttl, name FROM nsrecord WHERE zone = ? LIMIT " + limit));
	m.put(new Integer(Type.CNAME),
	      db.prepareStatement("SELECT ttl, name FROM cnamerecord WHERE zone = ? LIMIT " + limit));
	m.put(new Integer(Type.TXT),
	      db.prepareStatement("SELECT ttl, text FROM txtrecord WHERE zone = ? LIMIT " + limit));
	m.put(new Integer(Type.SOA),
	      db.prepareStatement("SELECT mname, rname, ser, refresh, retry, expire, minimum FROM soarecord " +
				  "WHERE zone = ? LIMIT " + limit));
	getResultSetStatements = Collections.unmodifiableMap(m);
    }

    /** Release resources associated with this database. */
    public void release() {
	try { findNextNodePreparedStatement.close(); } catch(SQLException sqe) { }
	try { findDbNamePreparedStatement.close(); } catch(SQLException sqe) { }
	try { db.close(); } catch(SQLException sqe) { }
    }

    /** Given a Name, turn it into a DbName by resolving as many labels as it can
     * into zones in the db.  If a DbName is given as the argument, it will construct a new DbName
     * by the same process.  */
    public DbName getDbName(Name name) throws SQLException {
	assert(db != null); assert(! db.isClosed());
	if(name == null) throw new NullPointerException("Can't lookup a null name.");
	if(name.equals(Name.ROOT)) return DbName.ROOT;
	int[] labelNumbers = new int[name.count()];
	boolean[] stars = new boolean[name.count()];
	Node node = Node.ROOT;
	int i;
	Node lastNode;
	for(i = 0; i < labelNumbers.length; i++) {
	    node = findNextNode(name.get(i), node);
	    if(node == null) break;
	    labelNumbers[i] = node.getNumber();
	    stars[i] = node.isStar();
	    lastNode = node;
	}
	int[] result = new int[i];
	boolean[] starResult = new boolean[i];
	for(i = 0; i < result.length; i++) result[i] = labelNumbers[i];
	for(i = 0; i < starResult.length; i++) starResult[i] = stars[i];
	return NameFactory.createDbNameFromName(name, result, starResult);
    }

    /** Given a string, and a previous node, find the node associated with the string. */
    private Node findNextNode(String s, Node n) throws SQLException {
	assert(db != null); assert(! db.isClosed());
	if(s == null) throw new NullPointerException("Cannot look up a null label.");
	if((s.length() == 0) || (s.length() > 63))
	    throw new IllegalArgumentException("String: " + s + " is not a valid name.");
	if(n == null) throw new NullPointerException("Node is null in findNextNode");
	Node result = null;
	String qstring = "SELECT number, star FROM zone WHERE name = ? AND previous = ?";
//	PreparedStatement ps = db.prepareStatement(qstring);
	findNextNodePreparedStatement.setString(1, s);
	findNextNodePreparedStatement.setInt(2, n.getNumber());
	// ps.setString(1, s); ps.setInt(2, n.getNumber());
//	ResultSet rs = ps.executeQuery();
	ResultSet rs = findNextNodePreparedStatement.executeQuery();
	if(rs.next())
	    result = Node.getInstance(rs.getInt(1), s, n.getNumber(), rs.getBoolean(2));
	rs.close(); findNextNodePreparedStatement.clearParameters();
	return result;
    }

    /** This is a helper method for getStatistics. */
    private int getCount(String qstring) throws SQLException {
	assert(db != null); assert(! db.isClosed());
	if(qstring == null) throw new IllegalArgumentException("can't query a null string");
	Statement st = db.createStatement();
	ResultSet rs = st.executeQuery(qstring);
	int result = 0;
	if(rs.next()) result = rs.getInt(1);
	rs.close(); st.close();
	return result;
    }

    /** Return a new ResolverStatistics object reflecting the current state of the resolver. */
    public ResolverStatistics getStatistics(Date startDate) {
	try {
	    ResolverStatistics result =
		new ResolverStatistics(startDate,
				       getCount("SELECT COUNT(number) FROM zone"),
				       getCount("SELECT COUNT(number) FROM arecord"),
				       getCount("SELECT COUNT(number) FROM mxrecord"),
				       getCount("SELECT COUNT(number) FROM soarecord"));
	    return result;
	}
	catch(SQLException sqe) {
	    LOG.warning("caught sql exception in getStatistics: " + sqe.getMessage());
	    return new ResolverStatistics();
	}
    }

    /** Given a zone number and a type, make up a ResultSet that consists of the corresponding rows.
     * This should not be used for the ANY type. */
/*    private ResultSet getResultSet(int number, int type, int limit) throws SQLException {
	String qstring = null;
	if(type == Type.A)
	    qstring = "SELECT ttl, addr FROM arecord WHERE zone = " + number + " ORDER BY RANDOM() LIMIT " + limit;
	else if(type == Type.MX)
	    qstring = "SELECT ttl, preference, exchange FROM mxrecord WHERE zone = " + number +
		" ORDER BY RANDOM() LIMIT " + limit;
	else if(type == Type.NS)
	    qstring = "SELECT ttl, name FROM nsrecord WHERE zone = " + number +
		" ORDER BY RANDOM() LIMIT " + limit;
	else if(type == Type.CNAME)
	    qstring = "SELECT ttl, name FROM cnamerecord WHERE zone = " + number +
		" ORDER BY RANDOM() LIMIT " + limit;
	else if(type == Type.TXT)
	    qstring = "SELECT ttl, text FROM txtrecord WHERE zone = " + number +
		" ORDER BY RANDOM() LIMIT " + limit;
	else if(type == Type.SOA)
	    qstring = "SELECT mname, rname, ser, refresh, retry, expire, minimum FROM soarecord " +
		"WHERE zone = " + number + " ORDER BY RANDOM() LIMIT " + limit;
	else qstring = null;
	if(qstring == null) { log.info("query type: " + type + " not supported"); return null; }
	PreparedStatement ps = db.prepareStatement(qstring);
	ResultSet result = ps.executeQuery();
	return result;
	}*/

    /** Given a zone number and a type, make up a ResultSet that consists of the corresponding rows.
     * This should not be used for the ANY type. */
    private ResultSet getResultSet(int number, int type, int limit) throws SQLException {
	PreparedStatement ps = (PreparedStatement) getResultSetStatements.get(new Integer(type));
	ps.setInt(1, number);
	ResultSet result = ps.executeQuery();
	return result;
    }

    /** Given a result set and a record type, load it up */
    public Rdata getNextResult(ResultSet rs, int type) throws SQLException {
	if(rs == null) throw new IllegalArgumentException("can't work with null result set");
	Rdata result = null;
	if(type == Type.A)
	    result = new ARdata(rs.getInt(1), rs.getInt(2));
	else if(type == Type.MX) {
	    Name n = findDbName(rs.getInt(3));
	    result = new MXRdata(rs.getInt(1), n, rs.getInt(2));
	} else if(type == Type.NS) {
	    Name n = findDbName(rs.getInt(2));
	    result = new NSRdata(rs.getInt(1), n);
	} else if(type == Type.CNAME) {
	    Name n = findDbName(rs.getInt(2));
	    result = new CNAMERdata(rs.getInt(1), n);
	} else if(type == Type.TXT) {
	    result = new TXTRdata(rs.getInt(1), rs.getString(2));
	} else if(type == Type.SOA) {
	    Name mname = findDbName(rs.getInt(1));
	    Name rname = findDbName(rs.getInt(2));
	    result = new SOARdata(mname, rname, rs.getInt(3), 
				  rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7));
	} else result = null;
	if(result == null) throw new IllegalArgumentException("type: " + type + " is not supported");
	return result;
    }

    /** Given a name and a record type, return a set of all the matching Rdata elements.
     * Note that this does not attempt to deal with ANY type records. */
    public Set getRdata(Name name, int type) throws SQLException {
	if(name == null) throw new IllegalArgumentException("getRdata: can't get from null name.");
	DbName dbName = null;
	if(name instanceof DbName) dbName = (DbName) name;
	else dbName = getDbName(name);
	int zoneNumber = dbName.getNumber();
	return getRdata(zoneNumber, type);
    }
    
    public Set getRdata(int zoneNumber, int type) throws SQLException {
	return getRdata(zoneNumber, type, Resolver.MAX_RECORDS);
    }

    /** Given a zone number, get a set of the Rdata of the given type which match this zone number.
     * If the type is not supported by this server, return an empty hash set.  This mimics
     * the behavior of BIND. */
    public Set getRdata(int zoneNumber, int type, int limit) throws SQLException {
	if(zoneNumber < 0) throw new IllegalArgumentException("invalid zone number in getRdata");
	if(! Type.isSupported(type)) return Collections.EMPTY_SET;
	Set result = new HashSet();
	ResultSet rs = getResultSet(zoneNumber, type, limit);
	while(rs.next()) result.add(getNextResult(rs, type));
	return result;
    }

    /** Determine whether a given zone number is a star label or an ordinary label. */
    public boolean isStarZone(int zoneNumber) throws SQLException {
	String qstring = "SELECT star FROM zone WHERE number = " + zoneNumber;
	PreparedStatement ps = db.prepareStatement(qstring);
	ResultSet rs = ps.executeQuery();
	if(! rs.next()) { rs.close(); ps.close(); return false; }
	boolean result = rs.getBoolean(1);
	rs.close(); ps.close();
	return result;
    }

    /** Given a zone number, resolve it fully into a DbName.  If the zone number cannot be found,
     * return null.  If the zone number is 0, return the root name.  This is useful for rdata
     * types such as MX or CNAME which contain a reference to another name.  This name
     * must then be loaded to compose the response. */
    public DbName findDbName(int previousNode) throws SQLException {
	if(previousNode < 0) throw new IllegalArgumentException("Zone number: " + previousNode + " is out of range.");
	if(previousNode == 0) return DbName.ROOT;
	String[] labelArray = new String[Name.MAX_LABELS + 1];
	int[] numberArray = new int[Name.MAX_LABELS + 1];
	boolean[] starArray = new boolean[Name.MAX_LABELS + 1];
	// String qstring = "SELECT name, previous, star FROM zone WHERE number = ?";
	// PreparedStatement ps = db.prepareStatement(qstring);
	int i = 0;
	// look for the label with number previousNode.
	// add this label to the list, and set previousNode to that number
	// keep doing this until we see a previousNode = 0, which means root, which means we're done
	ResultSet rs;
	// go through until previousNode is 0, which means we have hit the root.
	while(previousNode != 0) {
	    findDbNamePreparedStatement.setInt(1, previousNode);
	    rs = findDbNamePreparedStatement.executeQuery();
	    if(rs.next()) {
		labelArray[i] = rs.getString(1);
		starArray[i] = rs.getBoolean(2);
		numberArray[i] = previousNode;
		previousNode = rs.getInt(2);
		rs.close();
		i++;
		if(i >= labelArray.length) {
		    // there are too many nodes here; we can't add any more
		    rs.close(); return null;
		}
	    } else {
		// in this case, it didn't find a previous node, and we haven't reached 0 yet, so name can't be found
		rs.close(); return null;
	    }
	}
	// now we have a list of labels and zone numbers.  make a new DbName
	String[] resultLabels = new String[i];
	int[] resultNumbers = new int[i];
	boolean[] resultStars = new boolean[i];
	for(i = 0; i < resultLabels.length; i++) {
	    resultLabels[i] = labelArray[resultLabels.length - i - 1];
	    resultNumbers[i] = numberArray[resultNumbers.length -i - 1];
	    resultStars[i] = starArray[resultNumbers.length - i - 1];
	}
	DbName result = NameFactory.getDbName(resultLabels, resultNumbers, resultStars);
	return result;
    }

    /** Given a sequence name get the next value in the sequence.
     * WARNING: Only use this with safe strings; it uses a Statement, not a PreparedStatement. */
    private int getNextVal(String s) throws SQLException {
	if(s == null) throw new IllegalArgumentException("attempting to get next value for null string.");
	String qstring = "SELECT NEXTVAL('" + s + "')";
	Statement st = db.createStatement();
	ResultSet rs = st.executeQuery(qstring);
	if(!rs.next()) throw new SQLException("couldn't find a next value.");
	int result = rs.getInt(1);
	rs.close();
	st.close();
	return result;
    }

    /** These methods are used to update the database.  This first one adds a new zone.
     * This method returns the zone number of the new zone. */
    public int addZone(String zoneName, int previousZone) throws SQLException {
	if(!NameFactory.checkLabel(zoneName))
	    throw new IllegalArgumentException("Label: " + zoneName + " cannot be added.  Bad format.");
	if(previousZone < 0)
	    throw new IllegalArgumentException("Zone: " + previousZone + " is out of range.");
	if(!zoneExists(previousZone))
	    throw new OutOfRangeException("Zone: " + previousZone + " does not exist.");
	int number = getNextVal("zone_number_seq");
	String qstring = "INSERT INTO zone (number, time, name, previous) VALUES " +
	    "(" + number + ", NOW(), ?, " + previousZone + ")";
	PreparedStatement ps = db.prepareStatement(qstring);
	ps.setString(1, zoneName);
	ps.executeUpdate();
	return number;
    }

    /** Find out if the given zone number exists. */
    public boolean zoneExists(int number) throws SQLException {
	if(number < 0) return false;
	if(number == 0) return true; // ROOT always exists
	String qstring = "SELECT COUNT(number) FROM zone WHERE number = " + number;
	Statement st = db.createStatement();
	ResultSet rs = st.executeQuery(qstring);
	if(!rs.next()) throw new SQLException("couldn't get a result set in zoneExists.");
	int result = rs.getInt(1);
	rs.close(); st.close();
	return result == 1;
    }

    /** Given a new label and a previous node, create the new node and return the number.
     * The Resolver uses this to add a new Name to the database.
     * XXXXXXXXXXXXXXXXXXXXX
     * This needs to be fixed to do correctness checking on the label (make sure it
     * is unique at this level). */
    public int createNode(String label, int previousNode, boolean star) throws SQLException {
	if(! NameFactory.checkLabel(label)) throw new IllegalArgumentException("Label: " + label + " is invalid.");
	if(previousNode < 0) throw new IllegalArgumentException("Previous node: " + previousNode + " was out of range.");
	int result = getNextVal("zone_number_seq");
	String qstring = "INSERT INTO zone (number, time, name, previous, star) VALUES " +
	    "(" + result + ", NOW(), ?, " + previousNode + ", ?)";
	PreparedStatement ps = db.prepareStatement(qstring);
	ps.setString(1, label);
	ps.setBoolean(2, star);
	ps.executeUpdate();
	ps.close();
	return result;
    }

    /** Given a zone number, add an Rdata for that zone. */
    public int addRdata(int zoneNumber, Rdata rdata) throws SQLException {
	if(!zoneExists(zoneNumber))
	    throw new IllegalArgumentException("Zone: " + zoneNumber + " does not exist.");
	if(rdata == null)
	    throw new NullPointerException("Rdata was null in addRdata");
	String qstring;
	int nextVal = 0;
	Name serverName;
	switch(rdata.getType()) {
	    case Type.A: nextVal = getNextVal("arecord_number_seq");
		qstring = "INSERT INTO arecord (number, ttl, time, zone, addr) VALUES " +
		    "(" + nextVal + ", " + rdata.getTtl() + ", NOW(), " + zoneNumber + ", " +
		    ((ARdata) rdata).getAddress() + ")";
		break;
	    case Type.NS: nextVal = getNextVal("nsrecord_number_seq");
		serverName = ((NSRdata) rdata).getServer();
		if(!(serverName instanceof DbName))
		    throw new IllegalArgumentException("The servername in the rdata must be a dbname already.");
		qstring = "INSERT INTO nsrecord (number, ttl, time, zone, name) VALUES " +
		    "(" + nextVal + ", " + rdata.getTtl() + ", NOW(), " + zoneNumber + ", " +
		    ((DbName) serverName).getNumber() + ")";
		break;
	    case Type.CNAME: nextVal = getNextVal("cnamerecord_number_seq");
		serverName = ((CNAMERdata) rdata).getServer();
		if(!(serverName instanceof DbName))
		    throw new IllegalArgumentException("The servername in the rdata must be a dbname already.");
		qstring = "INSERT INTO cnamerecord (number, ttl, time, zone, name) VALUES " +
		    "(" + nextVal + ", " + rdata.getTtl() + ", NOW(), " + zoneNumber + ", " +
		    ((DbName) serverName).getNumber() + ")";
		break;
	    case Type.MX: nextVal = getNextVal("mxrecord_number_seq");
		serverName = ((MXRdata) rdata).getExchange();
		if(!(serverName instanceof DbName))
		    throw new IllegalArgumentException("The servername in the rdata must be a dbname already.");
		qstring = "INSERT INTO mxrecord (number, ttl, time, zone, preference, exchange) VALUES " +
		    "(" + nextVal + ", " + rdata.getTtl() + ", NOW(), " + zoneNumber + ", " +
		    ((MXRdata) rdata).getPreference() + ", " + ((DbName) serverName).getNumber() + ")";
		break;
	    case Type.SOA: nextVal = getNextVal("soarecord_number_seq");
		SOARdata soa = (SOARdata) rdata;
		Name mname = soa.getMname();
		Name rname = soa.getRname();
		if((!(mname instanceof DbName)) || (!(rname instanceof DbName)))
		    throw new IllegalArgumentException("Mname or Rname of SOA was not a DbName type.");
		qstring =
		    "INSERT INTO soarecord (number, time, zone, mname, rname, ser, refresh, retry, expire, minimum) " +
		    "VALUES (" + nextVal + ", NOW(), " + zoneNumber + ", " + ((DbName) mname).getNumber() +
		    ", " + ((DbName) rname).getNumber() + ", " + soa.getSerial() + ", " +
		    soa.getRefresh() + ", " +
		    soa.getRetry() + ", " +
		    soa.getExpire() + ", " +
		    soa.getMinimum() + ")";
		break;
	    default:
		qstring = null;
	}
	if(qstring == null) throw new IllegalArgumentException("Type: " + rdata.getType() + " is not supported.");
	PreparedStatement ps = db.prepareStatement(qstring);
	ps.executeUpdate();
	ps.close();
	return nextVal;
    }
}
