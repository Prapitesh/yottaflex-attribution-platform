package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.WebSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSessionIdentityTest {
    @Test
    void unverifiedAnonymousSessionMustNotBeAssociatedWithContact() {
        WebSession session = new WebSession(
            "SES-ANON-1",
            null,
            "anon-no-evidence",
            Instant.parse("2026-01-01T00:00:00Z"),
            "/pricing",
            false,
            "RUN-1"
        );

        assertNull(session.getContact());
        assertFalse(session.isIdentityLinkVerified());
        assertEquals("anon-no-evidence", session.getAnonymousId());
        assertEquals("RUN-1", session.getDatasetRunId());
    }
}
