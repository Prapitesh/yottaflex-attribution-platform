package ai.yottaflex.attribution.controller;

import ai.yottaflex.attribution.entity.DatasetRun;
import ai.yottaflex.attribution.repository.DatasetRunRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dataset-runs")
public class DatasetRunController {
    private final DatasetRunRepository runs;

    public DatasetRunController(DatasetRunRepository runs) {
        this.runs=runs;
    }

    @GetMapping
    public List<DatasetRunResponse> listRuns() {
        return runs.findAllByOrderByCreatedAtDesc().stream()
            .map(DatasetRunResponse::from)
            .toList();
    }

    public record DatasetRunResponse(
            String id,
            long seed,
            int campaigns,
            int contacts,
            int touchpoints,
            int opportunities,
            Instant createdAt,
            String status) {
        static DatasetRunResponse from(DatasetRun run) {
            return new DatasetRunResponse(run.getId(),run.getSeed(),
                run.getCampaignCount(),run.getContactCount(),
                run.getTouchpointCount(),run.getOpportunityCount(),
                run.getCreatedAt(),run.getStatus());
        }
    }
}
