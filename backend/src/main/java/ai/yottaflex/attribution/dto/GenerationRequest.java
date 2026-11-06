package ai.yottaflex.attribution.dto;

import jakarta.validation.constraints.Min;

public record GenerationRequest(
    long seed,
    Scale scale,
    @Min(1) Integer campaignCount,
    @Min(1) Integer contactCount,
    @Min(1) Integer touchpointCount,
    @Min(1) Integer opportunityCount
) {
    public enum Scale { STARTER, FULL }
    public GenerationRequest {
        if (scale == null) scale = Scale.STARTER;
    }
    public int campaigns(){ return campaignCount != null ? campaignCount : (scale == Scale.FULL ? 500 : 12); }
    public int contacts(){ return contactCount != null ? contactCount : (scale == Scale.FULL ? 50_000 : 300); }
    public int touchpoints(){ return touchpointCount != null ? touchpointCount : (scale == Scale.FULL ? 200_000 : 1_200); }
    public int opportunities(){ return opportunityCount != null ? opportunityCount : (scale == Scale.FULL ? 10_000 : 80); }
}
