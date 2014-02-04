package org.pyneo.wirelesslocation.cellapi;

import org.pyneo.wirelesslocation.DataSource;
import org.pyneo.wirelesslocation.PropSpec;

import java.util.Collection;

public interface LocationSource<T extends PropSpec> extends DataSource {
	Collection<LocationSpec<T>> retrieveLocation(Collection<T> specs);
}
