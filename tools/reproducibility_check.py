#!/usr/bin/env python3
"""Generate twice with identical inputs and compare hashes + counts."""
import argparse,json,subprocess,sys,tempfile
from pathlib import Path
def main():
    root=Path(__file__).resolve().parents[1];gen=root/"tools"/"generate_synthetic.py";p=argparse.ArgumentParser();p.add_argument("--seed",type=int,default=20261022);p.add_argument("--full",action="store_true");a=p.parse_args()
    with tempfile.TemporaryDirectory() as t:
        one,two=Path(t)/"one",Path(t)/"two";args=[sys.executable,str(gen),"--seed",str(a.seed)]
        if a.full:args.append("--full")
        subprocess.run(args+["--out",str(one)],check=True,capture_output=True);subprocess.run(args+["--out",str(two)],check=True,capture_output=True)
        x=json.loads((one/"manifest.json").read_text());y=json.loads((two/"manifest.json").read_text());hx={k:v["sha256"] for k,v in x["files"].items()};hy={k:v["sha256"] for k,v in y["files"].items()};same=x["counts"]==y["counts"] and hx==hy and (one/"SHA256SUMS.txt").read_bytes()==(two/"SHA256SUMS.txt").read_bytes()
        print(json.dumps({"seed":a.seed,"reproducible":same,"counts_match":x["counts"]==y["counts"],"checksums_match":hx==hy,"artifact_checksums_match":(one/"SHA256SUMS.txt").read_bytes()==(two/"SHA256SUMS.txt").read_bytes()},indent=2));raise SystemExit(0 if same else 1)
if __name__=="__main__":main()
