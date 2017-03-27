package chiralsoftware.server.chiraldns.name;

import java.util.Iterator;
import java.nio.ByteBuffer;

/** This class extends Name and implements a Name which is suited
 * for using with the database because it keeps an array of zone numbers
 * which correspond to the labels.  Note that there may not be a zone number
 * for every label.  In fact there may be no zonenumbers for a zone which is unknown
 * to the zone file.<p>
 * DbName is immutable and therefore completely threadsafe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class DbName implements Name {
    
    final String[] labels;
    private final int[] zoneNumbers;
    private final boolean[] stars;
    private final int length;
    private final int hashCode;

    public int getNumber() {
	if(zoneNumbers.length == 0) return 0;
	return zoneNumbers[zoneNumbers.length - 1];
    }

    /** This is the ROOT node (".") as a DbName. */
    public static final DbName ROOT = new DbName(new String[0], new int[0], 1, new boolean[0]);

    /** Given an array of labels and an array of label numbers, construct a new DbName.
     * No defensive copying or correctness checking is done. */
    DbName(String[] labels, int[] ia, int length, boolean[] stars) {
	if(ia == null) throw new NullPointerException("int[] was null.");
	if(labels == null) throw new NullPointerException("No labels.");
	if(length < 1) throw new IllegalArgumentException("Length was less than 1.");
	if(stars == null) throw new NullPointerException("No star zone array.");
	if(stars.length != ia.length)
	    throw new IllegalArgumentException("The numbers and stars lengths must be the same.");
	this.labels = labels;
	this.zoneNumbers = ia;
	this.length = length;
	this.stars = stars;
	hashCode = NameFactory.computeHashCode(labels);
    }

    /** Return whether this name is fully-resolved.  In a fully-resolved name,
     * there is a label number for every label. */
    public boolean isFullyResolved() {
	return labels.length == zoneNumbers.length;
    }
    
    /** Is this zone a star zone? */
    public boolean isStar() {
	if(stars.length == 0) return false; // root is not a star zone
	return stars[stars.length - 1];
    }

    /** Return an array of the zone numbers for this name.  It returns a copy of its
     * internal array, to maintain immutability. */
    public int[] getZoneNumbers() {
	int[] result = new int[zoneNumbers.length];
	for(int i = 0; i < zoneNumbers.length; i++) result[i] = zoneNumbers[i];
	return result;
    }

    /** Give a count of the zone numbers known in this dbname. */
    public int getZoneNumberCount() {
	assert(zoneNumbers != null);
	return zoneNumbers.length;
    }

    /** Get the zone number at the given number. */
    public int getZoneNumber(int i) { return zoneNumbers[i]; }

    /** Test whether a particular label number is a star zone or not. */
    public boolean isStar(int i) { return stars[i]; }

    /** Return a new DbName object which is a sub-list of the labels in this name.
     * This should be made more efficient by not copying arrays, but instead it could
     * have a cut-off count number. */
    public Name subName(int count) {
	if(count < 0) throw new IllegalArgumentException("Count cannot be less than zero.");
	if(count == 0) // no labels are wanted, in otherwords return the root name
	    return DbName.ROOT;
	if(count >= labels.length) // this thing doesn't need to e truncated, so return itself.
	    return this;
	// otherwise we actually have to create a new object
	String[] newLabels = new String[count];
	int len = 0;
	for(int i = 0; i < newLabels.length; i++) {
	    newLabels[i] = labels[i]; len += (newLabels[i].length() + 1); }
	int[] newNumbers = new int[(count > zoneNumbers.length) ? zoneNumbers.length : count];
	boolean[] newStars = new boolean[(count > zoneNumbers.length) ? zoneNumbers.length : count];
	for(int i = 0; i < newNumbers.length; i++) { newNumbers[i] = zoneNumbers[i]; newStars[i] = stars[i]; }
	return new DbName(newLabels, newNumbers, len + 1, newStars);
    }

    /** Return a new DbName which has one less label than this DbName.  The Root
     * DbName will return itself as a subname. */
    public DbName subName() {
	if(labels.length == 0) return this;
	if(labels.length == 1) return DbName.ROOT;
	String[] newLabels = new String[labels.length - 1];
	for(int i = 0; i < newLabels.length; i++) newLabels[i] = labels[i];
	int len = 0;
	for(int i = 0; i < newLabels.length; i++) len+= (newLabels[i].length() + 1);
	len += 1;
	if(newLabels.length >= zoneNumbers.length) // no need to copy zone numbers if they aren't changing
	    return new DbName(newLabels, zoneNumbers, len, stars);
	// if we get here it means we must copy zone numbers
	int[] newNumbers = new int[newLabels.length];
	boolean[] newStars = new boolean[newLabels.length];
	for(int i = 0; i < newNumbers.length; i++) { newNumbers[i] = zoneNumbers[i]; newStars[i] = stars[i]; }
	return new DbName(newLabels, newNumbers, len, newStars);
    }

    /** Return a DbName which includes only the fully-resolved portion. */
    public DbName getFullyResolvedName() {
	if(labels.length == 0) return this;
	if(labels.length == zoneNumbers.length) return this;
	// if we get here, there are more labels than zone numbers
	if(zoneNumbers.length == 0) return DbName.ROOT;
	String[] newLabels = new String[zoneNumbers.length];
	int len = 0;
	for(int i = 0; i < newLabels.length; i++) { newLabels[i] = labels[i]; len += (labels[i].length() + 1); }
	return new DbName(newLabels, zoneNumbers, len + 1, stars);
    }

    public int length() { return length; }
    
    public int count() { return labels.length; }

    public String get(int i) { return labels[i]; }

    public String[] getLabels() { return NameFactory.copyLabels(labels); }

    public void send(ByteBuffer bb) { NameFactory.send(bb, labels); }

    public Iterator iterator() { return new NameIterator(labels); }
    public Iterator iterator(boolean reversed) { return new NameIterator(labels, reversed); }

    public boolean isRoot() { return labels.length == 0; }

    /** Return a compressed version of this name.  Required by the Name interface. */
    public Name compress(int compressedCount, int targetOffset) {
	return new CompressedName(labels, targetOffset, compressedCount);
    }

    public String toString() { 
	assert(labels != null); assert(zoneNumbers != null);
	if(labels.length == 0) return "ROOT DbName";
	StringBuffer sb = new StringBuffer("DbName: ");
	for(int i = labels.length - 1; i >= 0; i--)
	    if(i == 0) sb.append(labels[i]);
	    else sb.append(labels[i] + ".");
	sb.append(";");
	for(int i = zoneNumbers.length - 1; i >= 0; i--)
	    if(i == 0) sb.append(zoneNumbers[i]);
	    else sb.append(zoneNumbers[i] + ".");
	return sb.toString();
    }

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
}
