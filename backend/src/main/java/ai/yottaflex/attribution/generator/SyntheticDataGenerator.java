package ai.yottaflex.attribution.generator;

import ai.yottaflex.attribution.dto.GenerationRequest;
import ai.yottaflex.attribution.entity.*;
import ai.yottaflex.attribution.repository.*;
import ai.yottaflex.attribution.service.GenerationEventRecorder;
import ai.yottaflex.attribution.service.IdFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
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
    private final GenerationEventRecorder events;

    public SyntheticDataGenerator(ChannelRepository channels, CampaignRepository campaigns,
            CreativeRepository creatives, AudienceRepository audiences, CampaignAudienceRepository bridges,
            ContactRepository contacts, TouchpointRepository touchpoints, WebSessionRepository sessions,
            LeadRepository leads, OpportunityRepository opportunities, OrderRecordRepository orders,
            CostRecordRepository costs, GenerationEventRecorder events) {
        this.channels=channels; this.campaigns=campaigns; this.creatives=creatives; this.audiences=audiences;
        this.bridges=bridges; this.contacts=contacts; this.touchpoints=touchpoints; this.sessions=sessions;
        this.leads=leads; this.opportunities=opportunities; this.orders=orders; this.costs=costs; this.events=events;
    }

    @Transactional
    public Map<String,Object> generate(GenerationRequest request) {
        int nc=request.campaigns(), ncontact=request.contacts(), nt=request.touchpoints(), no=request.opportunities();
        if(nc<1||ncontact<1||nt<1||no<1) throw new IllegalArgumentException("All generated volumes must be >= 1");
        if(no>ncontact) throw new IllegalArgumentException("Opportunity count cannot exceed contact count in this generator");
        orders.deleteAllInBatch(); opportunities.deleteAllInBatch(); leads.deleteAllInBatch(); sessions.deleteAllInBatch();
        touchpoints.deleteAllInBatch(); costs.deleteAllInBatch(); bridges.deleteAllInBatch(); creatives.deleteAllInBatch();
        campaigns.deleteAllInBatch(); contacts.deleteAllInBatch(); audiences.deleteAllInBatch(); channels.deleteAllInBatch();

        Random random=new Random(request.seed());
        Instant base=Instant.parse("2025-01-01T00:00:00Z");
        String[] segments={"SMB","MID_MARKET","ENTERPRISE","STARTUP"};
        String[][] channelDefs={{"CH-001","Paid Search","PAID"},{"CH-002","Paid Social","SOCIAL"},
            {"CH-003","Email","EMAIL"},{"CH-004","Organic Search","ORGANIC"},
            {"CH-005","Partner Referral","REFERRAL"},{"CH-006","Field Event","OFFLINE"}};
        List<Channel> channelRows=new ArrayList<>();
        for(String[] d:channelDefs) channelRows.add(new Channel(d[0],d[1],d[2]));
        channels.saveAll(channelRows);

        List<Audience> audienceRows=new ArrayList<>();
        for(int i=0;i<segments.length;i++) audienceRows.add(new Audience(String.format("AUD-%03d",i+1),segments[i],"Synthetic segment: "+segments[i]));
        audiences.saveAll(audienceRows);

        List<Campaign> campaignRows=new ArrayList<>(nc);
        Map<String,Channel> channelByCampaign=new HashMap<>();
        IdFactory campaignIds=new IdFactory("CMP-2026-",6);
        for(int i=0;i<nc;i++){
            Channel channel=channelRows.get(i%channelRows.size());
            LocalDate from=LocalDate.of(2025,1,1).plusDays(random.nextInt(300));
            Campaign campaign=new Campaign(campaignIds.next(),"Campaign "+(i+1)+" - "+channel.getName(),channel,
                money(5000+random.nextInt(95000)),"INR",from,from.plusDays(365+random.nextInt(100)),"COMPLETED");
            campaignRows.add(campaign); channelByCampaign.put(campaign.getId(),channel);
        }
        campaigns.saveAll(campaignRows);

        List<CampaignAudience> bridgeRows=new ArrayList<>();
        for(int i=0;i<nc;i++) bridgeRows.add(new CampaignAudience(campaignRows.get(i),audienceRows.get(i%audienceRows.size()),campaignRows.get(i).getStartDate(),null));
        bridges.saveAll(bridgeRows);

        List<Creative> creativeRows=new ArrayList<>();
        Map<String,Creative> creativeByCampaign=new HashMap<>();
        for(int i=0;i<nc;i++){
            Campaign campaign=campaignRows.get(i);
            Creative creative=new Creative("CRE-"+String.format("%06d",i+1),"Creative "+(i+1),
                new String[]{"SEARCH_AD","IMAGE","VIDEO","EMAIL"}[i%4],campaign);
            creativeRows.add(creative); creativeByCampaign.put(campaign.getId(),creative);
        }
        creatives.saveAll(creativeRows);

        List<Contact> contactRows=new ArrayList<>(ncontact);
        for(int i=0;i<ncontact;i++){
            contactRows.add(new Contact(String.format("CON-2026-%06d",i+1),segments[random.nextInt(segments.length)],
                base.plusSeconds(random.nextInt(365*86400))));
        }
        contacts.saveAll(contactRows);

        List<Instant> conversionTimes=new ArrayList<>(no);
        for(int i=0;i<no;i++) conversionTimes.add(base.plusSeconds(500L*86400+random.nextInt(90*86400)));
        List<Touchpoint> touchRows=new ArrayList<>(nt+1);
        int[] perContactSequence=new int[ncontact];
        // 70% of base touches are assigned to contacts with opportunities to make real attribution journeys.
        for(int i=0;i<nt;i++){
            int contactIndex=random.nextInt(100)<70?random.nextInt(no):random.nextInt(ncontact);
            Contact contact=contactRows.get(contactIndex);
            Campaign campaign=campaignRows.get(random.nextInt(nc));
            Channel channel=channelByCampaign.get(campaign.getId());
            boolean offline="OFFLINE".equals(channel.getChannelType());
            Instant at;
            if(contactIndex<no && random.nextInt(100)<70){
                Instant conversion=conversionTimes.get(contactIndex);
                at=conversion.minusSeconds(random.nextInt(170*86400));
            }else{
                at=base.plusSeconds(random.nextInt(450*86400));
            }
            touchRows.add(new Touchpoint(IdFactory.contactTouch(contact.getId(),++perContactSequence[contactIndex]),
                contact,campaign,creativeByCampaign.get(campaign.getId()),channel,at,
                offline?"CALL":new String[]{"CLICK","IMPRESSION","EMAIL_OPEN"}[random.nextInt(3)],
                offline?"OFFLINE":"ONLINE"));
        }

        List<WebSession> sessionRows=new ArrayList<>(no+1);
        List<Lead> leadRows=new ArrayList<>(no);
        List<Opportunity> opportunityRows=new ArrayList<>(no);
        List<OrderRecord> orderRows=new ArrayList<>(no+1);
        for(int i=0;i<no;i++){
            Contact contact=contactRows.get(i);
            Instant conversion=conversionTimes.get(i);
            WebSession session=new WebSession(String.format("SES-2026-%06d",i+1),contact,null,
                conversion.minusSeconds(random.nextInt(45)*86400L),"/solutions/"+segments[i%segments.length].toLowerCase(Locale.ROOT),true);
            sessionRows.add(session);
            Instant leadAt=conversion.minusSeconds(random.nextInt(60)*86400L+3600);
            Lead lead=new Lead(String.format("LEAD-2026-%06d",i+1),contact,session,leadAt,"CONVERTED");
            leadRows.add(lead);
            Instant closedAt=conversion.plusSeconds(random.nextInt(30)*86400L);
            BigDecimal revenue=money(10000+random.nextInt(190000));
            Opportunity opportunity=new Opportunity(String.format("OPP-2026-%06d",i+1),lead,leadAt,closedAt,"WON",revenue,"INR");
            opportunityRows.add(opportunity);
            orderRows.add(new OrderRecord(String.format("ORD-2026-%06d",i+1),opportunity,
                "REV-"+String.format("%06d",i+1),revenue,"INR",closedAt));
        }

        // Five labelled exception cases. Keep the exception rows in their own traceable records.
        if(no>0 && nc>0){
            Opportunity first=opportunityRows.getFirst();
            Contact contact=first.getLead().getContact();
            Campaign campaign=campaignRows.getFirst();
            Channel channel=channelByCampaign.get(campaign.getId());
            touchRows.add(new Touchpoint("TOUCH-EDGE-POST-CONVERSION",contact,campaign,creativeByCampaign.get(campaign.getId()),
                channel,first.getClosedAt().plusSeconds(86400),"CLICK","ONLINE"));
            OrderRecord firstOrder=orderRows.getFirst();
            orderRows.add(new OrderRecord("ORD-EDGE-DUPLICATE",first,firstOrder.getExternalRevenueKey(),
                firstOrder.getRevenue(),"INR",firstOrder.getRecognizedAt()));
            sessionRows.add(new WebSession("SES-EDGE-ANON-001",contact,"anon-no-evidence",
                base.plusSeconds(200L*86400),"/pricing",false));
        }

        // Persist in parent-first order, including deliberate edge rows.
        touchpoints.saveAll(touchRows);
        sessions.saveAll(sessionRows);
        leads.saveAll(leadRows);
        opportunities.saveAll(opportunityRows);
        orders.saveAll(orderRows);

        List<CostRecord> costRows=new ArrayList<>();
        for(int i=0;i<nc;i++){
            if(i==4) continue; // deliberate omitted cost.
            Campaign campaign=campaignRows.get(i);
            BigDecimal spend=campaign.getBudget().multiply(new BigDecimal("0.72")).setScale(2,RoundingMode.HALF_UP);
            costRows.add(new CostRecord(String.format("COST-%06d",i+1),campaign,campaign.getStartDate().plusDays(1),
                spend,i==7?"USD":"INR","MEDIA_SPEND")); // campaign 8 deliberately has currency mismatch.
        }
        costs.saveAll(costRows);

        events.recordCreated("CAMPAIGN",campaignRows.stream().map(Campaign::getId).toList());
        events.recordCreated("CONTACT",contactRows.stream().map(Contact::getId).toList());
        events.recordCreated("TOUCHPOINT",touchRows.stream().map(Touchpoint::getId).toList());
        events.recordCreated("OPPORTUNITY",opportunityRows.stream().map(Opportunity::getId).toList());

        return Map.of("seed",request.seed(),"campaigns",nc,"contacts",ncontact,"touchpoints",touchRows.size(),
            "opportunities",no,"orders",orderRows.size(),"costRecords",costRows.size(),"labelledEdgeCases",5);
    }
    private static BigDecimal money(int value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
}
