const API_BASE =
  location.origin === "null" || location.protocol === "file:"
    ? "http://localhost:8080"
    : "";

const api = {
  async getRooms() {
    const res = await fetch(`${API_BASE}/api/rooms`);
    if (!res.ok) throw new Error("Failed to fetch rooms");
    return res.json();
  },
  async addRoom(payload) {
    const res = await fetch(`${API_BASE}/api/rooms`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await safeJson(res);
    if (!res.ok) throw new Error(data?.message || "Failed to add room");
    return data;
  },
  async searchRooms({ minCapacity, needsAC, needsWashroom }) {
    const qs = new URLSearchParams({
      minCapacity: String(minCapacity),
      needsAC: String(!!needsAC),
      needsWashroom: String(!!needsWashroom),
    });
    const res = await fetch(`${API_BASE}/api/rooms/search?${qs.toString()}`);
    if (!res.ok) throw new Error("Failed to search rooms");
    return res.json();
  },
  async allocate({ students, needsAC, needsWashroom }) {
    const res = await fetch(`${API_BASE}/api/rooms/allocate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ students, needsAC, needsWashroom }),
    });
    const data = await safeJson(res);
    if (!res.ok) throw new Error(data?.message || "No room available");
    return data;
  },
};

function $(sel) {
  return document.querySelector(sel);
}

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function safeJson(res) {
  try {
    return await res.json();
  } catch {
    return null;
  }
}

function badgeYesNo(v) {
  return v
    ? `<span class="badge ok">Yes</span>`
    : `<span class="badge no">No</span>`;
}

function roomRowHtml(r) {
  return `<tr>
    <td>${escapeHtml(r.roomNo)}</td>
    <td class="num">${escapeHtml(r.capacity)}</td>
    <td>${badgeYesNo(r.hasAC)}</td>
    <td>${badgeYesNo(r.hasAttachedWashroom)}</td>
  </tr>`;
}

function setOutput(el, { title, body, tone = "info" }) {
  const t =
    tone === "success"
      ? "SUCCESS"
      : tone === "danger"
        ? "ERROR"
        : tone === "warning"
          ? "NOTICE"
          : "INFO";
  el.textContent = `${t}: ${title}\n\n${body || ""}`.trim();
}

function wireTabs() {
  const tabs = Array.from(document.querySelectorAll(".tab"));
  const screens = Array.from(document.querySelectorAll(".screen"));

  function activate(name) {
    for (const t of tabs) {
      const is = t.dataset.screen === name;
      t.classList.toggle("is-active", is);
      t.setAttribute("aria-selected", is ? "true" : "false");
    }
    for (const s of screens) {
      s.classList.toggle("is-active", s.dataset.screen === name);
    }
  }

  for (const t of tabs) {
    t.addEventListener("click", () => {
      activate(t.dataset.screen);
      if (t.dataset.screen === "rooms") refreshRooms();
    });
  }
}

async function refreshRooms() {
  const tbody = $("#roomsTbody");
  tbody.innerHTML = `<tr><td colspan="4" class="muted">Loading…</td></tr>`;
  try {
    const rooms = await api.getRooms();
    if (!rooms.length) {
      tbody.innerHTML = `<tr><td colspan="4" class="muted">No rooms added yet.</td></tr>`;
      return;
    }
    tbody.innerHTML = rooms.map(roomRowHtml).join("");
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="4" class="muted">Failed to load rooms.</td></tr>`;
  }
}

function wireAddRoom() {
  const form = $("#addRoomForm");
  const out = $("#outputPanel");
  const resetBtn = $("#addRoomReset");

  resetBtn.addEventListener("click", () => {
    form.reset();
    $("#roomNo").focus();
  });

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const roomNo = $("#roomNo").value.trim();
    const capacity = Number($("#capacity").value);
    const hasAC = $("#hasAC").checked;
    const hasAttachedWashroom = $("#hasWashroom").checked;

    if (!roomNo) {
      setOutput(out, {
        title: "Room number is required",
        body: "Please enter a room number (e.g. 105).",
        tone: "warning",
      });
      return;
    }
    if (!Number.isFinite(capacity) || capacity < 1) {
      setOutput(out, {
        title: "Capacity must be at least 1",
        body: "Please enter a valid number of students.",
        tone: "warning",
      });
      return;
    }

    try {
      await api.addRoom({ roomNo, capacity, hasAC, hasAttachedWashroom });
      setOutput(out, {
        title: `Room ${roomNo} added`,
        body: `Capacity: ${capacity}\nAC: ${hasAC ? "Yes" : "No"}\nWashroom: ${hasAttachedWashroom ? "Yes" : "No"}`,
        tone: "success",
      });
      form.reset();
    } catch (e) {
      setOutput(out, {
        title: "Could not add room",
        body: String(e.message || e),
        tone: "danger",
      });
    }
  });
}

function wireRoomListRefresh() {
  $("#refreshRoomsBtn").addEventListener("click", refreshRooms);
}

function wireSearch() {
  const form = $("#searchForm");
  const tbody = $("#searchTbody");
  const clearBtn = $("#searchClearBtn");

  clearBtn.addEventListener("click", () => {
    form.reset();
    $("#minCapacity").value = "1";
    tbody.innerHTML = "";
    $("#allocationOutput").textContent = "";
  });

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const minCapacity = Number($("#minCapacity").value);
    const needsAC = $("#searchNeedsAC").checked;
    const needsWashroom = $("#searchNeedsWashroom").checked;

    tbody.innerHTML = `<tr><td colspan="4" class="muted">Searching…</td></tr>`;
    try {
      const rooms = await api.searchRooms({ minCapacity, needsAC, needsWashroom });
      if (!rooms.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="muted">No rooms match these criteria.</td></tr>`;
        return;
      }
      tbody.innerHTML = rooms.map(roomRowHtml).join("");
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="4" class="muted">Search failed.</td></tr>`;
    }
  });
}

function wireAllocate() {
  const form = $("#allocateForm");
  const out = $("#allocationOutput");

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const students = Number($("#students").value);
    const needsAC = $("#allocNeedsAC").checked;
    const needsWashroom = $("#allocNeedsWashroom").checked;

    if (!Number.isFinite(students) || students < 1) {
      setOutput(out, {
        title: "Students must be at least 1",
        body: "Please enter a valid number of students.",
        tone: "warning",
      });
      return;
    }

    out.textContent = "Allocating…";
    try {
      const r = await api.allocate({ students, needsAC, needsWashroom });
      setOutput(out, {
        title: "Room allocated (smallest fit)",
        body:
          `Room: ${r.roomNo}\n` +
          `Capacity: ${r.capacity}\n` +
          `AC: ${r.hasAC ? "Yes" : "No"}\n` +
          `Washroom: ${r.hasAttachedWashroom ? "Yes" : "No"}\n\n` +
          `Algorithm: selected the smallest capacity room meeting all requirements.`,
        tone: "success",
      });
    } catch (e) {
      setOutput(out, {
        title: "No room available",
        body: String(e.message || e),
        tone: "danger",
      });
    }
  });
}

function main() {
  wireTabs();
  wireAddRoom();
  wireRoomListRefresh();
  wireSearch();
  wireAllocate();
  refreshRooms();
}

main();

