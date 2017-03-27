package chiralsoftware.server.chiraldns.dns;

/** This except is thrown when the DNS packet which is received contains an
 * invalid format.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved. 
 * @author Eric Hollander */
public class ProtocolException extends Exception {
    public ProtocolException() { super(); }

    public ProtocolException(String message) { super(message); }
}
