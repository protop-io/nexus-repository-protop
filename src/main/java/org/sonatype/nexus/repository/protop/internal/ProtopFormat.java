package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.Format;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * protop repository format.
 */
@Named(ProtopFormat.NAME)
@Singleton
public class ProtopFormat extends Format {
    public static final String NAME = "protop";

    public ProtopFormat() {
        super(NAME);
    }
}
