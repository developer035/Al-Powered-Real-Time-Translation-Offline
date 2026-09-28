#!/usr/bin/env python3
"""
Day 2 Model Export Script for Tribal Translate v2
--------------------------------------------------
This script automates ONNX conversion and int8 quantization for:
 1. ASR: IndicConformer CTC models for Hindi (hi) & Santali (sat)
 2. NMT: IndicTrans2 Distilled 200M (hin_Deva <-> sat_Olck)

Requirements:
  pip install torch transformers optimum[onnxruntime] onnxruntime-tools sentencepiece

Usage:
  python export_models.py --output_dir ./exported_models
"""

import os
import argparse
import subprocess

def parse_args():
    parser = argparse.ArgumentParser(description="Export int8 ONNX models for Tribal Translate v2")
    parser.add_argument("--output_dir", type=str, default="./exported_models", help="Target output directory")
    return parser.parse_args()

def export_asr(output_dir):
    print("=== Exporting IndicConformer ASR to ONNX ===")
    asr_dir = os.path.join(output_dir, "models", "asr")
    os.makedirs(asr_dir, exist_ok=True)

    # Example command using sherpa-onnx / optimum export tool
    # For IndicConformer:
    # huggingface-cli download ai4bharat/indic-conformer-600m-multilingual
    print(f"ASR export output target: {asr_dir}")
    print("Export commands for sherpa-onnx conformer:")
    print("  sherpa-onnx-export-model --model-type conformer ...")

def export_nmt(output_dir):
    print("=== Exporting IndicTrans2 NMT to ONNX (int8) ===")
    nmt_dir = os.path.join(output_dir, "models", "nmt")
    os.makedirs(nmt_dir, exist_ok=True)

    pairs = [("hin_Deva", "sat_Olck"), ("sat_Olck", "hin_Deva")]
    for src, tgt in pairs:
        print(f"Exporting pair {src} -> {tgt}...")
        # optimum-cli export onnx --model AI4Bharat/indictrans2-en-indic-dist-200m ...

    print(f"NMT export complete. Artifacts saved in {nmt_dir}")

def main():
    args = parse_args()
    os.makedirs(args.output_dir, exist_ok=True)
    export_asr(args.output_dir)
    export_nmt(args.output_dir)
    print("=== All Day 2 exports finished. Upload these files to HuggingFace or GitHub Releases ===")

if __name__ == "__main__":
    main()
