package org.sonatype.nexus.repository.protop.upgrade;

/**
 * Holds information about the 'protop' model defined by this plugin.
 * <p>
 * This model is stored in the 'component' database as attributes in the generic 'component' model.
 * While previous upgrades used the generic 'component' model name, future upgrades should use this
 * more specific model.
 * <p>
 * Upgrades should depend on the 'component' model and version at the time the upgrade was written.
 *
 * @since 3.13
 */
public interface ProtopModel {
    String NAME = "protop";
}
