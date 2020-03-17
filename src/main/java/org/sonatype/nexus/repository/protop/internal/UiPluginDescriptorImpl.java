package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.rapture.UiPluginDescriptorSupport;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Rapture {@link org.sonatype.nexus.ui.UiPluginDescriptor} for {@code nexus-repository-protop}
 *
 * @since 3.16
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE - 300) // after nexus-proui-plugin
public class UiPluginDescriptorImpl
        extends UiPluginDescriptorSupport {
    public UiPluginDescriptorImpl() {
        super("nexus-repository-protop");
        setHasStyle(false);
        setNamespace("NX.protop");
        setConfigClassName("NX.protop.app.PluginConfig");
    }
}
