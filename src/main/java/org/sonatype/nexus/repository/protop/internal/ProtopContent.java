package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamFunction;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * protop focused {@link Content} allowing for setting {@link ProtopStreamPayload} fields after creation.
 *
 * @since 3.16
 */
public class ProtopContent
        extends Content {
    private ProtopStreamPayload payload;

    public ProtopContent(final ProtopStreamPayload payload) {
        super(payload);
        this.payload = payload;
    }

    public ProtopContent packageId(final String packageId) {
        payload.packageId(packageId);
        return this;
    }

    public ProtopContent revId(final String revId) {
        payload.revId(revId);
        return this;
    }

    public ProtopContent fieldMatchers(final ProtopFieldMatcher fieldMatcher) {
        return fieldMatchers(singletonList(fieldMatcher));
    }

    public ProtopContent fieldMatchers(final List<ProtopFieldMatcher> fieldMatchers) {
        payload.fieldMatchers(fieldMatchers);
        return this;
    }

    public ProtopContent missingBlobInputStreamSupplier(
            final InputStreamFunction<MissingAssetBlobException> missingBlobInputStreamSupplier) {
        payload.missingBlobInputStreamSupplier(missingBlobInputStreamSupplier);
        return this;
    }
}
