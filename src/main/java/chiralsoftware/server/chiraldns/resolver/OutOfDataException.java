package chiralsoftware.server.chiraldns.resolver;

import java.io.IOException;

/** This defines an exception which is hit when we are trying to read a multi-byte
 * value from a stream which is out of data.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public class OutOfDataException extends IOException {
    public OutOfDataException() { super(); }

    public OutOfDataException(String message) { super(message); }
}
