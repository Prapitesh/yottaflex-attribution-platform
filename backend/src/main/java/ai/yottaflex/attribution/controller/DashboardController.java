package ai.yottaflex.attribution.controller;

import ai.yottaflex.attribution.service.DashboardService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard){this.dashboard=dashboard;}
    @GetMapping("/dashboard/summary") public Map<String,Object> summary(){return dashboard.summary();}
    @GetMapping("/campaigns") public List<Map<String,Object>> campaigns(@RequestParam(defaultValue="LINEAR") String model){return dashboard.campaignRoi(model.toUpperCase());}
    @GetMapping("/attribution/results") public List<Map<String,Object>> attribution(@RequestParam(defaultValue="LINEAR") String model){return dashboard.attributionByModel();}
}
