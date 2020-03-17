package org.sonatype.nexus.repository.protop.internal;

import com.google.common.base.Supplier;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.storage.ContentValidator;
import org.sonatype.nexus.repository.storage.DefaultContentValidator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * protop specific {@link ContentValidator} that "hints" default content validator for protop metadata and format
 * specific files.
 *
 * @since 3.0
 */
@Named(ProtopFormat.NAME)
@Singleton
public class ProtopContentValidator
        extends ComponentSupport
        implements ContentValidator {
    private final DefaultContentValidator defaultContentValidator;

    @Inject
    public ProtopContentValidator(final DefaultContentValidator defaultContentValidator) {
        this.defaultContentValidator = checkNotNull(defaultContentValidator);
    }

    @Nonnull
    @Override
    public String determineContentType(final boolean strictContentTypeValidation,
                                       final Supplier<InputStream> contentSupplier,
                                       @Nullable final MimeRulesSource mimeRulesSource,
                                       @Nullable final String contentName,
                                       @Nullable final String declaredContentType) throws IOException {
        String name = contentName;
        // if not .tar.gz, it must be json package root
        if (name != null && !name.endsWith(".tar.gz")) {
            name += ".json";
        }
        return defaultContentValidator.determineContentType(
                strictContentTypeValidation, contentSupplier, mimeRulesSource, name, declaredContentType
        );
    }
}
