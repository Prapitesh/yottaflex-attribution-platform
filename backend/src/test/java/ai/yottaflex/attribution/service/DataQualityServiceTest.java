package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.repository.DataQualityIssueRepository;
import ai.yottaflex.attribution.repository.GroundTruthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class DataQualityServiceTest {
    @Test
    void flagsUnverifiedAnonymousSessionEvenWhenItHasNoContactLink() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DataQualityIssueRepository issues = mock(DataQualityIssueRepository.class);
        GroundTruthRepository truth = mock(GroundTruthRepository.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("from web_sessions")) {
                return List.of(Map.of("session_id", "SES-ANON-UNLINKED"));
            }
            return List.of();
        });

        Map<String, Object> result = new DataQualityService(issues, truth, jdbc).inspect("RUN-1");

        assertEquals(1, result.get("issueCount"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) result.get("byType");
        assertEquals(1L, byType.get("UNVERIFIED_IDENTITY_LINK"));
        verify(jdbc).queryForList(
            argThat(sql -> sql.contains("from web_sessions") && !sql.contains("contact_id is not null")),
            any(Object[].class)
        );
    }
}
