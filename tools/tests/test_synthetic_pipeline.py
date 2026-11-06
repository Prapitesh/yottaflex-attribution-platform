import csv
import tempfile
import unittest
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from generate_synthetic import generate
from validate_dataset import validate

class SyntheticPipelineTest(unittest.TestCase):
    def test_starter_relationships_anomaly_labels_and_credit(self):
        with tempfile.TemporaryDirectory() as temp:
            out=Path(temp)/"dataset"
            manifest=generate(20261022,12,300,1200,80,out)
            def rows(name):
                with (out/name).open(encoding="utf-8",newline="") as stream:
                    return list(csv.DictReader(stream))
            self.assertEqual(manifest["counts"]["campaigns.csv"],12)
            self.assertEqual(manifest["counts"]["contacts.csv"],300)
            self.assertEqual(manifest["counts"]["touchpoints.csv"],1201)
            campaign_ids={row["id"] for row in rows("campaigns.csv")}
            self.assertTrue(all(row["campaign_id"] in campaign_ids for row in rows("touchpoints.csv")))
            labels={row["issue_type"] for row in rows("ground_truth.csv")}
            self.assertEqual(labels,{"DUPLICATE_REVENUE","POST_CONVERSION_TOUCH","CURRENCY_MISMATCH","MISSING_CAMPAIGN_COST","UNVERIFIED_IDENTITY_LINK"})
            weights={}
            for row in rows("attribution_results.csv"):
                key=(row["opportunity_id"],row["model_name"])
                weights[key]=weights.get(key,0.0)+float(row["credit"])
            self.assertTrue(all(abs(value-1.0)<0.000001 for value in weights.values()))
    def test_semantic_validator_accepts_valid_dataset(self):
        with tempfile.TemporaryDirectory() as temp:
            out=Path(temp)/"dataset"
            generate(20261022,12,300,1200,80,out)
            report=validate(out)
            self.assertTrue(report["valid"], report["errors"])
            self.assertEqual(report["duplicate_revenue_records"],1)
            self.assertEqual(report["ground_truth_labels"],5)

    def test_semantic_validator_rejects_unverified_identity_link(self):
        with tempfile.TemporaryDirectory() as temp:
            out=Path(temp)/"dataset"
            generate(20261022,12,300,1200,80,out)
            path=out/"web_sessions.csv"
            with path.open(encoding="utf-8",newline="") as stream:
                rows=list(csv.DictReader(stream))
            for row in rows:
                if row["id"]=="SES-EDGE-ANON-001":
                    row["contact_id"]="CON-2026-000001"
            with path.open("w",encoding="utf-8",newline="") as stream:
                writer=csv.DictWriter(stream,fieldnames=rows[0].keys())
                writer.writeheader()
                writer.writerows(rows)
            report=validate(out)
            self.assertFalse(report["valid"])
            self.assertIn("unverified identity link must remain unlinked to a contact",report["errors"])

    def test_semantic_validator_checks_export_checksum_manifest(self):
        with tempfile.TemporaryDirectory() as temp:
            out=Path(temp)/"dataset"
            generate(20261022,12,300,1200,80,out)
            split=out/"evaluation_split.json"
            split.write_text(split.read_text(encoding="utf-8")+" ",encoding="utf-8")
            report=validate(out)
            self.assertFalse(report["valid"])
            self.assertTrue(any("SHA256SUMS.txt: checksum mismatch for evaluation_split.json" in error
                                for error in report["errors"]))

    def test_same_seed_produces_same_hashes(self):
        with tempfile.TemporaryDirectory() as temp:
            first,second=Path(temp)/"first",Path(temp)/"second"
            a=generate(20261022,12,300,1200,80,first)
            b=generate(20261022,12,300,1200,80,second)
            self.assertEqual(a["counts"],b["counts"])
            self.assertEqual({k:v["sha256"] for k,v in a["files"].items()},{k:v["sha256"] for k,v in b["files"].items()})

if __name__=="__main__": unittest.main()
