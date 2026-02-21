// ---------- helpers
const $ = (id) => document.getElementById(id);

const toast = (msg) => {
  const t = $("toast");
  if (!t) return;
  t.textContent = msg;
  t.classList.add("show");
  setTimeout(() => t.classList.remove("show"), 900);
};

const bytes = (n) => {
  if (!Number.isFinite(n)) return "";
  const u = ["B", "KB", "MB", "GB"];
  let i = 0, x = n;
  while (x >= 1024 && i < u.length - 1) { x /= 1024; i++; }
  return (i === 0 ? x : x.toFixed(1)) + " " + u[i];
};

const safeName = (s) => (s || "output").replace(/[^\w.\-]+/g, "_");

const escapeHtml = (s) =>
  (s ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));

// Highlight search matches (for modal preview)
function highlight(text, q) {
  if (!q) return escapeHtml(text);
  const safeQ = q.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const re = new RegExp(safeQ, "gi");
  return escapeHtml(text).replace(re, (m) => `<mark>${m}</mark>`);
}

function readableTextFromJson(json) {
  const meta = json?.meta || {};
  const chunks = json?.chunks || [];
  let out = "";
  out += `Файл: ${meta.filename || "-"}\n`;
  out += `Chunks: ${chunks.length}\n`;
  out += `Content-Type: ${meta.contentType || "-"}\n`;
  out += `Size: ${meta.sizeBytes ? bytes(meta.sizeBytes) : "-"}\n\n`;

  for (const c of chunks) {
    out += `----- CHUNK ${c.index} (${c.chars} chars) -----\n`;
    out += (c.text || "").trimEnd();
    out += "\n\n";
  }
  return out.trimEnd();
}

function buildMdFromChunks(json) {
  const chunks = json?.chunks || [];
  return chunks.map(c => (c.text || "").trimEnd()).join("\n\n---\n\n");
}

async function copyText(text) {
  await navigator.clipboard.writeText(text);
  toast("Copied ✅");
}

function downloadText(filename, text, mime = "text/plain;charset=utf-8") {
  const blob = new Blob([text], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 500);
}

// ---------- state
let selectedFile = null;
let lastJson = null;
let currentQuery = "";

// ---------- UI elements
const drop = $("drop");
const file = $("file");
const pickBtn = $("pickBtn");
const convertBtn = $("convertBtn");
const out = $("out");
const chunksEl = $("chunks");

const fileInfo = $("fileInfo");
const chunksInfo = $("chunksInfo");
const metaLine = $("metaLine");

const dlJsonBtn = $("dlJsonBtn");
const dlMdBtn = $("dlMdBtn");
const copyAllBtn = $("copyAllBtn");
const copyResultBtn = $("copyResultBtn");

const pingBtn = $("pingBtn");
const dot = $("dot");
const backendLabel = $("backendLabel");

const baseUrl = $("baseUrl");
const baseShown = $("baseShown");

const search = $("search");              // ✅ бір search ғана
const clearSearchBtn = $("clearSearchBtn");

const progressWrap = $("progressWrap");
const progressBar = $("progressBar");
const progressLabel = $("progressLabel");
const progressPct = $("progressPct");

const modal = $("modal");
const modalClose = $("modalClose");      // ✅ backdrop
const closeBtn = $("closeBtn");
const modalTitle = $("modalTitle");
const modalSub = $("modalSub");
const modalBody = $("modalBody");
const copyChunkBtn = $("copyChunkBtn");

const themeBtn = $("themeBtn");
const themeLabel = $("themeLabel");

// ---------- base URL reflect
if (baseUrl && baseShown) {
  baseUrl.addEventListener("input", () => {
    baseShown.textContent = baseUrl.value.trim() || "http://localhost:8091";
  });
}

// ---------- Theme toggle (Dark базовый, Light опция)
function applyTheme() {
  const saved = localStorage.getItem("docui_theme") || "dark";
  document.body.classList.toggle("light", saved === "light");
  if (themeLabel) themeLabel.textContent = saved === "light" ? "Light" : "Dark";
}
if (themeBtn) {
  themeBtn.onclick = () => {
    const cur = localStorage.getItem("docui_theme") || "dark";
    const next = cur === "dark" ? "light" : "dark";
    localStorage.setItem("docui_theme", next);
    applyTheme();
    toast(next === "light" ? "Light ✅" : "Dark ✅");
  };
}
applyTheme();

// ---------- file pick / drop
if (pickBtn && file) pickBtn.onclick = () => file.click();

if (drop && file) {
  drop.addEventListener("click", (e) => {
    if (e.target.closest("button")) return;
    file.click();
  });

  drop.addEventListener("dragover", (e) => { e.preventDefault(); drop.classList.add("drag"); });
  drop.addEventListener("dragleave", () => drop.classList.remove("drag"));
  drop.addEventListener("drop", (e) => {
    e.preventDefault();
    drop.classList.remove("drag");
    const f = e.dataTransfer.files?.[0];
    if (f) setFile(f);
  });
}

