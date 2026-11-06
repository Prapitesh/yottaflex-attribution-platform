#!/usr/bin/env python3
"""Deterministic synthetic data generator and reference attribution outputs."""
from __future__ import annotations
import argparse,csv,hashlib,json,random
from collections import defaultdict
from datetime import datetime,timedelta,timezone
from pathlib import Path
SEED_DEFAULT=20261022
ROOT=Path(__file__).resolve().parents[1]
OUT_DEFAULT=ROOT/"data"/"generated"
CHANNELS=[("CH-001","Paid Search","PAID"),("CH-002","Paid Social","SOCIAL"),("CH-003","Email","EMAIL"),("CH-004","Organic Search","ORGANIC"),("CH-005","Partner Referral","REFERRAL"),("CH-006","Field Event","OFFLINE")]
SEGMENTS=["SMB","MID_MARKET","ENTERPRISE","STARTUP"]
def iso(dt):return dt.astimezone(timezone.utc).isoformat().replace("+00:00","Z")
def money(n):return f"{n:.2f}"
def write_csv(path,rows,fields):
    with path.open("w",newline="",encoding="utf-8") as f:
        w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
def digest(path):
    h=hashlib.sha256()
    with path.open("rb") as f:
        for b in iter(lambda:f.read(1024*1024),b""):h.update(b)
    return h.hexdigest()
