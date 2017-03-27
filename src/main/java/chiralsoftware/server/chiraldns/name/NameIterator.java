package chiralsoftware.server.chiraldns.name;

import java.util.Iterator;

/** This class is an iterator which walks through the labels in a name.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved. 
 * @author Eric Hollander */
class NameIterator implements Iterator {

    private final String[] labels;

    protected NameIterator(String[] labels, boolean reversed) { this.labels = labels; this.reversed = reversed; }
    protected NameIterator(String[] labels) { this(labels, false); }

    private final boolean reversed;

    private int count = 0;

    public void remove() {
	throw new UnsupportedOperationException("Name does not implement remove()");
    }

    public boolean hasNext() {
	return count < labels.length;
    }

    public Object next() {
	if(reversed) return labels[labels.length - (count++) - 1];
	else return labels[count++];
    }
}

