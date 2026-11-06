package ai.yottaflex.attribution.controller;

import ai.yottaflex.attribution.repository.DataQualityIssueRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/quality")
public class QualityController {
    private final DataQualityIssueRepository issues;
    public QualityController(DataQualityIssueRepository issues){this.issues=issues;}
    @GetMapping("/issues")
    public List<Map<String,Object>> issues(){
        return issues.findAllByOrderByDetectedAtDesc().stream().map(i->Map.<String,Object>of(
            "id",i.getId(),"type",i.getIssueType(),"severity",i.getSeverity(),"description",i.getDescription())).toList();
    }
}
