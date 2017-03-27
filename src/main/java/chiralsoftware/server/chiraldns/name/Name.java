package chiralsoftware.server.chiraldns.name;

import java.util.Iterator;
import java.nio.ByteBuffer;

/** A Name holds a series of labels.  All Name implementations are immutable and threadsafe. <p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public interface Name {

    /** Return the length, in bytes, of this Name object, in its wire representation. */
    public int length();

    /** Return the number of labels this name represents. */
    public int count();

    /** Given an offset, return a new compressed version of the name. */
    public Name compress(int compressedCount, int targetOffset);

    /** Return the given label number. */
    public String get(int i);

    /** Send a Name out to a ByteBuffer. */
    public void send(ByteBuffer bb);

    /** Return a copy of the labels stored in this name.  This requires making a copy
     * of the array, to preserve the immutability of Name implementations. */
    public String[] getLabels();

    /** Is this a root entry? */
    public boolean isRoot();

    /** Return an Iterator which walks the labels in the Name. */
    public Iterator iterator();

    /** Return an Iterator which walks the labels, and specify the order. */
    public Iterator iterator(boolean reversed);
    
    /** This is the max length of a name we will deal with.
     * See http://www.faqs.org/rfcs/rfc1035.html.  The reason for the limit is to
     * simplify implementations. */
    public final static int MAX_NAME_LENGTH = 255;

    /** The limit of how many labels a name can have.  The DNS spec allows up to 127 labels in a name,
     * but in practice, if the server encounters a name
     * with more than this number of labels, it is probably from HAX0R2. */
    public final static int MAX_LABELS = 7;

    public final static Name ROOT = new SimpleName(new String[0], 1);

}
