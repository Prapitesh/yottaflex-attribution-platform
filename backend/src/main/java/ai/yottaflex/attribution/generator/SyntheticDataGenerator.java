package ai.yottaflex.attribution.generator;

import ai.yottaflex.attribution.dto.GenerationRequest;
import ai.yottaflex.attribution.entity.*;
import ai.yottaflex.attribution.repository.*;
import ai.yottaflex.attribution.service.IdFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class SyntheticDataGenerator {
    private final ChannelRepository channels;
    private final CampaignRepository campaigns;
    private final CreativeRepository creatives;
    private final AudienceRepository audiences;
    private final ContactRepository contacts;
    private final TouchpointRepository touchpoints;
    private final WebSessionRepository sessions;
    private final LeadRepository leads;
    private final OpportunityRepository opportunities;
    private final OrderRecordRepository orders;
    private final CostRecordRepository costs;

    public SyntheticDataGenerator(ChannelRepository channels, CampaignRepository campaigns,
            CreativeRepository creatives, AudienceRepository audiences, ContactRepository contacts,
            TouchpointRepository touchpoints, WebSessionRepository sessions, LeadRepository leads,
            OpportunityRepository opportunities, OrderRecordRepository orders, CostRecordRepository costs) {
        this.channels=channels; this.campaigns=campaigns; this.creatives=creatives; this.audiences=audiences;
        this.contacts=contacts; this.touchpoints=touchpoints; this.sessions=sessions; this.leads=leads;
        this.opportunities=opportunities; this.orders=orders; this.costs=costs;
    }

    @Transactional
    public Map<String,Object> generate(GenerationRequest request) {
        // Generation is an explicit new dataset run: clear dependent rows before parents.
        orders.deleteAllInBatch(); opportunities.deleteAllInBatch(); leads.deleteAllInBatch();
        sessions.deleteAllInBatch(); touchpoints.deleteAllInBatch(); costs.deleteAllInBatch();
        creatives.deleteAllInBatch(); campaigns.deleteAllInBatch(); contacts.deleteAllInBatch();
        audiences.deleteAllInBatch(); channels.deleteAllInBatch();

        Random random = new Random(request.seed());
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        List<Channel> channelRows = new ArrayList<>();
        String[][] channelDefs = {{"CH-001","Paid Search","PAID"},{"CH-002","Paid Social","SOCIAL"},
                {"CH-003","Email","EMAIL"},{"CH-004","Organic Search","ORGANIC"},
                {"CH-005","Partner Referral","REFERRAL"},{"CH-006","Field Event","OFFLINE"}};
        for (String[] d:channelDefs) channelRows.add(new Channel(d[0],d[1],d[2]));
        channels.saveAll(channelRows);

        List<Audience> audienceRows = new ArrayList<>();
        String[] segments={"SMB","MID_MARKET","ENTERPRISE","STARTUP"};
        for(int i=0;i<segments.length;i++) audienceRows.add(new Audience(String.format("AUD-%03d",i+1),segments[i], "Synthetic segment: "+segments[i]));
        audiences.saveAll(audienceRows);

        int campaignCount=request.campaigns(), contactCount=request.contacts();
        int touchpointCount=request.touchpoints(), opportunityCount=request.opportunities();
        IdFactory campaignIds=new IdFactory("CMP-2026-",6);
        List<Campaign> campaignRows=new ArrayList<>(campaignCount);
        Map<String,Campaign> campaignMap=new HashMap<>();
        Map<String,Channel> campaignChannels=new HashMap<>();
        for(int i=0;i<campaignCount;i++){
            Channel ch=channelRows.get(i%channelRows.size());
            String cid=campaignIds.next();
            BigDecimal budget=money(5000+random.nextInt(95000));
            Campaign c=new Campaign(cid,"Campaign "+(i+1)+" - "+ch.getName(),ch,budget,"INR",
                    LocalDate.of(2025,1,1).plusDays(random.nextInt(300)),
                    LocalDate.of(2025,1,1).plusDays(365+random.nextInt(100)),"COMPLETED");
            campaignRows.add(c);campaignMap.put(cid,c);campaignChannels.put(cid,ch);
        }
        campaigns.saveAll(campaignRows);

        List<Creative> creativeRows=new ArrayList<>(campaignCount);
        Map<String,Creative> creativeMap=new HashMap<>();
        for(int i=0;i<campaignRows.size();i++){
            Campaign c=campaignRows.get(i);
            Creative cr=new Creative("CRE-"+String.format("%06d",i+1),"Creative "+(i+1),
                    new String[]{"SEARCH_AD","IMAGE","VIDEO","EMAIL"}[i%4],c);
            creativeRows.add(cr);creativeMap.put(c.getId(),cr);
        }
        creatives.saveAll(creativeRows);

        List<Contact> contactRows=new ArrayList<>(contactCount);
        Map<String,Contact> contactMap=new HashMap<>();
        for(int i=0;i<contactCount;i++){
            String cid=String.format("CON-2026-%06d",i+1);
            Contact c=new Contact(cid,segments[random.nextInt(segments.length)],start.plusSeconds(random.nextInt(365*86400)));
            contactRows.add(c);contactMap.put(cid,c);
        }
        contacts.saveAll(contactRows);

        // Touchpoints are generated for contacts with at least one opportunity journey.
        List<Touchpoint> touchRows=new ArrayList<>(touchpointCount);
        Map<String,List<Touchpoint>> byContact=new HashMap<>();
        Map<String,Touchpoint> touchMap=new HashMap<>();
        int[] sequence=new int[contactCount];
        for(int i=0;i<touchpointCount;i++){
            int contactIndex=random.nextInt(contactCount);
            Contact contact=contactRows.get(contactIndex);
            Campaign campaign=campaignRows.get(random.nextInt(campaignRows.size()));
            Channel channel=campaignChannels.get(campaign.getId());
            Instant eventAt=start.plusSeconds(random.nextInt(450*86400));
            String id=IdFactory.contactTouch(contact.getId(),++sequence[contactIndex]);
            String type=channel.getChannelType().equals("OFFLINE")?"CALL":
                    new String[]{"CLICK","IMPRESSION","EMAIL_OPEN"}[random.nextInt(3)];
            String source=channel.getChannelType().equals("OFFLINE")?"OFFLINE":"ONLINE";
            Touchpoint t=new Touchpoint(id,contact,campaign,creativeMap.get(campaign.getId()),channel,eventAt,type,source);
            touchRows.add(t);byContact.computeIfAbsent(contact.getId(),k->new ArrayList<>()).add(t);touchMap.put(id,t);
        }
        touchpoints.saveAll(touchRows);

        List<WebSession> sessionRows=new ArrayList<>();
        List<Lead> leadRows=new ArrayList<>(opportunityCount);
        List<Opportunity> oppRows=new ArrayList<>(opportunityCount);
        List<OrderRecord> orderRows=new ArrayList<>(opportunityCount);
        int linkedContactCount=Math.min(contactCount,opportunityCount);
        for(int i=0;i<linkedContactCount;i++){
            Contact contact=contactRows.get(i);
            Instant convertedAt=start.plusSeconds(500*86400L+random.nextInt(90*86400));
            String sid=String.format("SES-2026-%06d",i+1);
            WebSession session=new WebSession(sid,contact,null,convertedAt.minusSeconds(86400L*random.nextInt(45)), "/solutions/"+segments[i%segments.length].toLowerCase(Locale.ROOT),true);
            sessionRows.add(session);
            Lead lead=new Lead(String.format("LEAD-2026-%06d",i+1),contact,session,convertedAt.minusSeconds(86400L*random.nextInt(60)+3600),"CONVERTED");
            leadRows.add(lead);
            Instant closedAt=convertedAt.plusSeconds(86400L*random.nextInt(30));
            BigDecimal revenue=money(10000+random.nextInt(190000));
            Opportunity opp=new Opportunity(String.format("OPP-2026-%06d",i+1),lead,lead.getCreatedAt(),closedAt,"WON",revenue,"INR");
            oppRows.add(opp);
            orderRows.add(new OrderRecord(String.format("ORD-2026-%06d",i+1),opp,"REV-"+String.format("%06d",i+1),revenue,"INR",closedAt));
        }
        sessions.saveAll(sessionRows); leads.saveAll(leadRows); opportunities.saveAll(oppRows); orders.saveAll(orderRows);

        List<CostRecord> costRows=new ArrayList<>();
        for(int i=0;i<campaignRows.size();i++){
            // Campaign 5 intentionally has omitted cost for a labelled edge case.
            if(i==4) continue;
            Campaign c=campaignRows.get(i);
            BigDecimal amount=c.getBudget().multiply(new BigDecimal("0.72")).setScale(2,RoundingMode.HALF_UP);
            String currency=i==7?"USD":"INR"; // labelled spend currency mismatch
            costRows.add(new CostRecord(String.format("COST-%06d",i+1),c,c.getStartDate().plusDays(1),amount,currency,"MEDIA_SPEND"));
        }
        costs.saveAll(costRows);

        return Map.of("seed",request.seed(),"campaigns",campaignCount,"contacts",contactCount,
                "touchpoints",touchpointCount,"opportunities",opportunityCount,"orders",orderRows.size(),
                "costRecords",costRows.size(),"generatedAt",Instant.now().toString());
    }
    private static BigDecimal money(int value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
}
