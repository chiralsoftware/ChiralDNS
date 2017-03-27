package chiralsoftware.server.chiraldns.name;

import chiralsoftware.server.chiraldns.resolver.OutOfRangeException;
import java.util.StringTokenizer;
import java.nio.ByteBuffer;


/** NameFactory is a static factory which produces various instances of Names.
 * There are no public constructors for Names, so this factory is the only
 * way a class outside of this package can get a Name object.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class NameFactory {

    /** Don't instantiate this class. */
    private NameFactory() { }

    /** The name which is the root of the DNS name hierarchy.  This name
     * has no labels, and so the count of its labels is zero.
     * It is the parent node of all names within DNS.  It is represented
     * in text form as ".", and can be seen at the end of fully-qualified
     * domain names, such as www.mobile.mp. */
    public static final Name ROOT = new SimpleName(new String[0], 1, computeHashCode(new String[0]));

    static final int INITIAL_ARRAY_SIZE = 10;

    /** Given a string which makes up a label, check whether it is a valid DNS label.  It must be
     * from 1 to 63 chars, and it must not contain the "." char, and there are several
     * other rules. */
    public boolean checkFormat(String s) {
	if(s == null) return false;
	if(s.length() > Name.MAX_NAME_LENGTH) return false;
	if(s.length() == 0) return false;
	if(s.indexOf(".") != -1) return false;
	return true;
    }

    /** Given an array of Strings, make up a safe array of labels.
     * @throws IllegalArgumentException if some label is not valid, or if the array
     * is not valid. */
    private static String[] safeLabels(String[] labels) {
	if(labels == null) throw new NullPointerException("No labels specified.");
	if(labels.length > Name.MAX_LABELS)
	    throw new IllegalArgumentException("Too many labels.");
	if(labels.length == 0) return labels; // zero-length arrays are final
	String test;
	int length = 0;
	String[] sa = new String[labels.length];
	for(int i = 0; i < sa.length; i++) {
	    test = labels[i];
	    length += test.length() + 1;
	    if(! checkLabel(test)) throw new IllegalArgumentException("Label: " + test + " was not valid.");
	    sa[i] = test;
	}
	if(length > Name.MAX_NAME_LENGTH) throw new IllegalArgumentException("Name exceeded maximum length.");
	return sa;
    }

    /** Given an array of ints, make up a safe array of ints. Checks that there are no duplicate numbers
     * (a loop). */
    private static int[] safeZoneNumbers(int[] ia) {
	if(ia == null) throw new NullPointerException("Null int array");
	if(ia.length == 0) return ia; // zero-length arrays are final
	if(ia.length > Name.MAX_LABELS)
	    throw new IllegalArgumentException("Too many zones in int[]");
	int test;
	int[] za = new int[ia.length];
	for(int i = 0; i < za.length; i ++) {
	    test = ia[i];
	    if(test < 1)
		throw new IllegalArgumentException("Test: " + test + " was out of range.");
	    for(int x = 0; x < i; x++) if(za[x] == test)
		throw new IllegalArgumentException("Found a duplicate label: " + test + " at: " + i);
	    za[i] = test;
	}
	return za;
    }

    /** Given a String, return a new Name object which represents that String. */
    public static Name nameFromString(String s) {
	if(s == null) throw new NullPointerException("Cannot create a name from a null string.");
	if(s.length() == 0) throw new IllegalArgumentException("Can't create a name from a zero-length string.");
	if(s.equals(".")) return ROOT;
	StringTokenizer st = new StringTokenizer(s.toUpperCase(), ".");
	int count = st.countTokens();
	if(count < 1) throw new IllegalArgumentException("Invalid name format.");
	if(count > Name.MAX_LABELS) throw new IllegalArgumentException("Too many labels: " + count);
	String[] labels = new String[count];
	String test;
	int len = 0;
	for(int i = count - 1; i >= 0; i--) {
	    test = st.nextToken();
	    if(! checkLabel(test))
		throw new IllegalArgumentException("Label: " + test + " was invalid.");
	    len += (test.length() + 1);
	    labels[i] = test;
	}
	len += 1;
	return new SimpleName(labels, len, computeHashCode(labels));
    }

    /** Create a new DbName, which contains zone numbers stored in a database. */
    public static DbName getDbName(String[] labels, int[] zoneNumbers, boolean[] stars) {
	if(zoneNumbers.length > labels.length)
	    throw new IllegalArgumentException("zoneNumbers length was greater than labels[] length.");
	if(stars.length != zoneNumbers.length)
	    throw new IllegalArgumentException("stars length must equal numbers length.");
	if(labels.length == 0) return DbName.ROOT;
	String[] sa = safeLabels(labels);
	int[] za = safeZoneNumbers(zoneNumbers);
	int length = 0;
	for(int i = 0; i < sa.length; i++) length += (sa[i].length() + 1);
	length += 1;
	boolean[] newStars = new boolean[stars.length];
	for(int i = 0; i < newStars.length; i++) newStars[i] = stars[i];
	return new DbName(sa, za, length, newStars);
    }

    /** Given an existing name, create a new DbName by adding on an array of ints.
     * This works very efficiently (no copies) if the Name argument is a SimpleName object.
     * It also works very efficiently for creating a new DbName from an existing one with a new
     * array of ints. */
    public static DbName createDbNameFromName(Name n, int[] ia, boolean[] stars) {
	if(n == null) throw new NullPointerException("No name specified.");
	String[] labels;
	if(n instanceof SimpleName) labels = ((SimpleName) n).labels;
	else if(n instanceof DbName) labels = ((DbName) n).labels;
	else { labels = new String[n.count()]; for(int i = 0, limit = n.count(); i < limit; i++) labels[i] = n.get(i); }
	int[] za = safeZoneNumbers(ia);
	if(za.length > labels.length) throw new IllegalArgumentException("Too many zone numbers");
	if(stars.length != za.length)
	    throw new IllegalArgumentException("Star count must match zone number count.");
	boolean[] newStars = new boolean[stars.length];
	for(int i = 0; i < newStars.length; i++) newStars[i] = stars[i];
	return new DbName(labels, za, n.length(), newStars);
    }

    /** Given a ByteBuffer, create a new Name object.  Note that this cannot yet
     * process a compressed name (ie, it can't uncompress).  Fix this later.
     * Also, fix this so it does some very basic correctness checks on the string
     * to make sure there are no naughty bits.
     * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
     * Fix this: This makes labels by reading bytes off the ByteBuffer, making a byte[],
     * and then using new String(byte[]) as the constructor.  This constructor
     * uses the platform's default encoding, which could be incorrect.  It should be forced
     * to use UTF-8 or whatever is the current standard for DNS.
     * Also, it should lowercase everything, not uppercase everything, because it seems
     * that the new IDN standard is that everything should be lower case. */
    public static Name nameFromByteBuffer(ByteBuffer bb) {
	if(bb == null) throw new NullPointerException("ByteBuffer was null.");
	int totalLength = 0; // needs to stay below MAX_NAME_LENGTH, or else this is a strange packet
	// pre-allocate an array as large as is acceptable... usually a very small number of labels in the array
	String[] tempArray = new String[Name.MAX_LABELS];
	int b; // a byte read out of the array
	int i = 0; // which label are we looking at
	byte[] labelBytes;
	boolean foundRoot = false;
	String test;
	while(totalLength < Name.MAX_NAME_LENGTH) {
	    // first read the length byte
	    if(i == Name.MAX_LABELS) {
		StringBuffer sb = new StringBuffer();
		for(int x = 0; x < tempArray.length; x++) sb.append(tempArray[x] + ".");
		throw new OutOfRangeException("Too many labels: " + sb);
	    }
	    b = bb.get() & 0xff;
	    if(b > 63) throw new OutOfRangeException("label too long: must be less than 64 chars");
	    if(b == 0) { // this means we have hit the root label
		foundRoot = true; break; }
	    labelBytes = new byte[b];
	    bb.get(labelBytes);
	    test = (new String(labelBytes)).toUpperCase();
	    tempArray[i] = test;
	    totalLength += (labelBytes.length + 1);
	    i++;
	}
	if(!foundRoot) throw new IllegalArgumentException("didn't find a root label in this query");
	if(totalLength > Name.MAX_NAME_LENGTH)
	    throw new IllegalArgumentException("name is too long in this query");
	// now copy this array to its final array
	String[] resultArray = new String[i];
	for(int x = 0; x < resultArray.length; x++) resultArray[x] = tempArray[i - x - 1];
	return new SimpleName(resultArray, totalLength + 1, computeHashCode(resultArray));
    }

    /** All names should use the same hashcode formula. */
    static int computeHashCode(String[] labels, int count) {
	if(count < 0) throw new IllegalArgumentException("Count can't be less than zero.");
	if(labels == null) throw new NullPointerException("Labels cannot be null.");
	if(count == 0) return 42;
	if(labels.length == 0) return 42;
	int hashCode = 7369; // a big prime
	if(count > labels.length) count = labels.length;
	for(int i = 0; i < count; i++) hashCode += (hashCode * 17 + labels[i].hashCode());
	return hashCode;
    }

    static int computeHashCode(String[] labels) { return computeHashCode(labels, labels.length); }

    /** This is a mask to see if a packet is a simple query where we should cache the response. */
    private static final int queryMask = (1 << 15) | (15 << 12);

    /** Given a packet in a ByteBuffer, immediately get a QuestionKey from the packet.
     * The question name is always in the same place, so this can be found quickly before
     * anything else is checked.  This method also checks some header flags to make sure that this
     * packet is a query.  This allows a cache to very efficiently answer some queries. */
    public static QuestionKey getQuestionKey(ByteBuffer bb) {
	bb.position(2);
	int flags = bb.getShort();
	if((flags & queryMask) != 0) return null; // this means the packet wasn't a simple query
	bb.position(12); // this is where the name starts
	// this could be made more efficient... instead of reading a name and then
	// creating the questionkey why not create the questionkey directly
	Name n = nameFromByteBuffer(bb);
	return new QuestionKey(n, bb.getShort());
    }

    /** Given a label see if the format of it is valid.
     * If it passes this, just make it to upper before adding to the db. */
    public static boolean checkLabel(String s) {
	if(s == null) return false;
	if(s.length() > 63) return false; // labels must be less than 64 chars
	if(s.length() == 0) return false; // no zero-length labels
	if(s.indexOf(".") != -1) return false; // no dots in labels
	// we should do some regexp stuff here.
	// but for now return true.
	return true;
    }

    /** Send an array of labels to a ByteBuffer object.
     * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
     * Fix this also so that it uses a more reasonable encoding than "platform default." */
    static void send(ByteBuffer bb, String[] labels) {
	if(bb == null) throw new NullPointerException("ByteBuffer was null.");
	if(labels == null) throw new NullPointerException("String[] labels was null.");
	int length;
	for(int i = labels.length - 1; i >= 0; i--) {
	    length = labels[i].length();
	    // this & shouldn't be necessary because length should always be < 64..
	    length &= 63; // take only the lower 6 bits; the high 2 bits are reserved for compression flag
	    bb.put((byte) length);
	    bb.put(labels[i].getBytes());
	}
	// the last element in a name is a zero-length label
	bb.put((byte) 0);
    }

    static String[] copyLabels(String[] labels) {
	if(labels == null) throw new NullPointerException("Labels was null.");
	if(labels.length == 0) return labels;
	String[] result = new String[labels.length];
	for(int i = 0; i < result.length; i++) result[i] = labels[i];
	return result;
    }
}
