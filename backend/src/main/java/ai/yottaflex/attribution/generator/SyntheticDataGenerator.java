package ai.yottaflex.attribution.generator;

import ai.yottaflex.attribution.dto.GenerationRequest;
import ai.yottaflex.attribution.entity.*;
import ai.yottaflex.attribution.repository.*;
import ai.yottaflex.attribution.service.GenerationEventRecorder;
import ai.yottaflex.attribution.service.IdFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class SyntheticDataGenerator {
    private final ChannelRepository channels;
    private final CampaignRepository campaigns;
    private final CreativeRepository creatives;
    private final AudienceRepository audiences;
    private final CampaignAudienceRepository bridges;
    private final ContactRepository contacts;
    private final TouchpointRepository touchpoints;
    private final WebSessionRepository sessions;
    private final LeadRepository leads;
    private final OpportunityRepository opportunities;
    private final OrderRecordRepository orders;
    private final CostRecordRepository costs;
    private final DatasetRunRepository datasetRuns;
    private final GenerationEventRecorder events;

    public SyntheticDataGenerator(ChannelRepository channels, CampaignRepository campaigns,
            CreativeRepository creatives, AudienceRepository audiences,
            CampaignAudienceRepository bridges, ContactRepository contacts,
            TouchpointRepository touchpoints, WebSessionRepository sessions,
            LeadRepository leads, OpportunityRepository opportunities,
            OrderRecordRepository orders, CostRecordRepository costs,
            DatasetRunRepository datasetRuns, GenerationEventRecorder events) {
        this.channels = channels;
        this.campaigns = campaigns;
        this.creatives = creatives;
        this.audiences = audiences;
        this.bridges = bridges;
        this.contacts = contacts;
        this.touchpoints = touchpoints;
        this.sessions = sessions;
        this.leads = leads;
        this.opportunities = opportunities;
        this.orders = orders;
        this.costs = costs;
        this.datasetRuns = datasetRuns;
        this.events = events;
    }

    @Transactional
    public Map<String, Object> generate(GenerationRequest request) {
        int campaignCount = request.campaigns();
        int contactCount = request.contacts();
        int touchpointCount = request.touchpoints();
        int opportunityCount = request.opportunities();
        validateVolumes(campaignCount, contactCount, touchpointCount, opportunityCount);

        // Each generation is a durable, append-only snapshot.
        String runId = UUID.randomUUID().toString();
        String suffix = runId.substring(0, 8);
        Random random = new Random(request.seed());
        Instant base = Instant.parse("2025-01-01T00:00:00Z");
        String[] segments = {"SMB", "MID_MARKET", "ENTERPRISE", "STARTUP"};

        datasetRuns.save(new DatasetRun(runId, request.seed(), campaignCount,
                contactCount, touchpointCount, opportunityCount, Instant.now()));

        List<Channel> channelRows = createChannels(runId, suffix);
        channels.saveAll(channelRows);
        List<Audience> audienceRows = createAudiences(runId, suffix, segments);
        audiences.saveAll(audienceRows);

        List<Campaign> campaignRows = new ArrayList<>(campaignCount);
        Map<String, Channel> channelByCampaign = new HashMap<>();
        IdFactory campaignIds = new IdFactory("CMP-2026-", 6);
        for (int i = 0; i < campaignCount; i++) {
            Channel channel = channelRows.get(i % channelRows.size());
            LocalDate startDate = LocalDate.of(2025, 1, 1).plusDays(random.nextInt(300));
            Campaign campaign = new Campaign(campaignIds.next() + "-" + suffix,
                    "Campaign " + (i + 1) + " - " + channel.getName(), channel,
                    money(5000 + random.nextInt(95000)), "INR", startDate,
                    startDate.plusDays(365 + random.nextInt(100)), "COMPLETED", runId);
            campaignRows.add(campaign);
            channelByCampaign.put(campaign.getId(), channel);
        }
        campaigns.saveAll(campaignRows);

        List<CampaignAudience> bridgeRows = new ArrayList<>(campaignCount);
        for (int i = 0; i < campaignCount; i++) {
            bridgeRows.add(new CampaignAudience(campaignRows.get(i),
                    audienceRows.get(i % audienceRows.size()),
                    campaignRows.get(i).getStartDate(), null));
        }
        bridges.saveAll(bridgeRows);

        List<Creative> creativeRows = new ArrayList<>(campaignCount);
        Map<String, Creative> creativeByCampaign = new HashMap<>();
        for (int i = 0; i < campaignCount; i++) {
            Campaign campaign = campaignRows.get(i);
            Creative creative = new Creative("CRE-" + String.format("%06d", i + 1) + "-" + suffix,
                    "Creative " + (i + 1),
                    new String[]{"SEARCH_AD", "IMAGE", "VIDEO", "EMAIL"}[i % 4], campaign);
            creativeRows.add(creative);
            creativeByCampaign.put(campaign.getId(), creative);
        }
        creatives.saveAll(creativeRows);

        List<Contact> contactRows = new ArrayList<>(contactCount);
        for (int i = 0; i < contactCount; i++) {
            contactRows.add(new Contact("CON-2026-" + String.format("%06d", i + 1) + "-" + suffix,
                    segments[random.nextInt(segments.length)],
                    base.plusSeconds(random.nextInt(365 * 86400)), runId));
        }
        contacts.saveAll(contactRows);

        List<Instant> conversionTimes = new ArrayList<>(opportunityCount);
        for (int i = 0; i < opportunityCount; i++) {
            conversionTimes.add(base.plusSeconds(500L * 86400 + random.nextInt(90 * 86400)));
        }

        List<Touchpoint> touchRows = new ArrayList<>(touchpointCount + 1);
        int[] perContactSequence = new int[contactCount];
        for (int i = 0; i < touchpointCount; i++) {
            int contactIndex = random.nextInt(100) < 70
                    ? random.nextInt(opportunityCount) : random.nextInt(contactCount);
            Contact contact = contactRows.get(contactIndex);
            Campaign campaign = campaignRows.get(random.nextInt(campaignCount));
            Channel channel = channelByCampaign.get(campaign.getId());
            boolean offline = "OFFLINE".equals(channel.getChannelType());
            Instant eventAt;
            if (contactIndex < opportunityCount && random.nextInt(100) < 70) {
                eventAt = conversionTimes.get(contactIndex).minusSeconds(random.nextInt(170 * 86400));
            } else {
                eventAt = base.plusSeconds(random.nextInt(450 * 86400));
            }
            String touchId = IdFactory.contactTouch(contact.getId(),
                    ++perContactSequence[contactIndex]) + "-" + suffix;
            touchRows.add(new Touchpoint(touchId, contact, campaign,
                    creativeByCampaign.get(campaign.getId()), channel, eventAt,
                    offline ? "CALL" : new String[]{"CLICK", "IMPRESSION", "EMAIL_OPEN"}[random.nextInt(3)],
                    offline ? "OFFLINE" : "ONLINE"));
        }

        List<WebSession> sessionRows = new ArrayList<>(opportunityCount + 1);
        List<Lead> leadRows = new ArrayList<>(opportunityCount);
        List<Opportunity> opportunityRows = new ArrayList<>(opportunityCount);
        List<OrderRecord> orderRows = new ArrayList<>(opportunityCount + 1);
        for (int i = 0; i < opportunityCount; i++) {
            Contact contact = contactRows.get(i);
            Instant conversion = conversionTimes.get(i);
            String sequence = String.format("%06d", i + 1);
            WebSession session = new WebSession("SES-2026-" + sequence + "-" + suffix,
                    contact, null, conversion.minusSeconds(random.nextInt(45) * 86400L),
                    "/solutions/" + segments[i % segments.length].toLowerCase(Locale.ROOT), true, runId);
            sessionRows.add(session);
            Instant leadAt = conversion.minusSeconds(random.nextInt(60) * 86400L + 3600);
            Lead lead = new Lead("LEAD-2026-" + sequence + "-" + suffix,
                    contact, session, leadAt, "CONVERTED");
            leadRows.add(lead);
            Instant closedAt = conversion.plusSeconds(random.nextInt(30) * 86400L);
            BigDecimal revenue = money(10000 + random.nextInt(190000));
            Opportunity opportunity = new Opportunity("OPP-2026-" + sequence + "-" + suffix,
                    lead, leadAt, closedAt, "WON", revenue, "INR");
            opportunityRows.add(opportunity);
            orderRows.add(new OrderRecord("ORD-2026-" + sequence + "-" + suffix,
                    opportunity, "REV-" + sequence + "-" + suffix, revenue, "INR", closedAt));
        }

        if (!opportunityRows.isEmpty()) {
            Opportunity first = opportunityRows.getFirst();
            Contact contact = first.getLead().getContact();
            Campaign campaign = campaignRows.getFirst();
            Channel channel = channelByCampaign.get(campaign.getId());
            touchRows.add(new Touchpoint("TOUCH-EDGE-POST-" + suffix, contact, campaign,
                    creativeByCampaign.get(campaign.getId()), channel,
                    first.getClosedAt().plusSeconds(86400), "CLICK", "ONLINE"));
            OrderRecord firstOrder = orderRows.getFirst();
            orderRows.add(new OrderRecord("ORD-EDGE-DUP-" + suffix, first,
                    firstOrder.getExternalRevenueKey(), firstOrder.getRevenue(),
                    "INR", firstOrder.getRecognizedAt()));
            sessionRows.add(new WebSession("SES-EDGE-ANON-" + suffix, contact,
                    "anon-no-evidence-" + suffix, base.plusSeconds(200L * 86400),
                    "/pricing", false, runId));
        }

        touchpoints.saveAll(touchRows);
        sessions.saveAll(sessionRows);
        leads.saveAll(leadRows);
        opportunities.saveAll(opportunityRows);
        orders.saveAll(orderRows);

        List<CostRecord> costRows = new ArrayList<>();
        for (int i = 0; i < campaignCount; i++) {
            if (i == 4) continue;
            Campaign campaign = campaignRows.get(i);
            BigDecimal spend = campaign.getBudget().multiply(new BigDecimal("0.72"))
                    .setScale(2, RoundingMode.HALF_UP);
            costRows.add(new CostRecord("COST-" + String.format("%06d", i + 1) + "-" + suffix,
                    campaign, campaign.getStartDate().plusDays(1), spend,
                    i == 7 ? "USD" : "INR", "MEDIA_SPEND"));
        }
        costs.saveAll(costRows);

        events.recordCreated("DATASET_RUN", List.of(runId), runId, "GENERATED");
        events.recordCreated("CAMPAIGN", campaignRows.stream().map(Campaign::getId).toList(), runId, "PLANNED");
        events.recordTransition("CAMPAIGN", campaignRows.stream().map(Campaign::getId).toList(),
                "PLANNED", "ACTIVE", runId);
        events.recordTransition("CAMPAIGN", campaignRows.stream().map(Campaign::getId).toList(),
                "ACTIVE", "COMPLETED", runId);
        events.recordCreated("CONTACT", contactRows.stream().map(Contact::getId).toList(), runId);
        events.recordCreated("TOUCHPOINT", touchRows.stream().map(Touchpoint::getId).toList(), runId);
        events.recordCreated("LEAD", leadRows.stream().map(Lead::getId).toList(), runId, "NEW");
        events.recordTransition("LEAD", leadRows.stream().map(Lead::getId).toList(),
                "NEW", "QUALIFIED", runId);
        events.recordTransition("LEAD", leadRows.stream().map(Lead::getId).toList(),
                "QUALIFIED", "CONVERTED", runId);
        events.recordCreated("OPPORTUNITY", opportunityRows.stream().map(Opportunity::getId).toList(), runId, "OPEN");
        events.recordTransition("OPPORTUNITY", opportunityRows.stream().map(Opportunity::getId).toList(),
                "OPEN", "WON", runId);

        return Map.of("runId", runId, "seed", request.seed(),
                "campaigns", campaignCount, "contacts", contactCount,
                "touchpoints", touchRows.size(), "opportunities", opportunityCount,
                "orders", orderRows.size(), "costRecords", costRows.size(),
                "labelledEdgeCases", 5);
    }

    private static void validateVolumes(int campaigns, int contacts, int touchpoints, int opportunities) {
        if (campaigns < 1 || contacts < 1 || touchpoints < 1 || opportunities < 1)
            throw new IllegalArgumentException("All generated volumes must be greater than zero.");
        if (opportunities > contacts)
            throw new IllegalArgumentException("Opportunity count cannot exceed contact count.");
        if (campaigns > 500 || contacts > 50000 || touchpoints > 200000 || opportunities > 10000)
            throw new IllegalArgumentException("Requested volume exceeds the documented FULL scale.");
    }

    private static List<Channel> createChannels(String runId, String suffix) {
        String[][] definitions = {{"CH-001", "Paid Search", "PAID"},
                {"CH-002", "Paid Social", "SOCIAL"}, {"CH-003", "Email", "EMAIL"},
                {"CH-004", "Organic Search", "ORGANIC"},
                {"CH-005", "Partner Referral", "REFERRAL"},
                {"CH-006", "Field Event", "OFFLINE"}};
        List<Channel> rows = new ArrayList<>();
        for (String[] definition : definitions) {
            rows.add(new Channel(definition[0] + "-" + suffix, definition[1],
                    definition[2], runId));
        }
        return rows;
    }

    private static List<Audience> createAudiences(String runId, String suffix, String[] segments) {
        List<Audience> rows = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            rows.add(new Audience("AUD-" + String.format("%03d", i + 1) + "-" + suffix,
                    segments[i], "Synthetic segment: " + segments[i], runId));
        }
        return rows;
    }

    private static BigDecimal money(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
