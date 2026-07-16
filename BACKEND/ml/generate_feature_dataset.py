"""
Generator dataset SEMENTARA (sintetis) buat training model klasifikasi
berbasis fitur ringan RMS/ZCR/Peak -- 3 angka yang sama persis dengan yang
dikirim mobile ke POST /predict-features (bukan audio mentah).

INI BUKAN DATA ASLI dari device fisik. Range angka tiap kelas di bawah ini
cuma perkiraan awal berdasarkan karakteristik akustik yang masuk akal:
- Obrolan/ambient normal: pelan, ga ada yang mencolok
- Bantingan (benda pecah/dibanting): transient -> peak mendadak tinggi,
  tapi energi keseluruhan (RMS) relatif rendah karena cuma sekejap
- Teriakan: energi tinggi + noisy/kasar sepanjang durasi (RMS & ZCR tinggi)
- Darurat SOS KDRT: kombinasi ekstrem di semua sumbu -- diasumsikan pola
  paling parah (teriakan + benturan berulang / perjuangan fisik)

Begitu ada data kalibrasi asli dari HP (rekam skenario nyata, lihat angka
RMS/ZCR/Peak yang keluar), GANTI generator ini dengan data asli sebelum
model dipakai buat apa pun yang serius -- baca README bagian 3.
"""

import os

import numpy as np
import pandas as pd

RNG = np.random.default_rng(42)
N_PER_CLASS = 3000

BASE_DIR = os.path.dirname(__file__)
OUT_DIR = os.path.join(BASE_DIR, "dataset_features")
OUT_PATH = os.path.join(OUT_DIR, "data.csv")


def _clip01(arr):
    return np.clip(arr, 0.0, 1.0)


# v2: range tiap kelas sengaja DILEBARIN & noise-nya digedein biar ada overlap
# antar kelas yang berdekatan (mis. normal vs bantingan, teriakan vs darurat_sos).
# Versi pertama terlalu "bersih" (hampir ga ada overlap) sehingga model belajar
# batas keputusan yang terlalu tajam -> confidence selalu persis 1.0, ga
# realistis buat data device asli yang pasti lebih berisik/ambigu.

def gen_normal(n):
    """Obrolan biasa, suara ambient/TV/musik pelan -- ga ada yang mencolok."""
    rms = _clip01(RNG.uniform(0.01, 0.16, n) + RNG.normal(0, 0.02, n))
    zcr = _clip01(RNG.uniform(0.02, 0.11, n) + RNG.normal(0, 0.02, n))
    peak = _clip01(RNG.uniform(0.08, 0.55, n) + RNG.normal(0, 0.05, n))
    return rms, zcr, peak


def gen_bantingan(n):
    """Benda dibanting/pecah -- transient, peak mendadak tinggi, RMS sustain rendah-sedang."""
    rms = _clip01(RNG.uniform(0.03, 0.30, n) + RNG.normal(0, 0.03, n))
    zcr = _clip01(RNG.uniform(0.03, 0.22, n) + RNG.normal(0, 0.03, n))
    peak = _clip01(RNG.uniform(0.62, 1.00, n) + RNG.normal(0, 0.05, n))
    return rms, zcr, peak


def gen_teriakan(n):
    """Teriakan/bentakan -- energi tinggi & noisy sepanjang durasi buffer."""
    rms = _clip01(RNG.uniform(0.15, 0.60, n) + RNG.normal(0, 0.04, n))
    zcr = _clip01(RNG.uniform(0.10, 0.44, n) + RNG.normal(0, 0.04, n))
    peak = _clip01(RNG.uniform(0.42, 0.98, n) + RNG.normal(0, 0.05, n))
    return rms, zcr, peak


def gen_darurat_sos(n):
    """Darurat SOS KDRT -- kombinasi paling ekstrem di 3 sumbu sekaligus."""
    rms = _clip01(RNG.uniform(0.38, 0.90, n) + RNG.normal(0, 0.04, n))
    zcr = _clip01(RNG.uniform(0.22, 0.60, n) + RNG.normal(0, 0.04, n))
    peak = _clip01(RNG.uniform(0.78, 1.00, n) + RNG.normal(0, 0.04, n))
    return rms, zcr, peak


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    generators = {
        "normal": gen_normal,
        "bantingan": gen_bantingan,
        "teriakan": gen_teriakan,
        "darurat_sos": gen_darurat_sos,
    }

    rows = []
    for label, gen_fn in generators.items():
        rms, zcr, peak = gen_fn(N_PER_CLASS)
        for r, z, p in zip(rms, zcr, peak):
            rows.append({"rms": round(float(r), 4), "zcr": round(float(z), 4), "peak": round(float(p), 4), "label": label})
        print(f"[INFO] {N_PER_CLASS} sample dibuat untuk label '{label}'")

    df = pd.DataFrame(rows).sample(frac=1, random_state=42).reset_index(drop=True)
    df.to_csv(OUT_PATH, index=False)
    print(f"[DONE] {len(df)} baris ditulis ke {OUT_PATH}")
    print("Ini dataset SINTETIS -- ganti dengan data kalibrasi asli sebelum dipakai serius.")


if __name__ == "__main__":
    main()
