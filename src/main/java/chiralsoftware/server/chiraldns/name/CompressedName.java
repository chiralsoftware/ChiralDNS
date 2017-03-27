package chiralsoftware.server.chiraldns.name;

import java.util.Iterator;
import java.nio.ByteBuffer;

/** This class extends the Name class to implement a compressed name.
 * A compressed name looks just like a regular name, except when it is outputting
 * to a stream, it outputs itself in correctly compressed format, and when it
 * is asked for its length, it returns that with correct compression.<p>
 * This class is immutable and therefore threadsafe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
final class CompressedName implements Name {

    /** This holds how many labels are UNcompressed.  Ie, if we are compressing
     * traffic.yahoo.com, and the compression target is www.yahoo.com, the uncompressedCount
     * would be 1, and the uncompressed label would be traffic, and the pointer would be to the
     * first byte of the yahoo.com label (ie, the length byte). */
    private final int compressedLabels;

    /** Hold the labels that make up this name. */
    final String[] labels;

    /** This contains the pointer to the first byte in the TARGET compression name. */
    private final int targetOffset;

    /** The length of this CompressedName. */
    private final int length;
    
    /** Hash code is pre-calcualted. */
    private final int hashCode;

    /** Create a new CompressedName given a Name, a target offset, and a count of compressed labels.
     * In other words, if compressMe = www.yahoo.com, and the compressedLabels count is 2,
     * and the targetOffset is 19, then the compressed name is www, followed by a pointer to
     * 19.
     * @throws IllegalArgumentException if offset is negative or compressedLabels is 0 or less. */
    public CompressedName(Name compressMe, int targetOffset, int compressedLabels) {
	if(compressMe == null) throw new NullPointerException("compressMe was null.");
	if(targetOffset < 1) throw new IllegalArgumentException("targetOffset was out of range: " + targetOffset);
	if((compressedLabels < 0) || (compressedLabels > compressMe.length()))
	    throw new IllegalArgumentException("Compressed labels count out of range: " + compressedLabels);
	if(compressMe instanceof SimpleName) labels = ((SimpleName) compressMe).labels;
	else if (compressMe instanceof DbName) labels = ((DbName) compressMe).labels;
	else {
	    String[] ra = new String[compressMe.count()];
	    for(int i = 0; i < ra.length; i++) ra[i] = compressMe.get(i);
	    labels = ra;
	}
	int len = 0;
	for(int i = compressedLabels; i < labels.length; i++) len += (labels[i].length() + 1);
	if(compressedLabels == 0) // nothing was compressed
	    length = len + 1;
	else // something was compressed
	    length = len + 2; // so the final label is 2 bits of flag and 14 bits of pointer
	hashCode = NameFactory.computeHashCode(labels);
	this.targetOffset = targetOffset;
	this.compressedLabels = compressedLabels;
    }

    /** Create a new CompressedName with doing no error-checking on the labels. */
    protected CompressedName(String sa[], int targetOffset, int compressedLabels) {
	if(sa == null) throw new NullPointerException("String[] was null");
	if(targetOffset < 1) throw new IllegalArgumentException("targetOffset was out of range.");
	if((compressedLabels < 0) || (compressedLabels > sa.length))
	    throw new IllegalArgumentException("compressedLabels: " + compressedLabels + " was out of range.");
	labels = sa;
	int len = 0;
	for(int i = compressedLabels; i < labels.length; i++) len += (labels[i].length() + 1);
	if(compressedLabels == 0) // nothing was compressed
	    length = len + 1;
	else // something was compressed
	    length = len + 2; // so the final label is 2 bits of flag and 14 bits of pointer
	hashCode = NameFactory.computeHashCode(labels);
	this.targetOffset = targetOffset;
	this.compressedLabels = compressedLabels;
    }
	

    /** Send this CompressedName to the given ByteBuffer stream.  This method is required by the Name interface.
     * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
     * This should be fixed to use UTF-8 instead of the platform default encoding in the String.getBytes()
     * method. */
    public void send(ByteBuffer bb) {
	if(bb == null) throw new NullPointerException("can't send to null ByteBuffer");
	int length;
	for(int i = labels.length - 1, limit = compressedLabels - 1; i > limit; i--) {
	    length = labels[i].length();
	    length &= 63; // take only the lower 6 bits; the high 2 bits are reserved for compression flag
	    bb.put((byte) length);
	    // now write the label itself
	    bb.put(labels[i].getBytes());
	}
	// now we have to write the offset pointer
	// the first byte to write is the the first 6 bits of the pointer, with the two leftmost
	// bits set.
	int b = ((targetOffset >> 8) & 0xff) | (3 << 6);
	bb.put((byte) b);
	// the second byte to write is simply the rightmost 8 bits
	b = targetOffset & 0xff;
	bb.put((byte) b);
	// and we're done
    }

    /** Find the length of this Name in compressed wire format. Required by the Name interface. */
    public int length() { return length; }

    /** Return the number of labels in this name.  Required by the Name interface. */
    public int count() { return labels.length; }

    public int hashCode() { return hashCode; }

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


    /** The Name interface requires that a Name be compressable, but that doesn't make sense for
     * a Name which is already compressed.  Don't use this. */
    public Name compress(int compressedCount, int targetOffset) { return this; }

    /** Return the requested label.  Required by the Name interface. */
    public String get(int i) {
	if((i < 0) || (i >= labels.length)) throw new IllegalArgumentException("int out of range: " + i);
	return labels[i];
    }

    public String[] getLabels() { return NameFactory.copyLabels(labels); }

    /** Iterators are required by the Name interface. */
    public Iterator iterator() { return  new NameIterator(labels); }
    public Iterator iterator(boolean reversed) { return new NameIterator(labels, reversed); }

    public boolean isRoot() { return labels.length == 0; }

    /** Implement toString for debugging. */
    public String toString() {
	StringBuffer sb = new StringBuffer("Compressedname (" + length + " bytes): ");
	sb.append("Offset: " + targetOffset + "; Labels: ");
	for(int i = labels.length - 1; i >= 0; i--) sb.append(labels[i] + ".");
	sb.append("; Target byte: " + targetOffset + "; compressed count: " + compressedLabels);
	return sb.toString();
    }
}
