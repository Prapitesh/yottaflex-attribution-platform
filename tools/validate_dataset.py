#!/usr/bin/env python3
"""Validate primary keys, foreign keys, enums, dates and expected anomaly labels."""
import argparse,csv,json,hashlib
from pathlib import Path
def read(d,n):
    with (d/n).open(encoding="utf-8",newline="") as f:return list(csv.DictReader(f))
def main():
    p=argparse.ArgumentParser();p.add_argument("--data",type=Path,default=Path(__file__).resolve().parents[1]/"data"/"generated");a=p.parse_args();d=a.data;errors=[];tables={x.stem:read(d,x.name) for x in d.glob("*.csv")};counts={}
    for name,rs in tables.items():
        counts[name]=len(rs);ids=[r["id"] for r in rs]
        if len(ids)!=len(set(ids)):errors.append(f"{name}: duplicate primary IDs")
    def fk(child,key,parent):
        valid={r["id"] for r in tables[parent]};bad=[r["id"] for r in tables[child] if r.get(key) and r[key] not in valid]
        if bad:errors.append(f"{child}.{key}: {len(bad)} invalid foreign keys")
    for c,k,pn in [("campaigns","channel_id","channels"),("creatives","campaign_id","campaigns"),("touchpoints","contact_id","contacts"),("touchpoints","campaign_id","campaigns"),("touchpoints","channel_id","channels"),("leads","contact_id","contacts"),("leads","session_id","web_sessions"),("opportunities","lead_id","leads"),("orders","opportunity_id","opportunities"),("cost_records","campaign_id","campaigns")]:fk(c,k,pn)
    if any(r["channel_type"] not in {"PAID","ORGANIC","OFFLINE","REFERRAL","EMAIL","SOCIAL"} for r in tables["channels"]):errors.append("invalid channel enum")
    if any(r["end_date"]<r["start_date"] for r in tables["campaigns"]):errors.append("campaign date ordering")
    if any(r["closed_at"]<r["created_at"] for r in tables["opportunities"]):errors.append("opportunity date ordering")
    revenue=[r["external_revenue_key"] for r in tables["orders"]];duplicates=len(revenue)-len(set(revenue))
    if duplicates<1:errors.append("duplicate revenue anomaly not generated")
    labels={r["issue_type"] for r in read(d,"ground_truth.csv")};required={"DUPLICATE_REVENUE","POST_CONVERSION_TOUCH","CURRENCY_MISMATCH","MISSING_CAMPAIGN_COST","UNVERIFIED_IDENTITY_LINK"}
    if required-labels:errors.append("missing anomaly labels: "+", ".join(sorted(required-labels)))
    manifest=json.loads((d/"manifest.json").read_text(encoding="utf-8"))
    for n,info in manifest["files"].items():
        h=hashlib.sha256((d/n).read_bytes()).hexdigest()
        if h!=info["sha256"]:errors.append(f"{n}: checksum mismatch")
    print(json.dumps({"counts":counts,"duplicate_revenue_records":duplicates,"ground_truth_labels":len(read(d,"ground_truth.csv")),"errors":errors,"valid":not errors},indent=2))
    raise SystemExit(1 if errors else 0)
if __name__=="__main__":main()
