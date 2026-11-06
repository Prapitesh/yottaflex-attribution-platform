package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.EntityEvent;
import ai.yottaflex.attribution.repository.EntityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class GenerationEventRecorder {
    private final EntityEventRepository events;
    public GenerationEventRecorder(EntityEventRepository events){this.events=events;}

    @Transactional
    public void recordCreated(String type,List<String> ids) {
        recordCreated(type,ids,null);
    }

    @Transactional
    public void recordCreated(String type,List<String> ids,String datasetRunId) {
        Instant now=Instant.now();
        Map<String,Object> metadata=datasetRunId==null?Map.of():Map.of("datasetRunId",datasetRunId);
        events.saveAll(ids.stream().map(id->new EntityEvent(type,id,
            "CREATED",null,"ACTIVE",now,metadata)).toList());
    }
}
