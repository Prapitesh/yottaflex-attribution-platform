package ai.yottaflex.attribution.controller;

import ai.yottaflex.attribution.dto.GenerationRequest;
import ai.yottaflex.attribution.generator.SyntheticDataGenerator;
import ai.yottaflex.attribution.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final SyntheticDataGenerator generator;
    private final AttributionEngine engine;
    private final DataQualityService quality;
    public AdminController(SyntheticDataGenerator generator,AttributionEngine engine,DataQualityService quality){this.generator=generator;this.engine=engine;this.quality=quality;}
    @PostMapping("/generate") public Map<String,Object> generate(@Valid @RequestBody GenerationRequest request){return generator.generate(request);}
    @PostMapping("/recalculate") public Map<String,Object> recalculate(){return engine.calculate();}
    @PostMapping("/validate") public Map<String,Object> validate(){return quality.inspect();}
}
