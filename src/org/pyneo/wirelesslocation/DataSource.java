package org.pyneo.wirelesslocation;

public interface DataSource {
	String getName();
	String getDescription();
	String getCopyright();
	boolean isSourceAvailable();
}
