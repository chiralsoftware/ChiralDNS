package chiralsoftware.server.chiraldns.cli;

import chiralsoftware.server.chiraldns.resolver.Resolver;
import chiralsoftware.server.chiraldns.resolver.ResolverStatistics;
import chiralsoftware.server.chiraldns.resolver.StringUtils;
import chiralsoftware.server.chiraldns.dns.InvalidNameFormatException;
import chiralsoftware.server.chiraldns.dns.ResourceRecord;
import chiralsoftware.server.chiraldns.dns.Type;
import chiralsoftware.server.chiraldns.dns.rdata.ARdata;
import chiralsoftware.server.chiraldns.dns.rdata.MXRdata;
import chiralsoftware.server.chiraldns.dns.rdata.Rdata;
import chiralsoftware.server.chiraldns.name.DbName;
import chiralsoftware.server.chiraldns.name.Name;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import java.sql.SQLException;

/** This class implements a console which can query and look at the database directly.
 * It requires the Java readline interface library, which in turn requires either
 * GNU readline or libeditline to be somewhere in the link path.  Note that
 * if GNU readline is linked in, and the software is redistributed, then the entire
 * thing falls under gpl.  editline is under bsd license so it doesn't have this consequence.
 * Java readline is under lgpgl.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public class Console {
    private static final String defaultPropertiesName = ".chiralDNSsettings";

    private static final int MAX_LINE_LENGTH = 1024;

    /** Holds all the user-definable settings */
    private static Properties properties;
    private static Properties defaultProperties;
    static {
	// initialize default properties
	defaultProperties = new Properties();
	defaultProperties.setProperty("readlineLib", "GnuReadline");
    }

    /** This is the name of the app, so users can put in app-specific stuff in their .inputrc files. */
    private static final String appName = "chiralDNS";

    /** This is the default main prompt */
    private static final String mainPrompt = "chiralDNS> ";

    /** This holds whatever is the current prompt. */
    private static String currentPrompt = mainPrompt;

    private static File history;

    private static Resolver resolver;

    /** Mode defines whether we are in main mode or in zone mode. */
    private static int mode;

    /** If we are in zone mode, this holds a ref to the zone we are in. */
    private static DbName currentZone;

    /** Given a filename, load the properties file, and load the appropriate Readline class. */
    private static void loadProperties(String n) throws IOException, SQLException, ClassNotFoundException {
	System.out.println("chiralDNS Command Console");
	System.out.println("Copyright 2001-2017, Eric Hollander.  All rights reserved.");

	if((n == null) || (n.length() == 0)) n = defaultPropertiesName;
	try {
	    FileInputStream fis = new FileInputStream(n);
	    properties = new Properties();
	    properties.load(fis);
	    fis.close();
	}
	catch(IOException ioe) {
	    System.out.println("Couldn't read " + defaultPropertiesName + "; using default settings.");
	    properties = defaultProperties;
	}

	String readlineLib = properties.getProperty("readlineLib");
	// this should always work because there should always be a properties object
	Readline.load(ReadlineLibrary.byName(readlineLib));
	Readline.initReadline(appName);
	// we will not be reading any custom inputrc file... it isn't needed because you can
	// put app-specific stuff in the .inputrc file.
	// at this point we could define some extra function keys like this:
	// Readline.parseAndBind("\"\\e[18~\":	\"Function key F7\"");
	// but we don't need to
	// read in the history file
	history = new File(".chiralDNS_history");
	try {
	    if(history.exists())
		Readline.readHistoryFile(history.getName());
	}
	catch(Exception e) {
	    System.err.println("error reading history file: " + e.getMessage());
	}
	// set the word break chars to break on space and tab
	Readline.setWordBreakCharacters(" \t");
	// set the completer
	Readline.setCompleter(new ConsoleCompleter(ConsoleCompleter.MAIN_COMPLETER));
	// load in resolver properties file
	ResolverSettings.load();
	// now activate the resolver.  this opens an sql link, etc.
	resolver = new Resolver();
	mode = ConsoleCompleter.MAIN_COMPLETER;
	currentZone = null;
    }

    /** This parses a ttl argument, ie, 12h or 1d gets converted into seconds and returned.
     * If it can't parse it returns 0. */
    private static int parseTtl(String s) {
	if(s == null) return 0;
	if((s.length() == 0) || (s.length() > 10))
	    throw new IllegalArgumentException("Ttl out of range: bad length..");
	if(! StringUtils.checkForLegalChars(s, "01234567890hd"))
	    throw new IllegalArgumentException("Ttl out of range: illegal characters.");
	// if we have a d or an h, we must specify some digits there too
	String numberString = StringUtils.getLegalString(s, "1234567890");
	if(numberString.length() == 0)
	    throw new IllegalArgumentException("Ttl out of range: No number specified.");
	int number = 0;
	try { number = Integer.parseInt(numberString); }
	catch(NumberFormatException nfe) { // should absolutely never happen
	    System.out.println("caught a numberformatexception: " + nfe.getMessage()); }
	// now see if we have to multiple anything
	if(s.endsWith("d")) number *= (60 * 60 * 24);
	else if(s.endsWith("h")) number *= (60 * 60);
	return number;
    }

    /** Display the main menu help message. */
    private static void mainHelp(String[] args) {
	System.out.println("This is the main help file.");
	System.out.println("The commands are....");
    }

    /** Enter into Zone mode. */
    private static void mainZone(String[] args) {
	if(args.length != 2) {
	    System.out.println("You did not specify a correct zone."); return; }
	System.out.println("Looking up zone: " + args[1]);
	currentZone = resolver.findZone(args[1]);
	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// also set the prompt to display which zone we're in
	// AND set the console completer to know that we are in zone mode
	ConsoleCompleter c = (ConsoleCompleter) Readline.getCompleter();
	c.setMode(ConsoleCompleter.ZONE_COMPLETER);
	Name n = currentZone.getName();
	currentPrompt = n + "> ";
	mode = ConsoleCompleter.ZONE_COMPLETER;
    }

    private static void mainDel(String[] args) {
	System.out.println("Deleting a zone... not implemented.");
	if((args == null) || (args.length != 2))
	    throw new IllegalArgumentException("You did not specify correct arguments for delete.");
	Name n = new Name(args[1]);
	System.out.println("About to delete zone: " + n);
	// resolver.deleteZone(n);
	System.out.println("Zone has been deleted.");
	System.out.println("This is not implemented!!!");
    }

    private static void mainAdd(String[] args) {
	System.out.println("Adding a zone.");
	if((args == null) || (args.length < 2)) {
	    System.out.println("You did not specify enough arguments to add a zone.");
	}
	Name n = null;
	try { n = new Name(args[1]); }
	catch(InvalidNameFormatException infe) {
	    System.out.println("The zone you attempted to add was not in a valid format: " +
			       infe.getMessage());
	}
	System.out.println("Adding name: " + n);
	System.out.println("THIS IS NOT IMPLEMENTED!");
	//try { resolver.addZone(n); }
	//catch(SQLException sqe) {
	//  System.out.println("Caught sql exception: " + sqe.getMessage()); return; }
	System.out.println("The new zone has been added; entering Zone mode.");
	// and now enter that zone
	ConsoleCompleter c = (ConsoleCompleter) Readline.getCompleter();
	c.setMode(ConsoleCompleter.ZONE_COMPLETER);
	currentZone = resolver.findZone(n);
	currentPrompt = n + "> ";
	mode = ConsoleCompleter.ZONE_COMPLETER;
    }

    private static void mainLz(String[] args) {
	System.out.println("listing a zone... not implemented.");
    }

    private static void mainLr(String[] args) {
	System.out.println("listing a zone records... not implemented.");
    }

    private static void mainFind(String[] args) {
	System.out.println("finding a zone... not implemented.");
    }

    private static void mainStats(String[] args) {
	System.out.println("Resolver statistics:");
	ResolverStatistics rs;
	try { rs = resolver.getStatistics(); }
	catch(SQLException sqe) {
	    System.out.println("Caught SQLException: " + sqe.getMessage()); return; }
	if(rs == null) {
	    System.out.println("Statistics could not be loaded."); return; }
	System.out.println("Resolver started at: " + rs.getStartDate());
	System.out.println("Zone count: " + rs.getZoneCount());
	System.out.println("A record count: " + rs.getACount());
	System.out.println("MX record count: " + rs.getMXCount());
	System.out.println("SOA record count: " + rs.getSOACount());
    }

    private static void mainQuit(String[] args) {
	System.out.println("Shutting down command console.");
	exit();
    }

    private static void mainUnknown(String[] args) {
	if((args == null) || (args.length == 0))
	    System.out.println("Unknown or invalid command.");
	else
	    System.out.println("Unknown command: " + args[0]);
    }

    private static void zoneError(String[] args) {
	System.out.println("An unknown zone command error has occured.");
    }

    /** List the records in a zone, given certain args. */
    private static void zoneLr(String[] args) {
	Set typeSet;
	if(args.length >= 2) {
	    typeSet = new HashSet(args.length - 1);
	    for(int i = 0; i < (args.length - 1); i++)
		if(Type.isSupported(args[i + 1]))
		    typeSet.add(Type.reverseTypes.get(args[i + 1].toUpperCase()));
	    Iterator it = typeSet.iterator();
	    short currentType;
	    int i;
	    Short si;
	    Set resourceRecordSet;
	    while(it.hasNext()) {
		si = (Short) it.next();
		if(si == null) { System.out.println("iterator next is null!!!"); break; }
		currentType = si.shortValue();
		try {
		    resourceRecordSet =
			resolver.getResourceRecordSet(currentZone.getNumber(), (int) currentType);
		    if(resourceRecordSet.isEmpty()) continue;
		    System.out.println(Type.string(currentType) + " records: ");
		    Iterator rrIterator = resourceRecordSet.iterator();
		    while(rrIterator.hasNext())
			System.out.println("     " + ((ResourceRecord) rrIterator.next()).getRdata());
		}
		catch(SQLException sqe) { System.out.println("Caught an sqlexception: " + sqe.getMessage()); }
	    }
	    // in this case we just return
	    return;
	}
	// otherwise, no arg was specified so that means we should list them ALL
	System.out.println("Listing all records is not supported yet.");
    }
	
    private static void zoneLz(String[] args) {
	System.out.println("Zone List has not been implemented yet.");
    }
	
    private static void zoneDel(String[] args) {
	System.out.println("Zone del has not been implemented yet.");
    }

    /** Create a new ARecord.  The command string would look like this:
     * <pre>ar a 12h 128.32.43.201</pre> which means, add a record, type a record, 
     * 12h ttl, with the address. */
    private static void newARecord(String[] args) {
	if(args == null) throw new IllegalArgumentException("null args to newARecord");
	if(args.length != 4) throw new IllegalArgumentException("incorrect number of arguments.");
	try {
	    int ttl = parseTtl(args[2]);
	    String ipString = args[3];
	    int ipInt;
	    ipInt = StringUtils.ipStringToInt(ipString);
	    // now we can add this record with a ttl of 0.  When the Resolver adds it
	    // it will set the ttl to be the zone default ttl.  We should fix the console
	    // so that it is possible to update ttl, but users shouldn't have to specify them
	    // when creating a record because they will almost always be the default ttl.
	    Rdata newRdata = new ARdata(ttl, ipInt);
	    resolver.addRdata(currentZone.getNumber(), newRdata);
	}
	catch(Exception e) {
	    System.out.println("Record could not be created: " + e.getMessage());
	    return;
	}
	System.out.println("Record was added.");
    }

    private static void newMXRecord(String[] args) {
	if(args.length != 5) throw new IllegalArgumentException("incorrect number of arguments");
	try {
	    int ttl = parseTtl(args[2]);
	    int preference = Integer.parseInt(args[3]);
	    String exchangeString = args[4];
	    Name exchange = new Name(exchangeString);
	    Rdata newRdata = new MXRdata(ttl, exchange, preference);
	    resolver.addRdata(currentZone.getNumber(), newRdata);
	}
	catch(Exception e) {
	    System.out.println("Record could not be created: " + e.getMessage());
	    return;
	}
	System.out.println("Record added.");
    }

    private static void zoneAdd(String[] args) {
	System.out.println("attempting to add a new record.");
	if((args == null) || (args.length < 3)) {
	    System.out.println("You did not specify enough arguments to add a record to this zone.");
	}
	String type = args[1];
	if(type == null) { System.out.println("Invalid command."); return; }
	type = type.toUpperCase();
	if(type.equals("A")) newARecord(args);
	else if(type.equals("MX")) newMXRecord(args);
	else System.out.println("this type is not supported yet");
	return;
    }

    private static void zoneZone(String[] args) {
	if((args == null) || (args.length != 2)) {
	    System.out.println("zone: invalid arguments."); return; }
	// now we change to the new zone specified
	mainZone(args);
    }

    /** Return to the main command setting. */
    private static void zoneBack(String[] args) {
	currentZone = null;
	mode = ConsoleCompleter.MAIN_COMPLETER;
	currentPrompt = mainPrompt;
	ConsoleCompleter c = (ConsoleCompleter) Readline.getCompleter();
	c.setMode(ConsoleCompleter.MAIN_COMPLETER);
	// XXXXXXXXXXXXX
	// set prompt and update the completer
	System.out.println("back in main mode");
    }

    private static void zoneHelp(String[] args) {
	System.out.println("Zone mode help file");
	System.out.println("This is info about the commands...");
    }
	
    private static void zoneUnknown(String[] args) {
	System.out.println("Unknown zone command.");
    }

    /** Execute a line of command input. */
    public static void execute(String line) {
	if(line == null) return;
	if(line.length() > MAX_LINE_LENGTH) return;
	StringTokenizer st = new StringTokenizer(line);
	String[] lineArray = new String[st.countTokens()];
	if(lineArray.length == 0) return; // no action was specified
	for(int i = 0; i < lineArray.length; i++) lineArray[i] = st.nextToken();
	String command = lineArray[0];
	if(mode == ConsoleCompleter.MAIN_COMPLETER) {
	    if(command.equals("help")) mainHelp(lineArray);
	    else if(command.equals("zone")) mainZone(lineArray);
	    else if(command.equals("del")) mainDel(lineArray);
	    else if(command.equals("add")) mainAdd(lineArray);
	    else if(command.equals("lz")) mainLz(lineArray);
	    else if(command.equals("lr")) mainLr(lineArray);
	    else if(command.equals("find")) mainFind(lineArray);
	    else if(command.equals("stats")) mainStats(lineArray);
	    else if(command.equals("quit")) mainQuit(lineArray);
	    else mainUnknown(lineArray);
	    return;
	} else { // mode is zone mode
	    if(currentZone == null) { zoneError(lineArray); return; }
	    if(command.equals("lr")) zoneLr(lineArray);
	    else if(command.equals("lz")) zoneLz(lineArray);
	    else if(command.equals("del")) zoneDel(lineArray);
	    else if(command.equals("add")) zoneAdd(lineArray);
	    else if(command.equals("zone")) zoneZone(lineArray);
	    else if(command.equals("back")) zoneBack(lineArray);
	    else if(command.equals("help")) zoneHelp(lineArray);
	    else zoneUnknown(lineArray);
	}
    }

    public static void exit() {
	try {
	    Readline.writeHistoryFile(history.getName());
	}
	catch(Exception e) {
	    System.err.println("Exception while writing history: " + e.getMessage());
	}
	System.out.println();
	Readline.cleanup();
	System.exit(0);
    }

    public static void main(String[] args) {
	// first load the properties file, and the readline class.  readline will
	// be ready to use at this point
	try {
	    if(args.length == 1) loadProperties(args[0]); else loadProperties(null);
	}
	catch(IOException ioe) {
	    System.err.println("caught ioexception while loading properties: " + ioe.getMessage()); }
	catch(SQLException sqe) {
	    System.err.println("Caught an SQLException while loading properties: " + sqe.getMessage()); }
	catch(ClassNotFoundException cnfe) {
	    System.err.println("Caught a ClassNotFoundException while loading properties: " +
			       cnfe.getMessage());
	}
	String line;
	Zone zone;
	while(true) {
	    try {
		line = Readline.readline(currentPrompt);
		if(line == null) // XXXXXX delete this
		    System.out.println("no input");
		else {
		    System.out.println("Trying this command: " + line);
		    execute(line);
		    // for now let's just treat the entire line as a zone
		    // and load up all the records for it
		    // later add commands like del, add, ls, etc.
		    //zone = resolver.findZone(line);
		    //System.out.println("Got this zone: " + zone);
		}
		 
	    }
	    catch(UnsupportedEncodingException enc) {
		System.err.println("caught UnsupportedEncodingException: " + enc.getMessage());
		break;
	    }
	    catch(IOException ioe) {
		System.err.println("caught ioexception: " + ioe.getMessage());
		break;
	    }
	}
	exit();
    }
}
