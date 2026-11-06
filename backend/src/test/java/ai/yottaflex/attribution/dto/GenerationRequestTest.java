package ai.yottaflex.attribution.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenerationRequestTest {
    @Test
    void defaultsToStarterScale() {
        GenerationRequest request=new GenerationRequest(20261022L,null,null,null,null,null);
        assertEquals(GenerationRequest.Scale.STARTER,request.scale());
        assertEquals(12,request.campaigns());
        assertEquals(300,request.contacts());
        assertEquals(1200,request.touchpoints());
        assertEquals(80,request.opportunities());
    }

    @Test
    void fullScaleMatchesChallengeVolumes() {
        GenerationRequest request=new GenerationRequest(20261022L,
            GenerationRequest.Scale.FULL,null,null,null,null);
        assertEquals(500,request.campaigns());
        assertEquals(50000,request.contacts());
        assertEquals(200000,request.touchpoints());
        assertEquals(10000,request.opportunities());
    }

    @Test
    void customVolumesOverrideDefaults() {
        GenerationRequest request=new GenerationRequest(7L,
            GenerationRequest.Scale.STARTER,5,100,250,20);
        assertEquals(5,request.campaigns());
        assertEquals(100,request.contacts());
        assertEquals(250,request.touchpoints());
        assertEquals(20,request.opportunities());
    }
}
