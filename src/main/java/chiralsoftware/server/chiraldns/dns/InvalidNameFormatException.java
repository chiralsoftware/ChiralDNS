package chiralsoftware.server.chiraldns.dns;

/** This class implements an exception which is thrown when a name is in an invalid format.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public class InvalidNameFormatException extends IllegalArgumentException {
    public InvalidNameFormatException() { super(); }

    public InvalidNameFormatException(String message) { super(message); }
}
