package chiralsoftware.server.chiraldns.utils;

import chiralsoftware.server.chiraldns.resolver.Resolver;
import chiralsoftware.server.chiraldns.resolver.StringUtils;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.dns.rdata.ARdata;
import chiralsoftware.server.chiraldns.dns.rdata.CNAMERdata;
import chiralsoftware.server.chiraldns.dns.rdata.MXRdata;
import chiralsoftware.server.chiraldns.dns.rdata.NSRdata;
import chiralsoftware.server.chiraldns.dns.rdata.Rdata;
import chiralsoftware.server.chiraldns.dns.rdata.SOARdata;
import chiralsoftware.server.chiraldns.name.DbName;
import chiralsoftware.server.chiraldns.name.Name;
import chiralsoftware.server.chiraldns.name.NameFactory;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;
import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;

/** LoadFile takes zone data from the standard input and stores it in the database,
 * creating zones and resource records as needed.<p>
 * The format of input lines is as follows:
 * <pre>
 * mp 43200 SOA ns1.nic.mp hostmaster.nic.mp 3	14400 7200 604800 43200
 * mp 14400 NS	ns1.nic.mp
 * mp 14400 NS	ns2.nic.mp
 * nic.mp 14400 MX	10	mail.nic.mp
 * nic.mp 14400 MX	20	mail.chiral.com
 * ; this is a comment
 * # this is also a comment
 * </pre>
 * LoadFile is not thread-safe.  Administrators must take care to ensure that only one
 * administrator is making changes to the db at any given time.<p>
 * BUG WARNING: Order matter when creating star names.  If a cname is created, and the target
 * name is foo.mp, and then later on a record for *.foo.mp is created, the new record will not
 * be a star record.  Star records need to be created on the first usage of a name in the 
 * zone file.  This is somewhat of a bug, but star records are exceptional, and should be rare in files.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class LoadFile {

    private final Resolver resolver;

    public LoadFile(Connection db) throws SQLException {
	if(db == null) throw new NullPointerException("Can't load a file with a null db connection.");
	if(db.isClosed()) throw new IllegalArgumentException("Can't load a file from a closed db connection.");
	resolver = new Resolver(db);
    }

    public LoadFile(Properties properties) throws SQLException, ClassNotFoundException {
	resolver = new Resolver(properties);
    }

    /** Returns true if this line is a comment line. */
    private static boolean isComment(String s) {
	if(s == null) throw new NullPointerException("String was null.");
	return s.matches("^\\s*[#;].*");
    }

    /** Given a line of input, add it to the db. */
    private void processLine(Command command, PrintStream ps) throws SQLException {
	assert((command != null) && (ps != null));
	int result = 0;
	DbName newName = resolver.addName(command.getName(), command.getStar());
	if(command.getType() == Type.A) {
	    // only one arg: the ip addr
	    int ipAddr = StringUtils.ipStringToInt(command.get(0));
	    Rdata rdata = new ARdata(command.getTtl(), ipAddr);
	    result = resolver.addRdata(newName.getNumber(), rdata);
	    ps.println(newName + ": " + rdata + " (record #" + result + ")");
	} else if(command.getType() == Type.NS) {
	    Name newServer; try { newServer = NameFactory.nameFromString(command.get(0)); }
	    catch(Exception e) { ps.println("Name: " + command.get(0) + " is not valid."); return; }
	    DbName server = resolver.addName(newServer, false);
	    Rdata rdata = new NSRdata(command.getTtl(), server);
	    result = resolver.addRdata(newName.getNumber(), rdata);
	    ps.println(newName + ": " + rdata + " (record #" + result + ")");
	} else if(command.getType() == Type.CNAME) {
	    Name newServer; try { newServer = NameFactory.nameFromString(command.get(0)); }
	    catch(Exception e) { ps.println("Name: " + command.get(0) + " is not valid."); return; }
	    DbName server = resolver.addName(newServer, false);
	    Rdata rdata = new CNAMERdata(command.getTtl(), server);
	    result = resolver.addRdata(newName.getNumber(), rdata);
	    ps.println(newName + ": " + rdata + " (record #" + result + ")");
	} else if(command.getType() == Type.MX) {
	    // ps.println("Command 0: " + command.get(0) + "; command 1: " + command.get(1));
	    int pref; try { pref = Integer.parseInt(command.get(0)); }
	    catch(NumberFormatException nfe) { ps.println("Preference: " + command.get(0) + " is not valid."); return; }
	    Name newServer; try { newServer = NameFactory.nameFromString(command.get(1)); }
	    catch(Exception e) { ps.println("Name: " + command.get(1) + " is not valid."); return; }
	    DbName server = resolver.addName(newServer, false);
	    Rdata rdata = new MXRdata(command.getTtl(), server, pref);
	    result = resolver.addRdata(newName.getNumber(), rdata);
	    ps.println(newName + ": " + rdata + " (record #" + result + ")");
	} else if(command.getType() == Type.SOA) {
	    // an SOA line looks like this:
	    // mname rname serial refresh retry expire minimum
	    Name mName;
	    Name rName;
	    try { mName = NameFactory.nameFromString(command.get(0));
	    rName = NameFactory.nameFromString(command.get(1)); }
	    catch(Exception e) { ps.println("One of the names in the command line was not valid."); return; }
	    mName = resolver.addName(mName, false);
	    rName = resolver.addName(rName, false);
	    int serial, refresh, retry, expire, minimum;
	    try {
		serial = Integer.parseInt(command.get(2));
		refresh = Integer.parseInt(command.get(3));
		retry = Integer.parseInt(command.get(4));
		expire = Integer.parseInt(command.get(5));
		minimum = Integer.parseInt(command.get(6));
	    }
	    catch(Exception e) { ps.println("One of the numbers was out of range."); return; }
	    Rdata rdata = new SOARdata(mName, rName, serial, refresh, retry, expire, minimum);
	    result = resolver.addRdata(newName.getNumber(), rdata);
	    ps.println(newName + ": " + rdata + " (record #" + result + ")");
	} else {
	    ps.println("Command type: " + command.getType() + " is not supported.");
	}
    }

    public int processInput(InputStream is) throws IOException, SQLException {
	if(is == null) throw new NullPointerException("No input stream.");
	BufferedReader r = new BufferedReader(new InputStreamReader(is));
	String line;
	int count = 0;
	Command command;
	while((line = r.readLine()) != null) {
	    if(isComment(line)) { System.out.println(line); continue; }
	    if(line.matches("^\\s*$")) continue;
	    command = new Command(line);
	    processLine(command, System.out); count++; }
	return count;
    }

    /** Private static inner class to parse the different command types.
     * A command looks like this:
     * <pre>www.chiral.mp 5400 MX mail.chiral.mp 20</pre>
     * Commands are immutable and threadsafe. */
    static final class Command {
	private final Name name;
	private final int ttl;
	private final int type;
	private final String[] args;
	private final boolean star;

	/** Given a String, parse it into a new Command.  */
	Command(String s) throws NumberFormatException {
	    if(s == null) throw new NullPointerException("Cannot parse a null string.");
	    StringTokenizer st = new StringTokenizer(s);
	    int count = st.countTokens();
	    if(count < 4) throw new IllegalArgumentException("Not enough tokens in the command string.");
	    String nameString = st.nextToken();
	    if(nameString.startsWith("*.")) {
		// System.out.println("This zone: " + s + " is a star zone.");
		star = true;
		nameString = nameString.substring(2, nameString.length());
	    } else {
		star = false;
	    }
	    name = NameFactory.nameFromString(nameString);
	    String ttlString = st.nextToken();
	    ttl = Integer.parseInt(ttlString);
	    String typeString = st.nextToken();
	    type = Type.value(typeString);
	    switch(type) {
		case(Type.A): args = new String[1]; args[0] = st.nextToken(); break;
		case(Type.NS): args = new String[1]; args[0] = st.nextToken(); break;
		case(Type.CNAME): args = new String[1]; args[0] = st.nextToken(); break;
		case(Type.MX): args = new String[2]; args[0] = st.nextToken(); args[1] = st.nextToken(); break;
		case(Type.SOA): args = new String[7]; for(int i = 0; i < 7; i++) args[i] = st.nextToken(); break;
		default: throw new IllegalArgumentException("Type: " + type + " is unknown.");
	    }
	}
	
	Name getName() { return name; }
	int getTtl() { return ttl; }
	int getType() { return type; }
	boolean getStar() { return star; }
	String[] getArgs() { return args; }
	String get(int i) { return args[i]; }

	public String toString() {
	    StringBuffer result = new StringBuffer(Type.string((short) type) + ": (" + ttl + ") ");
	    for(int i = 0; i < args.length; i++) { result.append((i == args.length - 1) ? args[i] : (args[i] + " ")); }
	    return result.toString();
	}
    }

    public static void main(String[] args) {
	System.out.println("chiralDNS(tm) input file processor");
	System.out.println("Copyright 2001-2017, Eric Hollander.  All rights reserved.");
	try {
	    Properties properties = new Properties();
	    String fileName = System.getProperty("chiralDNS.ResolverSettings");
	    if(fileName == null) fileName = "ResolverSettings";
	    try { FileInputStream fis = new FileInputStream(fileName); properties.load(fis); }
	    catch(FileNotFoundException fnfe)
	    { System.out.println("File: " + fileName + " was not found."); System.exit(1); }
	    LoadFile loadFile = new LoadFile(properties);
	    loadFile.processInput(System.in);
	    System.out.println("Done.");
	}
	catch(IOException ioe) {
	    System.out.println("IOException caught: " + ioe);
	    System.exit(1);
	}
	catch(SQLException sqe) {
	    System.out.println("SQLException caught: " + sqe);
	    System.exit(1);
	}
	catch(ClassNotFoundException cnfe) {
	    System.out.println("ClassNotFoundException caught: " + cnfe);
	    System.out.println("This probably means that the JDBC jar could not be found.");
	    System.exit(1);
	}
    }
}
