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
        orders.deleteAllInBatch(); opportunities.deleteAllInBatch(); leads.deleteAllInBatch();
        sessions.deleteAllInBatch(); touchpoints.deleteAllInBatch(); costs.deleteAllInBatch();
        creatives.deleteAllInBatch(); campaigns.deleteAllInBatch(); contacts.deleteAllInBatch();
        audiences.deleteAllInBatch(); channels.deleteAllInBatch();

        Random random = new Random(request.seed());
        Instant base = Instant.parse("2025-01-01T00:00:00Z");
        List<Channel> channelRows = new ArrayList<>();
        String[][] defs={{"CH-001","Paid Search","PAID"},{"CH-002","Paid Social","SOCIAL"},
                {"CH-003","Email","EMAIL"},{"CH-004","Organic Search","ORGANIC"},
                {"CH-005","Partner Referral","REFERRAL"},{"CH-006","Field Event","OFFLINE"}};
        for(String[] d:defs) channelRows.add(new Channel(d[0],d[1],d[2]));
        channels.saveAll(channelRows);

        List<Audience> audienceRows=new ArrayList<>();
        String[] segments={"SMB","MID_MARKET","ENTERPRISE","STARTUP"};
        for(int i=0;i<segments.length;i++) audienceRows.add(new Audience(String.format("AUD-%03d",i+1),segments[i],"Synthetic segment: "+segments[i]));
        audiences.saveAll(audienceRows);

        int campaignCount=request.campaigns(),contactCount=request.contacts(),touchpointCount=request.touchpoints(),opportunityCount=request.opportunities();
        IdFactory campaignIds=new IdFactory("CMP-2026-",6);
        List<Campaign> campaignRows=new ArrayList<>(campaignCount);
        Map<String,Channel> campaignChannels=new HashMap<>();
        for(int i=0;i<campaignCount;i++){
            Channel ch=channelRows.get(i%channelRows.size());
            LocalDate start=LocalDate.of(2025,1,1).plusDays(random.nextInt(300));
            Campaign c=new Campaign(campaignIds.next(),"Campaign "+(i+1)+" - "+ch.getName(),ch,
                    money(5000+random.nextInt(95000)),"INR",start,start.plusDays(365+random.nextInt(100)),"COMPLETED");
            campaignRows.add(c);campaignChannels.put(c.getId(),ch);
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
        for(int i=0;i<contactCount;i++){
            contactRows.add(new Contact(String.format("CON-2026-%06d",i+1),segments[random.nextInt(segments.length)],
                    base.plusSeconds(random.nextInt(365*86400))));
        }
        contacts.saveAll(contactRows);

        List<Touchpoint> touchRows=new ArrayList<>(touchpointCount);
        int[] sequence=new int[contactCount];
        for(int i=0;i<touchpointCount;i++){
            int ci=random.nextInt(contactCount);Contact contact=contactRows.get(ci);
            Campaign campaign=campaignRows.get(random.nextInt(campaignRows.size()));
            Channel channel=campaignChannels.get(campaign.getId());
            Instant at=base.plusSeconds(random.nextInt(450*86400));
            boolean offline=channel.getChannelType().equals("OFFLINE");
            touchRows.add(new Touchpoint(IdFactory.contactTouch(contact.getId(),++sequence[ci]),contact,campaign,
                    creativeMap.get(campaign.getId()),channel,at,
                    offline?"CALL":new String[]{"CLICK","IMPRESSION","EMAIL_OPEN"}[random.nextInt(3)],
                    offline?"OFFLINE":"ONLINE"));
        }
        touchpoints.saveAll(touchRows);

        List<WebSession> sessionRows=new ArrayList<>();
        List<Lead> leadRows=new ArrayList<>(opportunityCount);
        List<Opportunity> oppRows=new ArrayList<>(opportunityCount);
        List<OrderRecord> orderRows=new ArrayList<>(opportunityCount);
        int linked=Math.min(contactCount,opportunityCount);
        for(int i=0;i<linked;i++){
            Contact contact=contactRows.get(i);
            Instant close=base.plusSeconds(500*86400L+random.nextInt(90*86400));
            WebSession session=new WebSession(String.format("SES-2026-%06d",i+1),contact,null,
                    close.minusSeconds(86400L*random.nextInt(45)),"/solutions/"+segments[i%segments.length].toLowerCase(Locale.ROOT),true);
            sessionRows.add(session);
            Instant leadAt=close.minusSeconds(86400L*random.nextInt(60)+3600);
            Lead lead=new Lead(String.format("LEAD-2026-%06d",i+1),contact,session,leadAt,"CONVERTED");
            leadRows.add(lead);
            Instant closedAt=close.plusSeconds(86400L*random.nextInt(30));
            BigDecimal revenue=money(10000+random.nextInt(190000));
            Opportunity opp=new Opportunity(String.format("OPP-2026-%06d",i+1),lead,leadAt,closedAt,"WON",revenue,"INR");
            oppRows.add(opp);
            orderRows.add(new OrderRecord(String.format("ORD-2026-%06d",i+1),opp,
                    "REV-"+String.format("%06d",i+1),revenue,"INR",closedAt));
        }
        sessions.saveAll(sessionRows);leads.saveAll(leadRows);opportunities.saveAll(oppRows);orders.saveAll(orderRows);

        List<CostRecord> costRows=new ArrayList<>();
        for(int i=0;i<campaignRows.size();i++){
            if(i==4) continue; // deliberate missing-cost case
            Campaign c=campaignRows.get(i);
            BigDecimal spend=c.getBudget().multiply(new BigDecimal("0.72")).setScale(2,RoundingMode.HALF_UP);
            costRows.add(new CostRecord(String.format("COST-%06d",i+1),c,c.getStartDate().plusDays(1),spend,i==7?"USD":"INR","MEDIA_SPEND"));
        }
        costs.saveAll(costRows);

        return Map.of("seed",request.seed(),"campaigns",campaignCount,"contacts",contactCount,
                "touchpoints",touchpointCount,"opportunities",opportunityCount,"orders",orderRows.size(),
                "costRecords",costRows.size(),"generatedAt",Instant.now().toString());
    }
    private static BigDecimal money(int value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
}
