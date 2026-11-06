package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.EntityEvent;
import ai.yottaflex.attribution.repository.EntityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationEventRecorderTest {
    @Mock private EntityEventRepository repository;

    @Test
    void createdEventsRetainRunMetadata() {
        GenerationEventRecorder recorder=new GenerationEventRecorder(repository);
        recorder.recordCreated("CAMPAIGN",List.of("CMP-1","CMP-2"),"RUN-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EntityEvent>> captor=ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<EntityEvent> saved=captor.getValue();

        assertEquals(2,saved.size());
        assertEquals("CAMPAIGN",saved.getFirst().getEntityType());
        assertEquals("CREATED",saved.getFirst().getEventType());
        assertEquals("RUN-1",saved.getFirst().getMetadata().get("datasetRunId"));
    }
    @Test
    void statusTransitionsRetainOldAndNewStateAndRunMetadata() {
        GenerationEventRecorder recorder=new GenerationEventRecorder(repository);
        recorder.recordTransition("OPPORTUNITY",List.of("OPP-1"),"OPEN","WON","RUN-2");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EntityEvent>> captor=ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        EntityEvent event=captor.getValue().getFirst();

        assertEquals("STATUS_CHANGED",event.getEventType());
        assertEquals("OPEN",event.getOldState());
        assertEquals("WON",event.getNewState());
        assertEquals("RUN-2",event.getMetadata().get("datasetRunId"));
    }

    @Test
    void unsupportedLifecycleTransitionIsRejected() {
        GenerationEventRecorder recorder=new GenerationEventRecorder(repository);
        assertThrows(IllegalArgumentException.class,
            ()->recorder.recordTransition("OPPORTUNITY",List.of("OPP-1"),"WON","OPEN","RUN-3"));
        verifyNoInteractions(repository);
    }


}
