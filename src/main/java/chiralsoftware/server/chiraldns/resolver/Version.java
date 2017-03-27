package chiralsoftware.server.chiraldns.resolver;

/** Provides constants for defining the version numbers. */
public final class Version {

    /** Do not instantiate this class. */
    private Version() { }

    /** The major number of this version of chiralDNS. */
    public static final int major = 1;

    /** The minor number of this version of chiralDNS. */
    public static final int minor = 0;

    /** The sub-version of this version of chiralDNS. */
    public static final int subVersion = 1;

    public static String getVersionString() {
	return "chiralDNS " + major + "." + minor + "." + subVersion;
    }

    public static void main(String args[]) {
	System.out.println(getVersionString());
    }
}
