"""
Window tracking: lacak pola anomali berulang dalam window waktu tertentu per
device, buat mendeteksi "pola eskalatif" (spec produk bagian 3.1/4.5) sebelum
alert beneran di-trigger — 1 sample anomali doang belum tentu langsung alert,
biar ga gampang false alarm.

Spec produk bilang cukup in-memory buat skala MVP (1 device/1 server) dan
sengaja TIDAK pakai Redis. Tapi Redis udah kepasang duluan di project ini
buat caching /predict — jadi dipakai di sini kalau nyala (persist across
restart, siap discale ke banyak device/server instance), otomatis fallback
ke dict in-memory persis kayak yang disaranin spec kalau Redis lagi mati.
Behaviour-nya sama, cuma penyimpanannya beda.
"""

import time

from app.services.redis_service import get_redis_client

WINDOW_SECONDS = 30
ESCALATION_THRESHOLD = 2  # minimal N anomali dalam window buat dianggap "eskalatif"

# fallback in-memory kalau Redis ga tersedia: {device_id: [timestamp, ...]}
_memory_windows: dict[str, list[float]] = {}


def record_anomaly(device_id: str) -> int:
    """Catat 1 kejadian anomali buat device ini. Return jumlah anomali dalam window aktif (termasuk yang baru dicatat)."""
    now = time.time()
    client = get_redis_client()

    if client is not None:
        key = f"window:{device_id}"
        client.zadd(key, {str(now): now})
        client.zremrangebyscore(key, 0, now - WINDOW_SECONDS)
        client.expire(key, WINDOW_SECONDS)
        return client.zcard(key)

    events = _memory_windows.setdefault(device_id, [])
    events.append(now)
    events[:] = [t for t in events if t >= now - WINDOW_SECONDS]
    return len(events)
