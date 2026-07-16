from app.main import app  # Sesuaikan import dengan lokasi file utama FastAPI kamu
from fastapi.routing import APIRoute

print(f"{'METHOD':<20} {'PATH':<40} {'NAME'}")
print("-" * 80)

for route in app.routes:
    # Kita filter khusus untuk APIRoute (mengabaikan route bawaan untuk docs/redoc kalau gak butuh)
    if isinstance(route, APIRoute):
        # route.methods bentuknya set, kita ubah jadi string biar rapi
        methods = ", ".join(route.methods)
        print(f"{methods:<20} {route.path:<40} {route.name}")