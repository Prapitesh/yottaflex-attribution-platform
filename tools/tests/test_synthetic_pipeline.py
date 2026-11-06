import csv
import tempfile
import unittest
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from generate_synthetic import generate

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
    def test_same_seed_produces_same_hashes(self):
        with tempfile.TemporaryDirectory() as temp:
            first,second=Path(temp)/"first",Path(temp)/"second"
            a=generate(20261022,12,300,1200,80,first)
            b=generate(20261022,12,300,1200,80,second)
            self.assertEqual(a["counts"],b["counts"])
            self.assertEqual({k:v["sha256"] for k,v in a["files"].items()},{k:v["sha256"] for k,v in b["files"].items()})

if __name__=="__main__": unittest.main()
