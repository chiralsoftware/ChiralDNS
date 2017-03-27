package chiralsoftware.server.chiraldns.resolver;

import chiralsoftware.server.chiraldns.name.NameFactory;

/** This class implements a Node, which contains a name, the current number
 * and the next number.  This class is final and immutable and therefore threadsafe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class Node {

    private final int number;
    private final String name;
    private final int previous;
    private final boolean star;

    /** The root of the DNS hierarchy.  There is only one instance of the root node.
     * Calls to getInstance for node 0 will always return this root node. */
    public static final Node ROOT = new Node(0, null, 0, false);

    /** A static factory to generate new Nodes.  A static factory is used as the constructor
     * to ensure that only one root node is every instantiated so it can be tested with <code>==</code>.
     * If the number is 0, previous must also be 0, and ROOT is returned.  The String name argument
     * must be a valid label.  It is checked with <code>Name.checkLabel(String)</code>. */
    public static Node getInstance(int number, String name, int previous, boolean star) {
	// The previous number for node 0 (root) is always 0.
	if((number == 0) && (previous != 0))
	    throw new IllegalArgumentException("The previous node of root node is always also root.");
	if(number == 0) return ROOT;
	if(number < 0) throw new IllegalArgumentException("Node number can't be less than 0.");
	if(previous < 0) throw new IllegalArgumentException("Previous Node number can't be less than 0.");
	if(! NameFactory.checkLabel(name))
	    throw new IllegalArgumentException("Label: " + name + " is not a valid label.");
	return new Node(number, name, previous, star);
    }

    private Node(int number, String name, int previous, boolean star) {
	if(number < 0) throw new IllegalArgumentException("Node number cannot be less than 0");
	if(previous < 0) throw new IllegalArgumentException("Previous cannot be less than 0");
	if((number != 0) && (name == null)) throw new IllegalArgumentException("Name cannot be null except for root.");
	this.number = number; this.name = name; this.previous = previous; this.star = star;
    }

    /** Returns true if this node is a node for a star zone. */
    public boolean isStar() { return star; }

    /** Get the number of this zone. */
    public int getNumber() { return number; }

    /** Get the name of this zone. */
    public String getName() { return name; }

    /** Get the number of the previous label in this zone. */
    public int getPrevious() { return previous; }

    /** Returns true if this is the root node.
     * @return  */
    public boolean isRoot() { return number == 0; }

    @Override
    public String toString() {
	return ((number == 0) ? "ROOT" : name) + ": " + number + ", " + previous + ", star: " + star; }
}
