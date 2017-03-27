package chiralsoftware.server.chiraldns.name;

import chiralsoftware.server.chiraldns.dns.Type;

/** A QuestionKey is a key to a lookup in a table correlating questions and answers.
 * It is designed for fast creation and fast matching.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.
 * @author Eric Hollander */
public final class QuestionKey {
    
    private final String[] labels;
    private final int hashCode;
    private final int qType;

    /** Efficiently construct a new QuestionKey.  This works most efficiently if a name from
     * the name package is used. */
    public QuestionKey(Name n, int qType) {
	if(n instanceof SimpleName) labels = ((SimpleName) n).labels;
	else if(n instanceof DbName) labels = ((DbName) n).labels;
	else { labels = new String[n.count()]; for(int i = 0; i < labels.length; i++) labels[i] = n.get(i); }
	hashCode = n.hashCode() + (qType + 19) * 7919;
	this.qType = qType;
    }

    public boolean equals(Object o) {
	if(!(o instanceof QuestionKey)) return false;
	QuestionKey q = (QuestionKey) o;
	if(qType != q.qType) return false;
	if(labels.length != q.labels.length) return false;
	for(int i = 0; i < labels.length; i++) if(! labels[i].equals(q.labels[i])) return false;
	return true;
    }

    public int hashCode() { return hashCode; }

    /** Return a String form of this question key, suitable for logging use. */
    public String asLogString() {
	StringBuffer result = new StringBuffer();
	for(int i = labels.length - 1; i >= 0; i--)
	    result.append((i == 0) ? labels[i] : (labels[i] + "."));
	result.append(": " + Type.string((short) qType));
	return result.toString();
    }

    public String toString() {
	StringBuffer result = new StringBuffer("QuestionKey: Type: " + qType + " hashCode: " + hashCode + " ");
	for(int i = labels.length - 1; i >= 0; i--)
	    result.append((i == 0) ? labels[i] : (labels[i] + "."));
	return result.toString();
    }
}
