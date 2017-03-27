package chiralsoftware.server.chiraldns.name;

import java.nio.ByteBuffer;
import java.util.Iterator;


/** This class implements a Name, which is used in many different places in dns messages.
 * A name is represented in binary form as a series of labels.  A label has a 6-bit length and then
 * that number of chars, followed by the next label.<p>
 * This class is immutable and threadsafe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
final class SimpleName implements Name {

    /** When we are loading an array of labels from the InputStream, what is the typical
     * upper limit number of labels? */
    private final static int INITIAL_ARRAY_SIZE = 4;

    /** This is the root label; because all Names are generated by a static factory,
     * there is only one root name, so it can be tested for equality. */
    public static final Name ROOT = new SimpleName(new String[0], 1);

    /** This holds a list of the labels in the name.  it is in the reverse order:
     * www.yahoo.com, www = 2, yahoo = 1, com = 0. */
    final String[] labels;

    /** The length of the name is pre-calcualted.  This variable is not final, because
     * a Name constructor which is designed to be overridden may not set it. */
    private final int length;

    /** The hashCode is pre-calculated. */
    private final int hashCode;

    /** Given a set of labels and a length, construct a new Name.  No defensive copy is made. */
    SimpleName(String[] labels, int len) {
	this.labels = labels; length = len;
	hashCode = NameFactory.computeHashCode(labels);
    }

    /** Very efficiently construct a SimpleName given an array of labels, a length and a hashcode.
     * No defensive copy is made. */
    SimpleName(String[] labels, int len, int hashCode) { this.labels = labels; length = len; this.hashCode = hashCode; }

    /** Return a compressed version of this name.  Required by the Name interface. */
    public Name compress(int compressedCount, int targetOffset) {
	return new CompressedName(labels, targetOffset, compressedCount);
    }

    /** Return a new Name object which uses a sub-list of the labels in this name, with
     * count labels.  Specifying more labels than the name has throws an exception.
     * Specifying less than one label also throws an exception; only ROOT has zero labels.
     * Efficiency improvement: Instead of copying the array, it could have a limit marker
     * and share the array. */
    public Name subName(int count) {
	if(count < 0) throw new IllegalArgumentException("Count was out of range: " + count);
	if(count > labels.length) throw new IllegalArgumentException("Count out of range: " + count);
	if(count == 0) return SimpleName.ROOT;
	if(count == labels.length) return this;
	String[] sa = new String[count];
	int len = 0;
	for(int i = 0; i < sa.length; i++) { sa[i] = labels[i]; len += sa[i].length() + 1; }
	return new SimpleName(sa, len + 1);
    }
	
    public Iterator iterator() { return new NameIterator(labels); }
    public Iterator iterator(boolean reversed) { return new NameIterator(labels, reversed); }

    public boolean isRoot() { return labels.length == 0; }

    /** Return the length of this name in bytes, as it is ready to send in wire format.
     * For example the length of www.yahoo.com would be the length of the labels
     * (11) plus the label length bytes (3) plus a null label length to terminate it (1)
     * for a total of 15. */
    public int length() { return length; }

    public void send(ByteBuffer bb) { NameFactory.send(bb, labels); }

    /** Given two ints, return the minimum of the two. */
    private static int min(int x, int y) { return (x < y) ? x : y; }

    public boolean equals(Object o) {
	if(!(o instanceof Name)) return false;
	String[] olabels;
	if(o instanceof SimpleName) olabels = ((SimpleName) o).labels;
	else if(o instanceof DbName) olabels = ((DbName) o).labels;
	else if(o instanceof CompressedName) olabels = ((CompressedName) o).labels;
	else {
	    olabels = new String[((Name) o).count()];
	    for(int i = 0, l = ((Name) o).count(); i < l; i++) olabels[i] = ((Name) o).get(i);
	}
	if(olabels.length != labels.length) return false;
	for(int i = 0; i < labels.length; i++) if(! olabels[i].equalsIgnoreCase(labels[i])) return false;
	return true;
    }

    public int hashCode() { return hashCode; }

    /** Return a count of how many labels there are in this name. */
    public int count() {
	assert(labels != null);
	return labels.length;
    }
    
    /** Given a label number return the corresponding label.  Labels are stored in an array,
     * and so the label index starts at zero.  The index must be in the range
     * <code>0 <= index < Name.count()</code>. */
    public String get(int i) {
	if(labels == null) return null;
	if(i >= labels.length) throw new IllegalArgumentException("Index " + i + " is out of range.");
	return labels[i];
    }

    public String[] getLabels() { return NameFactory.copyLabels(labels); }

    public String toString() {
	assert(labels != null);
	if(labels.length == 0) return "ROOT name";
	int count = labels.length;
	StringBuffer sb = new StringBuffer();
	for(int i = count - 1; i >= 0; i--)
	    sb.append(labels[i] + ".");
	return sb.toString();
    }
}