if (file) {
  file.addEventListener("change", () => {
    const f = file.files?.[0];
    if (f) setFile(f);
  });
}

function setFile(f) {
  selectedFile = f;
  if (fileInfo) fileInfo.textContent = `Selected: ${f.name} (${bytes(f.size)})`;
  if (convertBtn) convertBtn.disabled = false;
  if (out) out.textContent = "Ready. Convert бас.";
  if (metaLine) metaLine.textContent = "Ready.";
}

// ---------- backend ping
async function pingBackend() {
  const base = (baseUrl?.value || "http://localhost:8091").trim().replace(/\/+$/, "");
  if (backendLabel) backendLabel.textContent = "Checking...";
  dot?.classList.remove("ok", "bad");

  try {
    let r = await fetch(base + "/health").catch(() => null);
    if (!r || !r.ok) r = await fetch(base + "/actuator/health");
    if (!r.ok) throw new Error("health not ok");

    if (backendLabel) backendLabel.textContent = "Backend OK";
    dot?.classList.add("ok");
    toast("Backend OK");
  } catch {
    if (backendLabel) backendLabel.textContent = "Backend OFF";
    dot?.classList.add("bad");
    toast("Backend not reachable");
  }
}
if (pingBtn) pingBtn.onclick = pingBackend;

// ---------- Search
if (search) {
  search.addEventListener("input", () => {
    currentQuery = search.value.trim();
    renderChunks();
  });
}
if (clearSearchBtn) {
  clearSearchBtn.onclick = () => {
    if (!search) return;
    search.value = "";
    currentQuery = "";
    renderChunks();
    search.focus();
  };
}

// ---------- Modal
function openModal(chunk) {
  if (!modal) return;
  modal.hidden = false;

  if (modalTitle) modalTitle.textContent = `CHUNK ${chunk.index}`;
  if (modalSub) modalSub.textContent = `${chunk.chars} chars`;
  if (modalBody) modalBody.innerHTML = highlight(chunk.text || "", currentQuery);

  if (copyChunkBtn) {
    copyChunkBtn.onclick = async () => {
      await copyText(chunk.text || "");
      const ci = $("copiedInfo");
      if (ci) ci.textContent = `Copied chunk ${chunk.index}`;
    };
  }
}
function closeModal() {
  if (!modal) return;
  modal.hidden = true;
  if (modalBody) modalBody.textContent = "";
}
if (modalClose) modalClose.onclick = closeModal; // ✅ backdrop click closes
if (closeBtn) closeBtn.onclick = closeModal;

window.addEventListener("keydown", (e) => {
  if (modal && !modal.hidden && e.key === "Escape") closeModal();
});

// ---------- Progress UI
function setProgress(show, pct = 0, label = "Uploading…") {
  if (!progressWrap || !progressBar || !progressLabel || !progressPct) return;
  progressWrap.hidden = !show;
  const clamped = Math.max(0, Math.min(100, pct));
  progressBar.style.width = clamped + "%";
  progressPct.textContent = clamped + "%";
  progressLabel.textContent = label;
}

// ---------- XHR multipart with upload progress
function xhrPostMultipart(url, formData, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", url, true);

    xhr.responseType = "text";
    xhr.timeout = 120000;

    xhr.upload.onprogress = (e) => {
      if (!e.lengthComputable) return;
      const pct = Math.round((e.loaded / e.total) * 100);
      if (onProgress) onProgress(pct);
    };

    xhr.onload = () => {
      const status = xhr.status;
      const body = xhr.responseText || "";
      const contentType = (xhr.getResponseHeader("content-type") || "").toLowerCase();

      if (status >= 200 && status < 300) {
        if (contentType.includes("application/json")) {
          try {
            resolve(JSON.parse(body));
          } catch {
            reject(new Error("Invalid JSON from backend:\n" + body.slice(0, 800)));
          }
        } else {
          resolve(body);
        }
        return;
      }

      reject(new Error(`HTTP ${status}\n\n${body.slice(0, 1500)}`));
    };

    xhr.onerror = () => reject(new Error("Network error"));
    xhr.ontimeout = () => reject(new Error("Timeout (backend too slow)"));

    xhr.send(formData); // ← БҰЛ ҚАЛУЫ КЕРЕК
  });
}
// ---------- Render chunks
function renderChunks() {
  const chunks = (lastJson?.chunks || []);
  const q = currentQuery.toLowerCase();

  const filtered = chunks.filter(c => {
    if (!q) return true;
    return (c.text || "").toLowerCase().includes(q);
  });

  if (chunksInfo) chunksInfo.textContent = `${filtered.length} / ${chunks.length} chunks`;
  if (!chunksEl) return;
  chunksEl.innerHTML = "";

  filtered.forEach((c) => {
    const row = document.createElement("div");
    row.className = "chunk";

    const preview = (c.text || "").replace(/\s+/g, " ").trim().slice(0, 90);

    row.innerHTML = `
      <div style="min-width:0">
        <b style="display:block">CHUNK ${c.index}</b>
        <small>${c.chars} chars · ${escapeHtml(preview)}${preview.length >= 90 ? "…" : ""}</small>
      </div>
      <div class="row" style="justify-content:flex-end">
        <button class="btn" data-act="preview">Preview</button>
        <button class="btn" data-act="copy">Copy</button>
      </div>
    `;

    row.querySelector('[data-act="copy"]').onclick = async () => {
      await copyText(c.text || "");
      const ci = $("copiedInfo");
      if (ci) ci.textContent = `Copied chunk ${c.index}`;
    };

    row.querySelector('[data-act="preview"]').onclick = () => openModal(c);

    row.onclick = (e) => {
      if (e.target.closest("button")) return;
      openModal(c);
    };

    chunksEl.appendChild(row);
  });
}

