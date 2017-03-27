package chiralsoftware.server.chiraldns.resolver;

/** This class implements an exception for when a user attempts to read bits or bytes
 * from a BitBuffer or a ByteBuffer which are out of range.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public class OutOfRangeException extends IllegalArgumentException {
    public OutOfRangeException() { super(); }

    public OutOfRangeException(String message) { super(message); }
}
