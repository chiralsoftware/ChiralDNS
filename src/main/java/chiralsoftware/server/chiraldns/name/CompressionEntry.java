package chiralsoftware.server.chiraldns.name;

/** A CompressionEntry is an entry in a compression map which knows the offsets of its labels
 * and can be converted into a Name efficiently.  This class is immutable and threadsafe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved. */
public final class CompressionEntry {

    private final String[] labels;
    private final int count;
    private final int hashCode;

    /** Create a new CompressionEntry from a Name.  This is usually highly
     * efficient because it usually doesn't have to copy the Name's internal structures. */
    public CompressionEntry(Name n) {
	if(n instanceof SimpleName) this.labels = ((SimpleName) n).labels;
	else if(n instanceof DbName) this.labels = ((DbName) n).labels;
	else {
	    String[] sa = new String[n.count()];
	    for(int i = 0, limit = n.count(); i < limit; i++) sa[i] = n.get(i);
	    this.labels = sa;
	}
	count = labels.length;
	hashCode = computeHashCode(labels, count);
    }

    /** Create a new CompressionEntry.  The labels are not copied or checked
     * for correctness, because this creation
     * needs to be fast, because it is used in the compression loop. */
    CompressionEntry(String[] labels, int count) {
	if(labels == null) throw new NullPointerException("Labels was null.");
	if(count < 1) throw new IllegalArgumentException("Count was out of range: " + count);
	if(count > labels.length) throw new IllegalArgumentException("Count too high: " + count);
	this.labels = labels;
	this.count = count;
	hashCode = computeHashCode(labels, count);
    }

    /** Return the length of the left-most label; for example, for an entry of mail.yahoo.com, this
     * returns 4. */
    public int getTopLabelLength() { return labels[count - 1].length(); }

    /** Used in constructors to pre-calculate the hashCode. */
    private static int computeHashCode(String[] labels, int count) {
	int result = 69;
	for(int i = 0; i < count; i++)
	    result = result * 17 + labels[i].hashCode();
	return result;
    }

    /** Make up a new compression entry by chopping off the highest-level name, ie www.yahoo.com goes to yahoo.com. */
    public CompressionEntry chop() {
	if(count == 1) throw new IllegalStateException("Can't chop off a CompressionEntry with only 1 element.");
	int newCount = count - 1;
	return new CompressionEntry(labels, newCount);
    }

    /** Return a count of how many labels are in this CompressionEntry. */
    public int getCount() { return count; }

    /** The hashCode is used for storing in HashMaps.  This HashCode is only based on
     * the "visible" labels.  The hashCode is computed in the constructor. */
    public int hashCode() { return hashCode; }
	
    /** Two CompressionEntries are equal only if they have the same count of labels
     * and if all the labels up to count are equal, ignoring case. */
    public boolean equals(Object o) {
	if(!(o instanceof CompressionEntry)) return false;
	CompressionEntry test = (CompressionEntry) o;
	if(test.count != count) return false;
	for(int i = 0; i < count; i++) if(! test.labels[i].equalsIgnoreCase(labels[i])) return false;
	return true;
    }

    public String toString() {
	StringBuffer result = new StringBuffer("CompressionEntry: ");
	for(int i = count - 1; i >= 0; i--) result.append(i == 0 ? labels[i] : (labels[i] + "."));
	return result.toString();
    }
}
