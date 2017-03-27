package chiralsoftware.server.chiraldns.resolver;

import java.util.Date;

/** This class holds certain statistics about the Resolver's database.
 * @author Eric Hollander
 * Copyright 2001-2017, Eric Hollander.  All rights reserved. */
public class ResolverStatistics {

    /** When did this resolver get started? */
    private Date startDate = null;

    /** How many zones are there total in the database? */
    private int zoneCount = 0;

    /** How many A records? */
    private int ACount = 0;

    /** How many MX records? */
    private int MXCount = 0;

    /** How many SOA records? */
    private int SOACount = 0;

    /** Empty constructor, used when the resolver can't connect to db usually. */
    public ResolverStatistics() { }

    public ResolverStatistics(Date startDate, int zoneCount, int ACount, int MXCount,
			      int SOACount) {
	this.startDate = startDate;
	this.zoneCount = zoneCount;
	this.ACount = ACount;
	this.MXCount = MXCount;
	this.SOACount = SOACount;
    }

    public Date getStartDate() { return startDate; }
    public int getZoneCount() { return zoneCount; }
    public int getACount() { return ACount; }
    public int getMXCount() { return MXCount; }
    public int getSOACount() { return SOACount; }
}