// ---------- Convert
if (convertBtn) {
  convertBtn.onclick = async () => {
    if (!selectedFile) return;

    const base = (baseUrl?.value || "http://localhost:8091").trim().replace(/\/+$/, "");
const endpoint = base + "/api/v1/convert-smart-json";

    convertBtn.disabled = true;
    convertBtn.textContent = "Converting...";
    if (out) out.textContent = "Processing...";
    setProgress(true, 1, "Preparing…");

    if (chunksEl) chunksEl.innerHTML = "";
    if (chunksInfo) chunksInfo.textContent = "0 chunks";
    const ci = $("copiedInfo");
    if (ci) ci.textContent = "";

    if (dlJsonBtn) dlJsonBtn.disabled = true;
    if (dlMdBtn) dlMdBtn.disabled = true;
    if (copyResultBtn) copyResultBtn.disabled = true;

    lastJson = null;

    try {
      const fd = new FormData();
      fd.append("file", selectedFile);

      // Upload progress: map 0..100 => 5..90
      const json = await xhrPostMultipart(endpoint, fd, (pct) => {
        const mapped = 5 + Math.round(pct * 0.85); // 5..90
        setProgress(true, mapped, "Uploading…");
        if (pct >= 100) setProgress(true, 92, "Processing…");
      });

      lastJson = json;

      const meta = json?.meta || {};
      const chunks = json?.chunks || [];

      if (metaLine) {
        metaLine.textContent =
          `${meta.filename || selectedFile.name} • ${meta.contentType || "-"} • ${meta.sizeBytes ? bytes(meta.sizeBytes) : bytes(selectedFile.size)} • ${chunks.length} chunks`;
      }

      if (out) out.textContent = readableTextFromJson(json);
      renderChunks();

      if (dlJsonBtn) dlJsonBtn.disabled = false;
      if (dlMdBtn) dlMdBtn.disabled = false;
      if (copyResultBtn) copyResultBtn.disabled = false;

      setProgress(true, 100, "Done ✅");
      setTimeout(() => setProgress(false), 700);
      toast("Done ✅");
    } catch (err) {
      setProgress(false);
      if (out) out.textContent = "Error:\n" + (err?.message || String(err));
      toast("Error ❌");
    } finally {
      convertBtn.disabled = false;
      convertBtn.textContent = "Convert";
    }
  };
}

// ---------- Actions
if (copyAllBtn) {
  copyAllBtn.onclick = async () => {
    if (!lastJson) return toast("Nothing to copy");
    const text = (lastJson.chunks || []).map(c => (c.text || "").trimEnd()).join("\n\n");
    await copyText(text);
  };
}

if (copyResultBtn) {
  copyResultBtn.onclick = async () => {
    if (!lastJson) return;
    await copyText(readableTextFromJson(lastJson));
  };
}

if (dlJsonBtn) {
  dlJsonBtn.onclick = () => {
    if (!lastJson) return;
    const name = safeName((lastJson.meta?.filename || "output") + ".json");
    downloadText(name, JSON.stringify(lastJson, null, 2), "application/json;charset=utf-8");
  };
}

if (dlMdBtn) {
  dlMdBtn.onclick = () => {
    if (!lastJson) return;
    const name = safeName((lastJson.meta?.filename || "output").replace(/\.[^.]+$/, "") + ".md");
    downloadText(name, buildMdFromChunks(lastJson), "text/markdown;charset=utf-8");
  };
}

// ---------- initial ping
pingBackend();