# Synthetic data tooling

Run from the repository root:

```powershell
python tools/generate_synthetic.py
python tools/validate_dataset.py
python tools/reproducibility_check.py
```

Full challenge-scale data:

```powershell
python tools/generate_synthetic.py --full
python tools/validate_dataset.py
python tools/reproducibility_check.py --full
```

Use `--seed 20261022` to reproduce. Use `--out <folder>` to write exports elsewhere. The Python tools use only the standard library.
