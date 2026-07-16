"""
Generator dataset SEMENTARA (dummy/sintetis) supaya pipeline training bisa
langsung dicoba end-to-end tanpa harus punya data asli dulu.

INI BUKAN DATA SUNGGUH-SUNGGUH -> model yang dihasilkan dari data ini cuma
buat memastikan kode training/inferensi jalan. Ganti dengan data asli
(rekaman/transkrip beneran atau dataset publik, lihat README) sebelum
dipakai buat apa pun yang serius.

- Audio "kdrt": noise kasar + amplitudo tinggi + modulasi nada naik-turun
  cepat, meniru karakter teriakan/bentakan (RMS energy & pitch variance tinggi).
- Audio "normal": nada synth stabil dengan sedikit noise, meniru
  percakapan tenang (RMS energy rendah, pitch stabil).

- Teks "kdrt": contoh kalimat ancaman/bentakan (Bahasa Indonesia).
- Teks "normal": contoh obrolan sehari-hari yang netral.
"""

import os
import random

import numpy as np
import soundfile as sf

RNG = np.random.default_rng(42)
random.seed(42)

BASE_DIR = os.path.dirname(__file__)
AUDIO_DIR = os.path.join(BASE_DIR, "dataset")
TEXT_DIR = os.path.join(BASE_DIR, "dataset_text")

SR = 22050
DURATION = 2.5
N_SAMPLES_PER_CLASS = 25


def gen_kdrt_audio(duration=DURATION, sr=SR):
    t = np.linspace(0, duration, int(sr * duration), endpoint=False)
    base_freq = RNG.uniform(300, 500)
    vibrato = np.sin(2 * np.pi * RNG.uniform(6, 12) * t) * RNG.uniform(80, 150)
    tone = np.sin(2 * np.pi * (base_freq + vibrato) * t)
    amp_envelope = np.abs(np.sin(2 * np.pi * RNG.uniform(2, 5) * t)) ** 0.5
    noise = RNG.normal(0, 0.35, size=t.shape)
    signal = (tone * 0.7 + noise) * amp_envelope
    signal = signal / (np.max(np.abs(signal)) + 1e-9) * RNG.uniform(0.85, 0.98)
    return signal.astype(np.float32)


def gen_normal_audio(duration=DURATION, sr=SR):
    t = np.linspace(0, duration, int(sr * duration), endpoint=False)
    base_freq = RNG.uniform(100, 200)
    tone = np.sin(2 * np.pi * base_freq * t)
    amp_envelope = 0.5 + 0.1 * np.sin(2 * np.pi * RNG.uniform(0.5, 1.5) * t)
    noise = RNG.normal(0, 0.03, size=t.shape)
    signal = (tone * 0.3 + noise) * amp_envelope
    signal = signal / (np.max(np.abs(signal)) + 1e-9) * RNG.uniform(0.2, 0.4)
    return signal.astype(np.float32)


def build_audio_dataset():
    for label, gen_fn in [("kdrt", gen_kdrt_audio), ("normal", gen_normal_audio)]:
        out_dir = os.path.join(AUDIO_DIR, label)
        os.makedirs(out_dir, exist_ok=True)
        for i in range(N_SAMPLES_PER_CLASS):
            signal = gen_fn()
            sf.write(os.path.join(out_dir, f"{label}_{i:03d}.wav"), signal, SR)
        print(f"[INFO] {N_SAMPLES_PER_CLASS} file audio dummy dibuat untuk label '{label}'")


KDRT_TEXT_TEMPLATES = [
    "diam kamu, jangan macam-macam sama aku",
    "awas kalau kamu berani lawan aku lagi",
    "dasar bodoh, kamu tidak berguna",
    "aku bisa hancurkan hidup kamu kapan saja",
    "kamu pantas dipukul kalau begini terus",
    "jangan coba-coba keluar dari rumah ini",
    "gua bisa bikin kamu menyesal seumur hidup",
    "kamu bikin aku marah terus, mau kucekik ya",
    "diam! jangan banyak bicara atau kupukul kamu",
    "kamu memang goblok, tidak becus mengurus apa-apa",
    "sudah kubilang jangan bantah, awas kalau berani",
    "kamu tidak akan bisa lari dariku",
    "aku hajar kamu kalau ulangi lagi",
    "beraninya kamu melawan, akan kubuat kapok",
    "tampar kamu baru tahu rasa",
]

NORMAL_TEXT_TEMPLATES = [
    "hari ini cuacanya cerah sekali ya",
    "aku baru selesai masak buat makan malam",
    "besok kita jadi ketemuan jam berapa",
    "terima kasih sudah bantu aku hari ini",
    "kamu sudah makan siang belum tadi",
    "aku mau nonton film habis ini, mau ikut",
    "kerjaan kantor hari ini lumayan santai",
    "anak-anak main di taman sore tadi",
    "aku lagi belajar masak resep baru",
    "nanti malam kita diskusi rencana liburan ya",
    "tolong ambilkan air minum di dapur",
    "aku senang banget hari ini jalan-jalan",
    "kucing kita lucu banget lagi tidur",
    "aku baru beres beres rumah tadi pagi",
    "gimana kabar kamu hari ini, sehat kan",
]


def build_text_dataset():
    os.makedirs(TEXT_DIR, exist_ok=True)
    rows = [(t, "kdrt") for t in KDRT_TEXT_TEMPLATES] + [(t, "normal") for t in NORMAL_TEXT_TEMPLATES]
    random.shuffle(rows)

    csv_path = os.path.join(TEXT_DIR, "data.csv")
    with open(csv_path, "w", encoding="utf-8") as f:
        f.write("text,label\n")
        for text, label in rows:
            f.write(f'"{text}",{label}\n')

    print(f"[INFO] {len(rows)} baris teks dummy ditulis ke {csv_path}")


if __name__ == "__main__":
    print("[1/2] Generate dataset audio dummy...")
    build_audio_dataset()
    print("[2/2] Generate dataset teks dummy...")
    build_text_dataset()
    print("[DONE] Dataset sementara siap. Ini SINTETIS, ganti dengan data asli sebelum production.")
