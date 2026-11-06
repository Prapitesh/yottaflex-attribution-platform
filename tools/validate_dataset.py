#!/usr/bin/env python3
"""Validate synthetic attribution exports, anomaly evidence and temporal rules."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from collections import defaultdict
from datetime import datetime, timedelta
from pathlib import Path

REQUIRED_TABLES = {
    "channels", "audiences", "campaigns", "creatives", "campaign_audiences",
    "contacts", "touchpoints", "web_sessions", "leads", "opportunities",
    "orders", "cost_records", "attribution_results", "ground_truth",
}
REQUIRED_ANOMALIES = {
    "DUPLICATE_REVENUE", "POST_CONVERSION_TOUCH", "CURRENCY_MISMATCH",
    "MISSING_CAMPAIGN_COST", "UNVERIFIED_IDENTITY_LINK",
}


def read_table(directory: Path, name: str) -> list[dict[str, str]]:
    with (directory / f"{name}.csv").open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream))


def parse_time(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def validate(directory: Path) -> dict:
    errors: list[str] = []
    tables = {path.stem: read_table(directory, path.stem) for path in directory.glob("*.csv")}
    missing = REQUIRED_TABLES - tables.keys()
    if missing:
        errors.append("missing required tables: " + ", ".join(sorted(missing)))

    counts: dict[str, int] = {}
    for name, rows in tables.items():
        counts[f"{name}.csv"] = len(rows)
        ids = [row["id"] for row in rows if row.get("id")]
        if len(ids) != len(set(ids)):
            errors.append(f"{name}: duplicate primary IDs")

    def foreign_key(child: str, key: str, parent: str) -> None:
        if child not in tables or parent not in tables:
            return
        valid = {row["id"] for row in tables[parent]}
        invalid = [row.get("id", "unknown") for row in tables[child]
                   if row.get(key) and row[key] not in valid]
        if invalid:
            errors.append(f"{child}.{key}: {len(invalid)} invalid foreign keys")

    relations = [
        ("campaigns", "channel_id", "channels"),
        ("creatives", "campaign_id", "campaigns"),
        ("campaign_audiences", "campaign_id", "campaigns"),
        ("campaign_audiences", "audience_id", "audiences"),
        ("touchpoints", "contact_id", "contacts"),
        ("touchpoints", "campaign_id", "campaigns"),
        ("touchpoints", "creative_id", "creatives"),
        ("touchpoints", "channel_id", "channels"),
        ("web_sessions", "contact_id", "contacts"),
        ("leads", "contact_id", "contacts"),
        ("leads", "session_id", "web_sessions"),
        ("opportunities", "lead_id", "leads"),
        ("orders", "opportunity_id", "opportunities"),
        ("cost_records", "campaign_id", "campaigns"),
        ("attribution_results", "opportunity_id", "opportunities"),
        ("attribution_results", "touchpoint_id", "touchpoints"),
    ]
    for child, key, parent in relations:
        foreign_key(child, key, parent)

    if not REQUIRED_TABLES.issubset(tables):
        return {"counts": counts, "errors": errors, "valid": False}

    campaigns = {row["id"]: row for row in tables["campaigns"]}
    contacts = {row["id"]: row for row in tables["contacts"]}
    sessions = {row["id"]: row for row in tables["web_sessions"]}
    leads = {row["id"]: row for row in tables["leads"]}
    opportunities = {row["id"]: row for row in tables["opportunities"]}
    touchpoints = {row["id"]: row for row in tables["touchpoints"]}
    costs = tables["cost_records"]
    orders = tables["orders"]
    ground_truth = tables["ground_truth"]

    if any(row["channel_type"] not in {"PAID", "ORGANIC", "OFFLINE", "REFERRAL", "EMAIL", "SOCIAL"}
           for row in tables["channels"]):
        errors.append("invalid channel enum")
    if any(row["end_date"] < row["start_date"] for row in campaigns.values()):
        errors.append("campaign date ordering")
    if any(parse_time(row["closed_at"]) < parse_time(row["created_at"])
           for row in opportunities.values()):
        errors.append("opportunity date ordering")

    # Each labelled anomaly must exist in the source rows and demonstrate the expected failure mode.
    labels = {row["issue_type"] for row in ground_truth}
    if REQUIRED_ANOMALIES - labels:
        errors.append("missing anomaly labels: " + ", ".join(sorted(REQUIRED_ANOMALIES - labels)))
    table_by_type = {
        "CAMPAIGN": campaigns,
        "COST_RECORD": {row["id"]: row for row in costs},
        "TOUCHPOINT": touchpoints,
        "ORDER": {row["id"]: row for row in orders},
        "WEB_SESSION": sessions,
    }
    for label in ground_truth:
        entity = table_by_type.get(label["entity_type"], {}).get(label["entity_id"])
        if entity is None:
            errors.append(f"ground_truth {label['id']}: target entity does not exist")
            continue
        issue = label["issue_type"]
        if issue == "UNVERIFIED_IDENTITY_LINK":
            if entity.get("identity_link_verified", "").lower() != "false" or entity.get("contact_id"):
                errors.append("unverified identity link must remain unlinked to a contact")
        elif issue == "CURRENCY_MISMATCH":
            campaign = campaigns.get(entity.get("campaign_id"))
            if campaign and entity.get("currency") == campaign.get("currency"):
                errors.append("currency mismatch label does not point to a mismatched cost")
        elif issue == "MISSING_CAMPAIGN_COST":
            if any(cost["campaign_id"] == label["entity_id"] for cost in costs):
                errors.append("missing campaign cost label points to a campaign with a cost")
        elif issue == "DUPLICATE_REVENUE":
            key = entity.get("external_revenue_key")
            if not key or sum(row["external_revenue_key"] == key for row in orders) < 2:
                errors.append("duplicate revenue label does not point to a duplicated revenue key")
        elif issue == "POST_CONVERSION_TOUCH":
            contact_id = entity.get("contact_id")
            related_closes = [
                parse_time(opp["closed_at"]) for opp in opportunities.values()
                if leads.get(opp["lead_id"], {}).get("contact_id") == contact_id
            ]
            event_at = parse_time(entity["event_at"])
            if not related_closes or not any(event_at > closed for closed in related_closes):
                errors.append("post-conversion touch label does not occur after a linked conversion")

    # Attribution must be contact-linked, inside the 180-day pre-conversion window, and normalized.
    opportunity_contact = {
        opp_id: leads.get(opp["lead_id"], {}).get("contact_id")
        for opp_id, opp in opportunities.items()
    }
    credit_totals: dict[tuple[str, str], float] = defaultdict(float)
    for row in tables["attribution_results"]:
        opportunity = opportunities.get(row["opportunity_id"])
        touch = touchpoints.get(row["touchpoint_id"])
        if not opportunity or not touch:
            continue
        close = parse_time(opportunity["closed_at"])
        event_at = parse_time(touch["event_at"])
        if touch["contact_id"] != opportunity_contact[row["opportunity_id"]]:
            errors.append("attribution contact does not match opportunity lead contact")
        if not close - timedelta(days=180) <= event_at <= close:
            errors.append("attribution includes a touch outside the 180-day pre-conversion window")
        credit_totals[(row["opportunity_id"], row["model_name"])] += float(row["credit"])
        if row["evaluation_split"] != opportunity["evaluation_split"]:
            errors.append("attribution split leakage")
    invalid_groups = [key for key, total in credit_totals.items() if abs(total - 1.0) > 0.000001]
    if invalid_groups:
        errors.append(f"attribution credits fail to sum to 1 in {len(invalid_groups)} groups")

    # Manifest counts and checksums must describe the actual CSV exports.
    manifest_path = directory / "manifest.json"
    if not manifest_path.exists():
        errors.append("manifest.json is missing")
    else:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        for name, expected_count in manifest.get("counts", {}).items():
            if counts.get(name) != expected_count:
                errors.append(f"{name}: manifest row count does not match export")
        for name, info in manifest.get("files", {}).items():
            path = directory / name
            if not path.exists():
                errors.append(f"{name}: manifest file is missing")
                continue
            actual = hashlib.sha256(path.read_bytes()).hexdigest()
            if actual != info["sha256"]:
                errors.append(f"{name}: checksum mismatch")

    checksum_path = directory / "SHA256SUMS.txt"
    if not checksum_path.exists():
        errors.append("SHA256SUMS.txt is missing")
    else:
        seen_checksums: set[str] = set()
        for line_number, line in enumerate(checksum_path.read_text(encoding="utf-8").splitlines(), 1):
            parts = line.split(maxsplit=1)
            if len(parts) != 2:
                errors.append(f"SHA256SUMS.txt:{line_number}: malformed checksum entry")
                continue
            expected_hash, filename = parts
            filename = filename.lstrip("*")
            if filename in seen_checksums:
                errors.append(f"SHA256SUMS.txt: duplicate entry for {filename}")
                continue
            seen_checksums.add(filename)
            target = directory / filename
            if not target.is_file():
                errors.append(f"SHA256SUMS.txt: listed file is missing: {filename}")
            elif hashlib.sha256(target.read_bytes()).hexdigest() != expected_hash:
                errors.append(f"SHA256SUMS.txt: checksum mismatch for {filename}")
        if not seen_checksums:
            errors.append("SHA256SUMS.txt contains no entries")

    duplicate_count = len(orders) - len({row["external_revenue_key"] for row in orders})
    return {
        "counts": counts,
        "duplicate_revenue_records": duplicate_count,
        "ground_truth_labels": len(ground_truth),
        "attribution_groups": len(credit_totals),
        "errors": errors,
        "valid": not errors,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path(__file__).resolve().parents[1] / "data" / "generated")
    args = parser.parse_args()
    report = validate(args.data)
    print(json.dumps(report, indent=2))
    raise SystemExit(0 if report["valid"] else 1)


if __name__ == "__main__":
    main()
