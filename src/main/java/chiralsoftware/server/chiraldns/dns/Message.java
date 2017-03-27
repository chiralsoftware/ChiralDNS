package chiralsoftware.server.chiraldns.dns;

import java.util.Set;
import java.util.Iterator;
import java.nio.ByteBuffer;

/** This class implements a DNS message.  A message contains up to five segments:
 * Header, Question, Answer, Authority, Additional.  Header and Question are mandatory.  The rest
 * may not be present.<p>
 * This class is not thread-safe.<p>
 * Copyright 2001-2017, Eric Hollander.  All rights reserved.<p>
 * @author Eric Hollander */
public final class Message {

    public static Message getInstance(ByteBuffer bb) {
	Message result = null;
	try { result = new Message(bb); }
	catch(IllegalArgumentException e) { return null; }
	catch(ProtocolException e) { return null; }
	return result;
    }

    /** Load in a message from an InputStream. */
    public Message(ByteBuffer bb) throws ProtocolException {
	header = new Header(bb);
	// there is always a question section
	questionSection = new QuestionSection(bb);
	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// this is a bug but for now we will not load the three other sets of RRs
	// even if they are present.  we don't need them to resolve things...
	answer = null;
	authority = null;
	additional = null;	
    }

    /** This constructs a nes message to send as a response. */
    public Message(Header header, QuestionSection questionSection, Set answerSet,
		   Set authoritySet, Set additionalSet) throws ProtocolException {
	if(header == null) throw new IllegalArgumentException("no header in constructor for message");
	if(questionSection == null)
	    throw new IllegalArgumentException("no question in constructor for message");
	if(answerSet == null)
	    throw new IllegalArgumentException("no answer in constructor for message");
	if(authoritySet == null)
	    throw new IllegalArgumentException("no authority in constructor for message");
	if(additionalSet == null)
	    throw new IllegalArgumentException("no additional in constructor for message");

	this.questionSection = questionSection;
	this.header = header;

	Iterator it;
	// the resource records must be stored in arrays, not sets, because we have to be
	// sure that they read out in the same order every time, or compression won't work correctly.
	answer = new ResourceRecord[answerSet.size()];
	authority = new ResourceRecord[authoritySet.size()];
	additional = new ResourceRecord[additionalSet.size()];
	
	int i;
	it = answerSet.iterator();
	i = 0;
	while(it.hasNext()) { answer[i] = (ResourceRecord) it.next(); i++; }

	it = authoritySet.iterator();
	i = 0;
	while(it.hasNext()) { authority[i] = (ResourceRecord) it.next(); i++; }

	it = additionalSet.iterator();
	i = 0;
	while(it.hasNext()) { additional[i] = (ResourceRecord) it.next(); i++; }

	updateCounts();
    }

    /** Call this method before sending to get the message into a compressed format.
     * After the message is compressed it can also be displayed by toString in its
     * compressed format. */
    public void compress() {
	assert(header != null);
	int offset = Compressor.HEADER_LENGTH;
	// compress question section first
	// the header itself can't be compressed because it doesn't contain any names
	Compressor compressor = new Compressor();
	offset = questionSection.compress(compressor, offset);
	int i;
	if(answer != null)
	    for(i = 0; i < answer.length; i++) offset = answer[i].compress(compressor, offset);
	if(authority != null)
	    for(i = 0; i < authority.length; i++) offset = authority[i].compress(compressor, offset);
	if(additional != null)
	    for(i = 0; i < additional.length; i++) offset = additional[i].compress(compressor, offset);
    }	

    /** Send the contents of this Message to a ByteBuffer. */
    public void send(ByteBuffer bb) {
	if(bb == null) throw new NullPointerException("Cannot send to a null ByteBuffer");
	header.send(bb);
	questionSection.send(bb);
	int i;
	for(i = 0; i < answer.length; i++) answer[i].send(bb);
	for(i = 0; i < authority.length; i++) authority[i].send(bb);
	for(i = 0; i < additional.length; i++) additional[i].send(bb);
    }

    private Header header;
    private QuestionSection questionSection;
    /** Answer, Authority, and Additional are all of the same format: Sets of Resource Records. */
    private ResourceRecord[] answer;
    private ResourceRecord[] authority;
    private ResourceRecord[] additional;

    /** This method updates the counts of various things so that the Header is consistent with
     * what is in the message. */
    public void updateCounts() throws ProtocolException {
	header = new Header(header, questionSection.getCount(), answer.length, authority.length, additional.length);
    }

    public Header getHeader() { return header; }

    public QuestionSection getQuestion() { return questionSection; }
    
    private String listResourceRecords(ResourceRecord[] rr) {
	if(rr == null) return null;
	StringBuffer result = new StringBuffer();
	for(int i = 0; i < rr.length; i++) result.append("     " + rr[i] + "\n");
	return result.toString(); }

    public String toString() {
	return "DNS Message; Header: " + header + "\n" +
	    "Question (" + questionSection.getCount() + "): " + questionSection + "\n" +
	    "Answer (" + ((answer == null) ? 0 : answer.length) + "):\n" +
	    listResourceRecords(answer) +
	    "Authority (" + ((authority == null) ? 0 : authority.length) + "): " +
		listResourceRecords(authority) + "\n" +
	    "Additional (" + ((additional == null) ? 0 : additional.length) + "): " +
	    listResourceRecords(additional) + "\n";
    }

    /** This should return a defensive copy. */
    public ResourceRecord[] getAnswer() { return answer; }

    /** This should return a defensive copy. */
    public ResourceRecord[] getAuthority() { return authority; }

    /** This should return a defensive copy. */
    public ResourceRecord[] getAdditional() { return additional; }

}