def generate(seed,cn,con,tn,on,out):
    rng=random.Random(seed);out.mkdir(parents=True,exist_ok=True);base=datetime(2025,1,1,tzinfo=timezone.utc)
    channels=[dict(id=i,name=n,channel_type=t) for i,n,t in CHANNELS]
    audiences=[dict(id=f"AUD-{i+1:03d}",name=s,segment_rule=f"Synthetic segment: {s}") for i,s in enumerate(SEGMENTS)]
    campaigns=[];creatives=[];bridges=[];costs=[];contacts=[];touches=[];sessions=[];leads=[];opps=[];orders=[];truth=[]
    for i in range(cn):
        ch=CHANNELS[i%len(CHANNELS)];start=base+timedelta(days=rng.randrange(300));cid=f"CMP-2026-{i+1:06d}";budget=rng.randrange(5000,100001)
        campaigns.append(dict(id=cid,name=f"Campaign {i+1} - {ch[1]}",channel_id=ch[0],budget=money(budget),currency="INR",start_date=start.date().isoformat(),end_date=(start+timedelta(days=365+rng.randrange(100))).date().isoformat(),status="COMPLETED"))
        creatives.append(dict(id=f"CRE-{i+1:06d}",name=f"Creative {i+1}",format=["SEARCH_AD","IMAGE","VIDEO","EMAIL"][i%4],campaign_id=cid))
        bridges.append(dict(campaign_id=cid,audience_id=audiences[i%len(audiences)]["id"],effective_from=start.date().isoformat(),effective_to=""))
        if i!=4:
            currency="USD" if i==7 else "INR"
            costs.append(dict(id=f"COST-{i+1:06d}",campaign_id=cid,incurred_on=(start+timedelta(days=1)).date().isoformat(),amount=money(budget*.72),currency=currency,cost_type="MEDIA_SPEND"))
            if i==7:truth.append(dict(id=f"GT-CURRENCY-{cid}",issue_type="CURRENCY_MISMATCH",entity_type="COST_RECORD",entity_id=f"COST-{i+1:06d}",expected_decision="QUARANTINE_UNTIL_FX",evaluation_split="TEST"))
        else:truth.append(dict(id=f"GT-MISSING-COST-CMP-2026-{i+1:06d}",issue_type="MISSING_CAMPAIGN_COST",entity_type="CAMPAIGN",entity_id=cid,expected_decision="MARK_ROI_UNAVAILABLE",evaluation_split="TEST"))
    channel_by={c["id"]:c for c in channels};creative_by={c["campaign_id"]:c for c in creatives}
    for i in range(con):contacts.append(dict(id=f"CON-2026-{i+1:06d}",segment=rng.choice(SEGMENTS),created_at=iso(base+timedelta(days=rng.randrange(365)))))
    touchseq=defaultdict(int)
    for _ in range(tn):
        contact=contacts[rng.randrange(con)];campaign=campaigns[rng.randrange(cn)];touchseq[contact["id"]]+=1
        at=base+timedelta(days=rng.randrange(450),seconds=rng.randrange(86400));channel=channel_by[campaign["channel_id"]];offline=channel["channel_type"]=="OFFLINE"
        touches.append(dict(id=f"TOUCH-{contact['id']}-{touchseq[contact['id']]:06d}",contact_id=contact["id"],campaign_id=campaign["id"],creative_id=creative_by[campaign["id"]]["id"],channel_id=channel["id"],event_at=iso(at),touch_type="CALL" if offline else rng.choice(["CLICK","IMPRESSION","EMAIL_OPEN"]),source_type="OFFLINE" if offline else "ONLINE"))
    touch_by_contact=defaultdict(list)
    for t in touches:touch_by_contact[t["contact_id"]].append(t)
    linked=min(con,on)
    for i in range(linked):
        contact=contacts[i];converted=base+timedelta(days=500+rng.randrange(90));sid=f"SES-2026-{i+1:06d}"
        sessions.append(dict(id=sid,contact_id=contact["id"],anonymous_id="",started_at=iso(converted-timedelta(days=rng.randrange(45))),landing_path="/solutions/"+contact["segment"].lower(),identity_link_verified="true"))
        lead_at=converted-timedelta(days=rng.randrange(60),hours=1);leadid=f"LEAD-2026-{i+1:06d}"
        leads.append(dict(id=leadid,contact_id=contact["id"],session_id=sid,created_at=iso(lead_at),status="CONVERTED"))
        closed=converted+timedelta(days=rng.randrange(30));amount=rng.randrange(10000,200001);oppid=f"OPP-2026-{i+1:06d}"
        opps.append(dict(id=oppid,lead_id=leadid,created_at=iso(lead_at),closed_at=iso(closed),stage="WON",expected_revenue=money(amount),currency="INR"))
        orders.append(dict(id=f"ORD-2026-{i+1:06d}",opportunity_id=oppid,external_revenue_key=f"REV-{i+1:06d}",revenue=money(amount),currency="INR",recognized_at=iso(closed)))
    if touches and opps:
        late=dict(touches[-1]);late.update(id="TOUCH-EDGE-POST-CONVERSION",contact_id=contacts[0]["id"],event_at=iso(datetime.fromisoformat(opps[0]["closed_at"].replace("Z","+00:00"))+timedelta(days=1)))
        touches.append(late);touch_by_contact[late["contact_id"]].append(late)
        truth.append(dict(id="GT-POST-CONVERSION-001",issue_type="POST_CONVERSION_TOUCH",entity_type="TOUCHPOINT",entity_id=late["id"],expected_decision="EXCLUDE_POST_CONVERSION",evaluation_split="TEST"))
    if orders:
        duplicate=dict(orders[0]);duplicate["id"]="ORD-EDGE-DUPLICATE";orders.append(duplicate)
        truth.append(dict(id="GT-DUPLICATE-REVENUE-001",issue_type="DUPLICATE_REVENUE",entity_type="ORDER",entity_id=duplicate["id"],expected_decision="COUNT_UNIQUE_REVENUE_KEY_ONCE",evaluation_split="TEST"))
    if contacts:
        sessions.append(dict(id="SES-EDGE-ANON-001",contact_id="",anonymous_id="anon-no-evidence",started_at=iso(base+timedelta(days=200)),landing_path="/pricing",identity_link_verified="false"))
        truth.append(dict(id="GT-ANON-LINK-001",issue_type="UNVERIFIED_IDENTITY_LINK",entity_type="WEB_SESSION",entity_id="SES-EDGE-ANON-001",expected_decision="REJECT_IDENTITY_LINK",evaluation_split="TEST"))
    attribution=[]
    for opp in opps:
        lead=next(x for x in leads if x["id"]==opp["lead_id"]);close=datetime.fromisoformat(opp["closed_at"].replace("Z","+00:00"));start=close-timedelta(days=180)
        eligible=sorted((t for t in touch_by_contact[lead["contact_id"]] if start<=datetime.fromisoformat(t["event_at"].replace("Z","+00:00"))<=close),key=lambda x:x["event_at"])
        if not eligible:continue
        for model,selected in [("FIRST_TOUCH",[eligible[0]]),("LAST_TOUCH",[eligible[-1]]),("LINEAR",eligible)]:
            each=1/len(selected);assigned=0.0
            for j,t in enumerate(selected):
                credit=round(1-assigned,6) if j==len(selected)-1 else round(each,6);assigned+=credit
                attribution.append(dict(id=f"ATR-{model}-{opp['id']}-{j+1:04d}",opportunity_id=opp["id"],touchpoint_id=t["id"],model_name=model,credit=f"{credit:.6f}",attributed_revenue=money(float(opp["expected_revenue"])*credit),calculated_at="2026-11-06T00:00:00Z"))
    split={"train_end":"2025-09-30","validation_end":"2025-11-30","test_start":"2025-12-01","basis":"Chronological opportunity close date. Attribution groups inherit opportunity split; ground_truth is evaluation-only."}
    def assign_split(iso_value):
        d=iso_value[:10]
        return "TRAIN" if d<="2025-09-30" else ("VALIDATION" if d<="2025-11-30" else "TEST")
    # Each source/derived row is assigned by its own event date or its conversion's date.
    for row in touches:row["evaluation_split"]=assign_split(row["event_at"])
    for row in leads:row["evaluation_split"]=assign_split(row["created_at"])
    for row in opps:row["evaluation_split"]=assign_split(row["closed_at"])
    for row in orders:row["evaluation_split"]=assign_split(row["recognized_at"])
    opp_split={o["id"]:o["evaluation_split"] for o in opps}
    for row in attribution:row["evaluation_split"]=opp_split[row["opportunity_id"]]
    tables={
    "channels.csv":(channels,["id","name","channel_type"]),
    "audiences.csv":(audiences,["id","name","segment_rule"]),
    "campaigns.csv":(campaigns,["id","name","channel_id","budget","currency","start_date","end_date","status"]),
    "campaign_audiences.csv":(bridges,["campaign_id","audience_id","effective_from","effective_to"]),
    "creatives.csv":(creatives,["id","name","format","campaign_id"]),
    "contacts.csv":(contacts,["id","segment","created_at"]),
    "touchpoints.csv":(touches,["id","contact_id","campaign_id","creative_id","channel_id","event_at","touch_type","source_type","evaluation_split"]),
    "web_sessions.csv":(sessions,["id","contact_id","anonymous_id","started_at","landing_path","identity_link_verified"]),
    "leads.csv":(leads,["id","contact_id","session_id","created_at","status","evaluation_split"]),
    "opportunities.csv":(opps,["id","lead_id","created_at","closed_at","stage","expected_revenue","currency","evaluation_split"]),
    "orders.csv":(orders,["id","opportunity_id","external_revenue_key","revenue","currency","recognized_at","evaluation_split"]),
    "cost_records.csv":(costs,["id","campaign_id","incurred_on","amount","currency","cost_type"]),
    "attribution_results.csv":(attribution,["id","opportunity_id","touchpoint_id","model_name","credit","attributed_revenue","calculated_at","evaluation_split"]),
    "ground_truth.csv":(truth,["id","issue_type","entity_type","entity_id","expected_decision","evaluation_split"])}
    manifest={}
    for name,(data,fields) in tables.items():
        p=out/name;write_csv(p,data,fields);manifest[name]={"rows":len(data),"sha256":digest(p)}
    (out/"evaluation_split.json").write_text(json.dumps(split,indent=2),encoding="utf-8")
    result={"seed":seed,"counts":{k:v["rows"] for k,v in manifest.items()},"files":manifest,"evaluation_split":split}
    (out/"manifest.json").write_text(json.dumps(result,indent=2),encoding="utf-8")
    checksum_files=sorted(path for path in out.iterdir()
                          if path.is_file() and path.name!="SHA256SUMS.txt")
    checksum_lines=[f"{digest(path)}  {path.name}" for path in checksum_files]
    (out/"SHA256SUMS.txt").write_text("\n".join(checksum_lines)+"\n",encoding="utf-8")
    return result
def main():
    p=argparse.ArgumentParser();p.add_argument("--seed",type=int,default=SEED_DEFAULT);p.add_argument("--campaigns",type=int,default=12);p.add_argument("--contacts",type=int,default=300);p.add_argument("--touchpoints",type=int,default=1200);p.add_argument("--opportunities",type=int,default=80);p.add_argument("--full",action="store_true");p.add_argument("--out",type=Path,default=OUT_DEFAULT);a=p.parse_args()
    if a.full:a.campaigns,a.contacts,a.touchpoints,a.opportunities=500,50000,200000,10000
    print(json.dumps(generate(a.seed,a.campaigns,a.contacts,a.touchpoints,a.opportunities,a.out),indent=2))
if __name__=="__main__":main()
