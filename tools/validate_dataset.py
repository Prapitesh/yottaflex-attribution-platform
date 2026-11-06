#!/usr/bin/env python3
"""Validate keys, relationships, domain rules, split labels and attribution weights."""
import argparse,csv,json,hashlib
from pathlib import Path
def read(d,n):
    with (d/n).open(encoding="utf-8",newline="") as f:return list(csv.DictReader(f))
def main():
    ap=argparse.ArgumentParser();ap.add_argument("--data",type=Path,default=Path(__file__).resolve().parents[1]/"data"/"generated");a=ap.parse_args();d=a.data;errors=[];tables={p.stem:read(d,p.name) for p in d.glob("*.csv")};counts={}
    for name,rs in tables.items():
        counts[name]=len(rs);keys=[r["id"] for r in rs if "id" in r]
        if len(keys)!=len(set(keys)):errors.append(f"{name}: duplicate primary IDs")
    def fk(child,key,parent):
        valid={r["id"] for r in tables[parent]};bad=[r.get("id","bridge-row") for r in tables[child] if r.get(key) and r[key] not in valid]
        if bad:errors.append(f"{child}.{key}: {len(bad)} invalid foreign keys")
    relations=[("campaigns","channel_id","channels"),("creatives","campaign_id","campaigns"),("campaign_audiences","campaign_id","campaigns"),("campaign_audiences","audience_id","audiences"),("touchpoints","contact_id","contacts"),("touchpoints","campaign_id","campaigns"),("touchpoints","creative_id","creatives"),("touchpoints","channel_id","channels"),("web_sessions","contact_id","contacts"),("leads","contact_id","contacts"),("leads","session_id","web_sessions"),("opportunities","lead_id","leads"),("orders","opportunity_id","opportunities"),("cost_records","campaign_id","campaigns"),("attribution_results","opportunity_id","opportunities"),("attribution_results","touchpoint_id","touchpoints")]
    for c,k,p in relations:fk(c,k,p)
    if any(r["channel_type"] not in {"PAID","ORGANIC","OFFLINE","REFERRAL","EMAIL","SOCIAL"} for r in tables["channels"]):errors.append("invalid channel enum")
    if any(r["end_date"]<r["start_date"] for r in tables["campaigns"]):errors.append("campaign date ordering")
    if any(r["closed_at"]<r["created_at"] for r in tables["opportunities"]):errors.append("opportunity date ordering")
    revenue=[r["external_revenue_key"] for r in tables["orders"]];duplicates=len(revenue)-len(set(revenue))
    if duplicates<1:errors.append("duplicate revenue anomaly missing")
    labels={r["issue_type"] for r in tables["ground_truth"]};required={"DUPLICATE_REVENUE","POST_CONVERSION_TOUCH","CURRENCY_MISMATCH","MISSING_CAMPAIGN_COST","UNVERIFIED_IDENTITY_LINK"}
    if required-labels:errors.append("missing anomaly labels: "+", ".join(sorted(required-labels)))
    grouped={}
    for row in tables.get("attribution_results",[]):
        key=(row["opportunity_id"],row["model_name"]);grouped[key]=grouped.get(key,0.0)+float(row["credit"])
    invalid=[k for k,v in grouped.items() if abs(v-1.0)>0.000001]
    if invalid:errors.append(f"attribution credits fail to sum to 1 in {len(invalid)} groups")
    # Every attribution inherits its opportunity's chronological split, never ground truth.
    opp_splits={r["id"]:r["evaluation_split"] for r in tables["opportunities"]}
    for row in tables["attribution_results"]:
        if row["evaluation_split"]!=opp_splits.get(row["opportunity_id"]):errors.append("attribution split leakage")
    manifest=json.loads((d/"manifest.json").read_text(encoding="utf-8"))
    for name,info in manifest["files"].items():
        actual=hashlib.sha256((d/name).read_bytes()).hexdigest()
        if actual!=info["sha256"]:errors.append(f"{name}: checksum mismatch")
    print(json.dumps({"counts":counts,"duplicate_revenue_records":duplicates,"ground_truth_labels":len(tables["ground_truth"]),"attribution_groups":len(grouped),"errors":errors,"valid":not errors},indent=2))
    raise SystemExit(1 if errors else 0)
if __name__=="__main__":main()
