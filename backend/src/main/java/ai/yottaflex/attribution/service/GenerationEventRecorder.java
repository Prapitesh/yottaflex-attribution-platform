package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.EntityEvent;
import ai.yottaflex.attribution.repository.EntityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class GenerationEventRecorder {
    private final EntityEventRepository events;
    public GenerationEventRecorder(EntityEventRepository events){this.events=events;}
    @Transactional
    public void recordCreated(String type,List<String> ids){
        Instant now=Instant.now();
        events.saveAll(ids.stream().map(id->new EntityEvent(type,id,"CREATED",null,"ACTIVE",now,"{}")).toList());
    }
}
