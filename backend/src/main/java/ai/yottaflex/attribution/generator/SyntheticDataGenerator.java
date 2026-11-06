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
    private final ChannelRepository channels; private final CampaignRepository campaigns;
    private final CreativeRepository creatives; private final AudienceRepository audiences;
    private final CampaignAudienceRepository bridges; private final ContactRepository contacts;
    private final TouchpointRepository touchpoints; private final WebSessionRepository sessions;
    private final LeadRepository leads; private final OpportunityRepository opportunities;
    private final OrderRecordRepository orders; private final CostRecordRepository costs;
    private final GenerationEventRecorder events;
    public SyntheticDataGenerator(ChannelRepository channels,CampaignRepository campaigns,CreativeRepository creatives,
        AudienceRepository audiences,CampaignAudienceRepository bridges,ContactRepository contacts,TouchpointRepository touchpoints,
        WebSessionRepository sessions,LeadRepository leads,OpportunityRepository opportunities,OrderRecordRepository orders,
        CostRecordRepository costs,GenerationEventRecorder events){
        this.channels=channels;this.campaigns=campaigns;this.creatives=creatives;this.audiences=audiences;this.bridges=bridges;
        this.contacts=contacts;this.touchpoints=touchpoints;this.sessions=sessions;this.leads=leads;
        this.opportunities=opportunities;this.orders=orders;this.costs=costs;this.events=events;
    }

    @Transactional
    public Map<String,Object> generate(GenerationRequest request){
        orders.deleteAllInBatch();opportunities.deleteAllInBatch();leads.deleteAllInBatch();sessions.deleteAllInBatch();
        touchpoints.deleteAllInBatch();costs.deleteAllInBatch();bridges.deleteAllInBatch();creatives.deleteAllInBatch();
        campaigns.deleteAllInBatch();contacts.deleteAllInBatch();audiences.deleteAllInBatch();channels.deleteAllInBatch();
        Random random=new Random(request.seed());Instant base=Instant.parse("2025-01-01T00:00:00Z");
        String[] segments={"SMB","MID_MARKET","ENTERPRISE","STARTUP"};
        String[][] defs={{"CH-001","Paid Search","PAID"},{"CH-002","Paid Social","SOCIAL"},{"CH-003","Email","EMAIL"},{"CH-004","Organic Search","ORGANIC"},{"CH-005","Partner Referral","REFERRAL"},{"CH-006","Field Event","OFFLINE"}};
        List<Channel> channelRows=new ArrayList<>();for(String[] d:defs)channelRows.add(new Channel(d[0],d[1],d[2]));channels.saveAll(channelRows);
        List<Audience> audienceRows=new ArrayList<>();for(int i=0;i<segments.length;i++)audienceRows.add(new Audience(String.format("AUD-%03d",i+1),segments[i],"Synthetic segment: "+segments[i]));audiences.saveAll(audienceRows);

        int nc=request.campaigns(),ncontact=request.contacts(),nt=request.touchpoints(),no=request.opportunities();
        List<Campaign> campaignRows=new ArrayList<>();Map<String,Channel> channelMap=new HashMap<>();
        IdFactory ids=new IdFactory("CMP-2026-",6);
        for(int i=0;i<nc;i++){Channel ch=channelRows.get(i%channelRows.size());LocalDate from=LocalDate.of(2025,1,1).plusDays(random.nextInt(300));
            Campaign c=new Campaign(ids.next(),"Campaign "+(i+1)+" - "+ch.getName(),ch,money(5000+random.nextInt(95000)),"INR",from,from.plusDays(365+random.nextInt(100)),"COMPLETED");campaignRows.add(c);channelMap.put(c.getId(),ch);}
        campaigns.saveAll(campaignRows);
        List<CampaignAudience> bridgeRows=new ArrayList<>();for(int i=0;i<nc;i++)bridgeRows.add(new CampaignAudience(campaignRows.get(i),audienceRows.get(i%audienceRows.size()),campaignRows.get(i).getStartDate(),null));bridges.saveAll(bridgeRows);
        List<Creative> creativeRows=new ArrayList<>();Map<String,Creative> creativeMap=new HashMap<>();
        for(int i=0;i<nc;i++){Campaign c=campaignRows.get(i);Creative cr=new Creative("CRE-"+String.format("%06d",i+1),"Creative "+(i+1),new String[]{"SEARCH_AD","IMAGE","VIDEO","EMAIL"}[i%4],c);creativeRows.add(cr);creativeMap.put(c.getId(),cr);}creatives.saveAll(creativeRows);
        List<Contact> contactRows=new ArrayList<>(ncontact);
        for(int i=0;i<ncontact;i++)contactRows.add(new Contact(String.format("CON-2026-%06d",i+1),segments[random.nextInt(segments.length)],base.plusSeconds(random.nextInt(365*86400))));contacts.saveAll(contactRows);

        List<Touchpoint> touchRows=new ArrayList<>(nt+1);int[] sequence=new int[ncontact];
        for(int i=0;i<nt;i++){int ci=random.nextInt(ncontact);Contact contact=contactRows.get(ci);Campaign campaign=campaignRows.get(random.nextInt(nc));Channel ch=channelMap.get(campaign.getId());boolean offline="OFFLINE".equals(ch.getChannelType());Instant at=base.plusSeconds(random.nextInt(450*86400));
            touchRows.add(new Touchpoint(IdFactory.contactTouch(contact.getId(),++sequence[ci]),contact,campaign,creativeMap.get(campaign.getId()),ch,at,offline?"CALL":new String[]{"CLICK","IMPRESSION","EMAIL_OPEN"}[random.nextInt(3)],offline?"OFFLINE":"ONLINE"));}
        touchpoints.saveAll(touchRows);

        List<WebSession> sessionRows=new ArrayList<>();List<Lead> leadRows=new ArrayList<>();List<Opportunity> oppRows=new ArrayList<>();List<OrderRecord> orderRows=new ArrayList<>();
        int linked=Math.min(ncontact,no);
        for(int i=0;i<linked;i++){Contact contact=contactRows.get(i);Instant conversion=base.plusSeconds(500*86400L+random.nextInt(90*86400));WebSession session=new WebSession(String.format("SES-2026-%06d",i+1),contact,null,conversion.minusSeconds(86400L*random.nextInt(45)),"/solutions/"+segments[i%segments.length].toLowerCase(Locale.ROOT),true);sessionRows.add(session);
            Instant leadAt=conversion.minusSeconds(86400L*random.nextInt(60)+3600);Lead lead=new Lead(String.format("LEAD-2026-%06d",i+1),contact,session,leadAt,"CONVERTED");leadRows.add(lead);
            Instant close=conversion.plusSeconds(86400L*random.nextInt(30));BigDecimal revenue=money(10000+random.nextInt(190000));Opportunity opp=new Opportunity(String.format("OPP-2026-%06d",i+1),lead,leadAt,close,"WON",revenue,"INR");oppRows.add(opp);
            orderRows.add(new OrderRecord(String.format("ORD-2026-%06d",i+1),opp,"REV-"+String.format("%06d",i+1),revenue,"INR",close));}
        if(linked>0&&nc>0){
            // Explicit, linked exceptions. Each keeps valid foreign keys and a documented expected decision.
            Opportunity first=oppRows.getFirst();Contact contact=first.getLead().getContact();Campaign campaign=campaignRows.getFirst();Channel ch=channelMap.get(campaign.getId());
            touchRows.add(new Touchpoint("TOUCH-EDGE-POST-CONVERSION",contact,campaign,creativeMap.get(campaign.getId()),ch,first.getClosedAt().plusSeconds(86400),"CLICK","ONLINE"));
            touchpoints.saveAll(touchRows.subList(nt,touchRows.size()));
            OrderRecord original=orderRows.getFirst();
            orderRows.add(new OrderRecord("ORD-EDGE-DUPLICATE",originalOpportunity(original,oppRows),original.getExternalRevenueKey(),original.getRevenue(),"INR",original.getRecognizedAt()));
            orders.save(orderRows.getLast());
            sessionRows.add(new WebSession("SES-EDGE-ANON-001",contact,"anon-no-evidence",base.plusSeconds(200L*86400),"/pricing",false));
        }
        sessions.saveAll(sessionRows);leads.saveAll(leadRows);opportunities.saveAll(oppRows);orders.saveAll(orderRows);
        List<CostRecord> costRows=new ArrayList<>();for(int i=0;i<nc;i++){if(i==4)continue;Campaign c=campaignRows.get(i);BigDecimal spend=c.getBudget().multiply(new BigDecimal("0.72")).setScale(2,RoundingMode.HALF_UP);costRows.add(new CostRecord(String.format("COST-%06d",i+1),c,c.getStartDate().plusDays(1),spend,i==7?"USD":"INR","MEDIA_SPEND"));}costs.saveAll(costRows);
        events.recordCreated("CAMPAIGN",campaignRows.stream().map(Campaign::getId).toList());events.recordCreated("CONTACT",contactRows.stream().map(Contact::getId).toList());
        events.recordCreated("TOUCHPOINT",touchRows.stream().map(Touchpoint::getId).toList());events.recordCreated("OPPORTUNITY",oppRows.stream().map(Opportunity::getId).toList());
        return Map.of("seed",request.seed(),"campaigns",nc,"contacts",ncontact,"touchpoints",touchRows.size(),"opportunities",no,"orders",orderRows.size(),"costRecords",costRows.size(),"labelledEdgeCases",linked>0?3:2);
    }
    private Opportunity originalOpportunity(OrderRecord record,List<Opportunity> rows){return rows.stream().filter(o->o.getId().equals(record.getId().replace("ORD-","OPP-"))).findFirst().orElse(rows.getFirst());}
    private static BigDecimal money(int value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
}
