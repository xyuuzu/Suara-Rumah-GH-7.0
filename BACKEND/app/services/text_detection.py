"""
Modul ini yang mengubah teks (transkrip chat/ucapan) jadi representasi angka
(TF-IDF vector) yang dipakai buat training atau prediksi di model scikit-learn.

Beda dengan audio (fitur akustik: MFCC, pitch, dll), untuk teks kita pakai
pendekatan Bag-of-Words / TF-IDF atas kata-kata yang sudah dinormalisasi
(lowercase, hapus tanda baca, hapus stopword Bahasa Indonesia pakai Sastrawi).
"""

import re

from Sastrawi.StopWordRemover.StopWordRemoverFactory import StopWordRemoverFactory

_stopword_remover = StopWordRemoverFactory().create_stop_word_remover()


def preprocess_text(text: str) -> str:
    """Lowercase, buang tanda baca/angka, buang stopword Bahasa Indonesia."""
    text = text.lower()
    text = re.sub(r"[^a-z\s]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return _stopword_remover.remove(text)
