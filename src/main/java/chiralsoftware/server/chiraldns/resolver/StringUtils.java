package chiralsoftware.server.chiraldns.resolver;

import java.util.StringTokenizer;

/** StringUtils is a collection of static methods for manipulating strings.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class StringUtils {
    
    /** Don't instantiate this. */
    private StringUtils() { }

    /** Truncates a String s to length i or less */
    public static String truncate(String s, int i) {
	if(s == null) { return null; }
	if(i < 1) { return null; }
	if(s.length() > i) { return s.substring(0, i); }
	else { return s; }
    }

    private static int stringToInt(String s) {
	if(s == null) return 0;
	try { return Integer.parseInt(s); }
	catch(NumberFormatException nfe) { return 0; }
    }

    public static int ipStringToInt(String s) {
	if(s == null) throw new IllegalArgumentException("attempting to parse null");
	StringTokenizer st = new StringTokenizer(s, ".");
	if(st.countTokens() != 4)
	    throw new IllegalArgumentException("IP address: " + s + " is not in the correct format.");
	int octetNumber = 0;
	int octetValue = 0;
	int result = 0;
	String octetString;
	while(st.hasMoreTokens()) {
	    octetValue = stringToInt(st.nextToken());
	    if(octetValue > 255)
		throw new IllegalArgumentException("Invalid format: " + octetValue + " is out of range.");
	    result = result + (octetValue << ((3 - octetNumber) * 8));
	    octetNumber++;
	}
	return result;
    }
    
    /** Given an ip addr as a 32bit int convert it to a string. */
    public static String ipIntToString(int ip) {
	// there are at least 8 chars in this buffer
	StringBuffer sb = new StringBuffer(8);
	for(int i = 3; i >= 0; i--) {
	    if(i == 0)
		sb.append(((ip >>> (i * 8)) & 0xff));
	    else
		sb.append(((ip >>> (i * 8)) & 0xff) + ".");
	}
	return sb.toString();
    }

}
