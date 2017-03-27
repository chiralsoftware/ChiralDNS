package chiralsoftware.server.chiraldns.resolver;

/** This class implements an exception for some internal error condition in the server.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public class InternalException extends Exception {
    public InternalException() { super(); }

    public InternalException(String message) { super(message); }
}
