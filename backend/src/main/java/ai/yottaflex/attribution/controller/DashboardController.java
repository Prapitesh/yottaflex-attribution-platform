package ai.yottaflex.attribution.controller;

import ai.yottaflex.attribution.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard){this.dashboard=dashboard;}

    @GetMapping("/dashboard/summary")
    public Map<String,Object> summary(@RequestParam(required=false) String runId) {
        return dashboard.summary(runId);
    }

    @GetMapping("/campaigns")
    public List<Map<String,Object>> campaigns(
            @RequestParam(defaultValue="LINEAR") String model,
            @RequestParam(required=false) String runId) {
        return dashboard.campaignRoi(model.toUpperCase(Locale.ROOT),runId);
    }

    @GetMapping("/attribution/results")
    public List<Map<String,Object>> attribution(
            @RequestParam(defaultValue="LINEAR") String model,
            @RequestParam(required=false) String runId) {
        return dashboard.attributionByModel(model.toUpperCase(Locale.ROOT),runId);
    }
}
