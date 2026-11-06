package ai.yottaflex.attribution.controller;

import ai.yottaflex.attribution.entity.DataQualityIssue;
import ai.yottaflex.attribution.repository.DataQualityIssueRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/quality")
public class QualityController {
    private final DataQualityIssueRepository issues;

    public QualityController(DataQualityIssueRepository issues) {
        this.issues = issues;
    }

    @GetMapping("/issues")
    public List<Map<String, Object>> issues(@RequestParam(required = false) String runId) {
        List<DataQualityIssue> rows = runId == null
                ? issues.findAllByOrderByDetectedAtDesc()
                : issues.findByDatasetRunIdOrderByDetectedAtDesc(runId);
        return rows.stream().map(issue -> Map.<String, Object>of(
                "id", issue.getId(),
                "type", issue.getIssueType(),
                "severity", issue.getSeverity(),
                "description", issue.getDescription()
        )).toList();
    }
}
