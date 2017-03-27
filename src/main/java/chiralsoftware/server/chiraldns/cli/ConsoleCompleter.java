package chiralsoftware.server.chiraldns.cli;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.HashSet;

import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.Readline;

/** This class defines a completer so that when users hit tab or ctl-D
 * to try to complete a file, it does the right thing.  Very basic for now.
 * @author Eric Hollander
 * Copyright 2001-2017, Eric Hollander.  All rights reserved. */
public class ConsoleCompleter implements ReadlineCompleter {

    /** This could be made more efficient by sorting the array first and then doing some
     * intelligent jumping in it, but it's so small that it doesn't matter for now. */
    public static final String[] mainCommands = {
	"help",
	"zone",
	"del",
	"add",
	"lz",
	"lr",
	"find",
	"stats",
	"quit" };

    public static final String[] zoneCommands = {
	"lr",
	"lz",
	"del",
	"add",
	"zone",
	"back",
	"help" };

    public static final int MAIN_COMPLETER = 1;
    public static final int ZONE_COMPLETER = 2;

    /** By default we are starting in main completer mode. */
    private int mode = MAIN_COMPLETER;
    
    /** Default constructor, doesn't do anything. */
    public ConsoleCompleter() { }

    /** Constructor that defines whether we are a main command completer or 
     * in the zone submenu. */
    public ConsoleCompleter(int mode) {
	if(!((mode == MAIN_COMPLETER) || (mode == ZONE_COMPLETER))) 
	    throw new IllegalArgumentException("mode: " + mode + " is not a valid mode");
	this.mode = mode;
    }

    private Iterator possibleValues;

    private Iterator getPossibleValues(String s) {
	if(s == null) throw new IllegalArgumentException("can't get possible values for null string");
	Set resultSet = new HashSet();
	String[] myArray = (mode == MAIN_COMPLETER) ? mainCommands : zoneCommands;
	for(int i = 0; i < myArray.length; i++)
	    if(myArray[i].startsWith(s)) resultSet.add(myArray[i]);
	return resultSet.iterator();
    }

    public void setMode(int i) {
	if((i != MAIN_COMPLETER) && (i != ZONE_COMPLETER))
	    throw new IllegalArgumentException("invalid completer mode setting");
	mode = i;
    }

    /** Are we at a command or argument to a command stage in this command line?
     * Look at the Readline.getLineBuffer to see the line so far. */
    private boolean commandOrArgument() {
	String line = Readline.getLineBuffer();
	if(line == null) return true; // nothing has been entered yet so we are in command mode???
	StringTokenizer st = new StringTokenizer(line);
	// if there are less than two tokens (ie, 0 or 1 tokens) then this is a command
	return st.countTokens() < 2;
    }

    /** Returns a possible completion, for the implementation of org.gnu.readline.ReadlineCompleter. */
    public String completer(String t, int state) {
	if(t == null) throw new IllegalArgumentException("can't complete a null string");
	if(commandOrArgument()) { // if this is a command token
	    if(state == 0) possibleValues = getPossibleValues(t);
	    if(possibleValues.hasNext()) return (String) possibleValues.next();
	    // otherwise we have reached the last choice.
	    return null;
	} else { // this is an argument token, in which case we don't complete it for now
	    return null;
	}
    }
}
