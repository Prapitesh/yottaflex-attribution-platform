package ai.yottaflex.attribution.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class DashboardServiceTest {
    @Test
    void campaignRoiQuarantinesAllSpendWhenAnyCostCurrencyMismatches() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        new DashboardService(jdbc).campaignRoi("LINEAR", null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("bool_or(cost.currency<>campaign.currency)"));
        assertTrue(sql.getValue().contains("s.has_currency_mismatch"));
        assertTrue(sql.getValue().contains("then 'CURRENCY_MISMATCH'"));
        assertTrue(sql.getValue().contains("or s.has_currency_mismatch or s.amount=0"));
    }
}
