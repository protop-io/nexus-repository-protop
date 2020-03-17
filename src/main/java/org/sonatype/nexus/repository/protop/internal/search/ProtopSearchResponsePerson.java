package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.annotation.Nullable;

/**
 * Data carrier (mapping to JSON) that contains protop "person"-like information.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponsePerson {
    @Nullable
    private String username;

    @Nullable
    private String email;

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(@Nullable final String username) {
        this.username = username;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable final String email) {
        this.email = email;
    }
}
