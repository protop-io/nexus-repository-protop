package org.sonatype.nexus.repository.protop.internal;

import com.google.common.collect.Maps;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpStatus.*;

/**
 * protop response utility, that sends status messages along with JSON, as protop CLI expects.
 *
 * @since 3.0
 */
public final class ProtopResponses {
    private ProtopResponses() {
        // nop
    }

    private static final Payload successPayload = statusPayload(true, null);

    private static Payload statusPayload(final boolean success, @Nullable final String error) {
        NestedAttributesMap errorObject = new NestedAttributesMap("error", Maps.newHashMap());
        errorObject.set("success", Boolean.valueOf(success));
        if (error != null) {
            errorObject.set("error", error);
        }
        return new BytesPayload(ProtopJsonUtils.bytes(errorObject), ContentTypes.APPLICATION_JSON);
    }

    @Nonnull
    static Response ok() {
        return HttpResponses.ok(successPayload);
    }

    @Nonnull
    static Response ok(@Nonnull final Payload payload) {
        return HttpResponses.ok(checkNotNull(payload));
    }

    @Nonnull
    static Response notFound(@Nullable final String message) {
        return new Response.Builder()
                .status(Status.failure(NOT_FOUND, "Not Found"))
                .payload(statusPayload(false, message))
                .build();
    }

    @Nonnull
    static Response packageNotFound(final ProtopProjectId packageId) {
        checkNotNull(packageId);
        return notFound("Package '" + packageId + "' not found");
    }

    @Nonnull
    static Response tarballNotFound(final ProtopProjectId packageId, final String tarballName) {
        checkNotNull(packageId);
        checkNotNull(tarballName);
        return notFound("Tarball '" + tarballName + "' in package '" + packageId + "' not found");
    }

    @Nonnull
    static Response badRequest(@Nullable final String message) {
        return new Response.Builder()
                .status(Status.failure(BAD_REQUEST))
                .payload(statusPayload(false, message))
                .build();
    }

    @Nonnull
    static Response badCredentials(@Nullable final String message) {
        return new Response.Builder()
                .status(Status.failure(UNAUTHORIZED))
                .payload(statusPayload(false, message))
                .build();
    }
}
