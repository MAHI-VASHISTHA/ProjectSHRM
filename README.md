# Smart Hostel Room Allocation System (Web App)

This project provides a **functional Smart Hostel Room Allocation System** with:

- **Frontend**: HTML + CSS + JavaScript (responsive UI) in `web/`
- **Backend**: Java (no external dependencies) in `SmartHostelServer.java`
- **Allocation rule**: allocate the **smallest possible room** that satisfies capacity + facility requirements

## Run (Windows / PowerShell)

Prerequisite:

- Java 17+ (`java -version`)

From the project folder:

```bash
javac SmartHostelServer.java
java SmartHostelServer
```

Open the app:

- `http://localhost:8080`

## Features (per requirements)

- **Add Room** (UI + `POST /api/rooms`)
- **View All Rooms** (UI + `GET /api/rooms`)
- **Search Rooms** by capacity/AC/washroom (UI + `GET /api/rooms/search`)
- **Allocate Room** (UI + `POST /api/rooms/allocate`)

## Notes

- Rooms are saved to a local file database: `data/rooms.json` (so **restarts keep your added rooms**).
- The backend preloads a few sample rooms the first time it runs (then persists them).

