package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.EntityEvent;
import ai.yottaflex.attribution.repository.EntityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GenerationEventRecorder {
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        "CAMPAIGN", Set.of("PLANNED->ACTIVE", "ACTIVE->COMPLETED", "ACTIVE->PAUSED",
            "PAUSED->ACTIVE", "PAUSED->COMPLETED"),
        "LEAD", Set.of("NEW->QUALIFIED", "NEW->DISQUALIFIED", "QUALIFIED->CONVERTED",
            "QUALIFIED->DISQUALIFIED"),
        "OPPORTUNITY", Set.of("OPEN->WON", "OPEN->LOST"),
        "DATASET_RUN", Set.of("GENERATED->VALIDATED", "GENERATED->FAILED")
    );

    private final EntityEventRepository events;

    public GenerationEventRecorder(EntityEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void recordCreated(String type, List<String> ids) {
        recordCreated(type, ids, null, "ACTIVE");
    }

    @Transactional
    public void recordCreated(String type, List<String> ids, String datasetRunId) {
        recordCreated(type, ids, datasetRunId, "ACTIVE");
    }

    @Transactional
    public void recordCreated(String type, List<String> ids, String datasetRunId, String initialState) {
        Instant now = Instant.now();
        Map<String, Object> metadata = metadata(datasetRunId);
        events.saveAll(ids.stream()
            .map(id -> new EntityEvent(type, id, "CREATED", null, initialState, now, metadata))
            .toList());
    }

    @Transactional
    public void recordTransition(String type, List<String> ids, String oldState,
            String newState, String datasetRunId) {
        String transition = oldState + "->" + newState;
        if (!ALLOWED_TRANSITIONS.getOrDefault(type, Set.of()).contains(transition)) {
            throw new IllegalArgumentException("Unsupported " + type + " lifecycle transition: " + transition);
        }

        Instant now = Instant.now();
        Map<String, Object> metadata = metadata(datasetRunId);
        events.saveAll(ids.stream()
            .map(id -> new EntityEvent(type, id, "STATUS_CHANGED", oldState, newState, now, metadata))
            .toList());
    }

    private Map<String, Object> metadata(String datasetRunId) {
        return datasetRunId == null ? Map.of() : Map.of("datasetRunId", datasetRunId);
    }
}
