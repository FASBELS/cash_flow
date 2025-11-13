// Frontend/script-proyectado.js
// Misma lógica base que Flujo Real, pero aislada con IDs terminados en "Proy"

const PROJECTS_BASE = "http://localhost:8080/api/proyectos";
const API_BASE      = "http://localhost:8080/api";

let proyectosProy = [];
let conceptosCargadosProy = { ingresos: [], egresos: [] };
let proyectoSeleccionadoProy = null;
let annoSeleccionadoProy = null;
let valoresProyActivo = false; // <-- NUEVO: indica si ya se activó el modo "valores proyectados"
let edicionProyActiva = false; // <-- modo edición de valores proyectados
let cacheValoresProyPorAnno = {}; // { [anno]: [ { codPartida, ingEgr, valores[15] } ] }

// --- Referencias UI Proyectado ---
const proyectoInfoProyEl = document.getElementById("proyectoInfoProy");
const codCiaProyHidden   = document.getElementById("codCiaProyHidden");
const codPytoProyHidden  = document.getElementById("codPytoProyHidden");
const statusMsgProyEl    = document.getElementById("statusMsgProy");

function setStatusProy(msg) {
  if (statusMsgProyEl) statusMsgProyEl.textContent = msg || "";
}

// === Helpers para leer versión y mapear árbol ===
// === Helpers para leer versión y mapear árbol ===
function getVersionActualProy() {
  const sel = document.getElementById("versionSelect");
  const v = sel?.value?.trim();
  return v || "1";
}

// Mapea { partida:{...}, hijos:[...] } a un nodo plano
function mapEntradaPartidaProy(entry, nivelBase = 1) {
  if (!entry) return null;

  const p = entry.partida || entry;

  const nodo = {
    codPartida: p.codPartida ?? p.CodPartida,
    ingEgr: (p.ingEgr ?? p.IngEgr ?? "").toUpperCase(),
    desPartida: p.desPartida ?? p.DesPartida ?? "",
    nivel: Number(p.nivel ?? p.Nivel ?? nivelBase ?? 1),
    orden: p.orden ?? p.Orden ?? null,
    children: []
  };

  const hijos = entry.hijos || entry.children || [];
  if (Array.isArray(hijos) && hijos.length) {
    nodo.children = hijos
      .map(h => mapEntradaPartidaProy(h, (nodo.nivel || 1) + 1))
      .filter(Boolean);
  }

  return nodo;
}

// Aplana árbol en preorden
function flattenTreeProy(tree, out = []) {
  const visit = (n) => {
    out.push(n);
    (n.children || []).forEach(ch => visit(ch));
  };
  (tree || []).forEach(visit);
  return out;
}


// Ordenar por código de partida (y nivel como desempate)
function ordenarPorPartidaProy(a, b) {
  const ca = Number(a.codPartida || 0);
  const cb = Number(b.codPartida || 0);

  if (ca !== cb) return ca - cb;
  // si por alguna razón tienen el mismo codPartida, usamos nivel
  return (a.nivel || 0) - (b.nivel || 0);
}


// =========================
// HELPERS DE TABLA / FORMATO
// =========================

function formatNumber(value) {
  if (value == null) return "0.00";
  const num = typeof value === "number" ? value : Number(value);
  if (isNaN(num)) return "0.00";
  return num.toLocaleString("es-PE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function createNumberCell(value) {
  const td = document.createElement("td");
  td.textContent = formatNumber(value);
  td.classList.add("cell-number", "text-center");
  return td;
}

function createTextCell(text, extraClasses = []) {
  const td = document.createElement("td");
  td.textContent = text || "";
  td.classList.add("concepto-column", ...extraClasses);
  return td;
}

// =========================
// TABLA BASE (header + filas vacías)
// =========================

function crearHeaderTablaProy() {
  const headerRow = document.getElementById("headerRowProy");
  if (!headerRow) return;

  headerRow.innerHTML = "";
  const cols = [
    "Concepto","Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio",
    "Agosto","Septiembre","Octubre","Noviembre","Diciembre","Suma","Acum. Ant.","Total",
  ];
  cols.forEach((c) => {
    const th = document.createElement("th");
    th.textContent = c;
    if (c === "Concepto") th.classList.add("concepto-column");
    headerRow.appendChild(th);
  });
}

function agregarFilasBaseProy() {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;
  tbody.innerHTML = "";

  // INGRESOS
  const trIngHeader = document.createElement("tr");
  trIngHeader.classList.add("separator-row");
  const tdIng = document.createElement("td");
  tdIng.colSpan = 16;
  tdIng.textContent = "INGRESOS";
  tdIng.classList.add("separator-cell");
  trIngHeader.appendChild(tdIng);
  tbody.appendChild(trIngHeader);

  // EGRESOS
  const trEgrHeader = document.createElement("tr");
  trEgrHeader.classList.add("separator-row");
  const tdEgr = document.createElement("td");
  tdEgr.colSpan = 16;
  tdEgr.textContent = "EGRESOS";
  tdEgr.classList.add("separator-cell");
  trEgrHeader.appendChild(tdEgr);
  tbody.appendChild(trEgrHeader);

  // NETO
  const trNet = document.createElement("tr");
  trNet.classList.add("separator-row", "separator-neto");
  const tdNet = document.createElement("td");
  tdNet.textContent = "FLUJO DE CAJA NETO PROYECTADO";
  tdNet.classList.add("separator-cell");
  trNet.appendChild(tdNet);
  for (let i = 0; i < 15; i++) {
    const td = createNumberCell(0);
    trNet.appendChild(td);
  }
  tbody.appendChild(trNet);
}

// Reset solo de tabla proyectada
function resetTablaProy(resetProyecto = false) {
  conceptosCargadosProy = { ingresos: [], egresos: [] };
  proyectoSeleccionadoProy = resetProyecto ? null : proyectoSeleccionadoProy;
  annoSeleccionadoProy = resetProyecto ? null : annoSeleccionadoProy;
  valoresProyActivo = false; // <-- NUEVO: al resetear, apagamos modo valores

  if (resetProyecto) {
    const selectProyectoProy = document.getElementById("selectProyectoProy");
    const yearSelectProy = document.getElementById("yearSelectProy");
    const fechaInicioProy = document.getElementById("fechaInicioProy");
    const fechaFinProy = document.getElementById("fechaFinProy");

    if (selectProyectoProy) selectProyectoProy.value = "";
    if (yearSelectProy) {
      yearSelectProy.innerHTML = '<option value="">Año</option>';
      yearSelectProy.disabled = true;
    }
    if (fechaInicioProy) fechaInicioProy.value = "";
    if (fechaFinProy) fechaFinProy.value = "";
    if (proyectoInfoProyEl) proyectoInfoProyEl.textContent = "";
    if (codCiaProyHidden) codCiaProyHidden.value = "";
    if (codPytoProyHidden) codPytoProyHidden.value = "";
    setStatusProy("");
  }

  agregarFilasBaseProy();
}

function createTextCell(text, extraClasses = []) {
  const td = document.createElement("td");
  td.textContent = text || "";
  td.classList.add("concepto-column", ...extraClasses);
  return td;
}

// 🔹 NUEVO: limpia todos los valores numéricos de la tabla (meses, suma, acum, total)
function limpiarValoresTablaProy() {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  tbody.querySelectorAll("tr.data-row-proy td[data-col-index], tr.separator-neto td[data-col-index]")
    .forEach(td => {
      td.textContent = formatNumber(0);
    });
}

// =========================
// CARGA DE PROYECTOS
// =========================

async function cargarProyectosProy() {
  const select = document.getElementById("selectProyectoProy");
  if (!select) return;

  const res = await fetch(PROJECTS_BASE, { mode: "cors" });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`Error al obtener proyectos: ${res.status} ${txt}`);
  }

  proyectosProy = await res.json();
  select.innerHTML = '<option value="">-- Seleccione proyecto --</option>';

  proyectosProy.forEach((p) => {
    const opt = document.createElement("option");
    opt.value = String(p.codPyto);
    opt.textContent = p.nombPyto;
    opt.dataset.annoIni = p.annoIni;
    opt.dataset.annoFin = p.annoFin;
    select.appendChild(opt);
  });

  select.disabled = false;
}

// =========================
// CARGA DE CONCEPTOS
// =========================

// === CARGA DE CONCEPTOS DESDE /proyectos/{cia}/{pyto}/{ver}/arbol ===
async function cargarConceptosProy(codPyto) {
  try {
    if (!proyectoSeleccionadoProy) {
      throw new Error("Seleccione primero un proyecto.");
    }

    const codCia = proyectoSeleccionadoProy.codCia;
    const ver = getVersionActualProy();

    const url = `${API_BASE}/proyectos/${codCia}/${codPyto}/${ver}/arbol`;
    console.log("[PROY] FETCH árbol:", url);

    const res = await fetch(url, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(`HTTP ${res.status} ${txt}`);
    }

    const data = await res.json();
    console.log("[PROY] Árbol crudo:", data);

    const arrI = data?.porIngEgr?.I ?? [];
    const arrE = data?.porIngEgr?.E ?? [];

    const ingresosRoots = arrI
      .map(e => mapEntradaPartidaProy(e, 1))
      .filter(Boolean);
    const egresosRoots = arrE
      .map(e => mapEntradaPartidaProy(e, 1))
      .filter(Boolean);

    // aplanamos en listas preorden (SIN sort)
    conceptosCargadosProy.ingresos = flattenTreeProy(ingresosRoots, []);
    conceptosCargadosProy.egresos  = flattenTreeProy(egresosRoots,  []);

    // pintamos usando tu renderer
    renderConceptosProy();
    setStatusProy("Árbol de partidas cargado.");
  } catch (err) {
    console.error("ERROR cargarConceptosProy:", err);
    alert("No se pudo cargar el árbol de partidas: " + err.message);
  }
}



function crearFilaPartidaProy(partida) {
  const tr = document.createElement("tr");

  // 🔹 Identificación para guardado
  tr.classList.add("data-row-proy");
  if (partida.ingEgr) {
    tr.dataset.ingEgr = partida.ingEgr;    // 'I' o 'E'
  }

  // 🔹 Nivel y si es hoja (tiene hijos o no)
  const nivel = Number(partida.nivel ?? 1);
  const esHoja = !partida.children || partida.children.length === 0;
  tr.dataset.nivel = String(nivel);
  tr.dataset.esHoja = esHoja ? "1" : "0";

  const tdConcepto = document.createElement("td");
  tdConcepto.textContent = partida.desPartida;
  tdConcepto.dataset.codPartida = partida.codPartida;
  tdConcepto.classList.add("concepto-column");

  // Sangría por nivel
  tdConcepto.style.paddingLeft = `${Math.max(0, nivel - 1) * 16}px`;

  tr.appendChild(tdConcepto);

  // 12 meses + Suma + Acum Ant. + Total
  for (let i = 0; i < 15; i++) {
    const td = createNumberCell(0);
    td.dataset.codPartida = partida.codPartida;
    td.dataset.colIndex = i;

    // marcar columnas de mes (0..11) con data-mes
    if (i < 12) {
      td.dataset.mes = i + 1;

      // estas serán editables cuando se pulse "Valores"
      if (esHoja) {
        td.classList.add("editable-mes-proy", "editable-cell");
      }
    }

    tr.appendChild(td);
  }

  return tr;
}


function renderConceptosProy() {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  tbody.innerHTML = "";

  // INGRESOS header
  const trIngHeader = document.createElement("tr");
  trIngHeader.classList.add("separator-row");
  const tdIng = document.createElement("td");
  tdIng.colSpan = 16;
  tdIng.textContent = "INGRESOS";
  tdIng.classList.add("separator-cell");
  trIngHeader.appendChild(tdIng);
  tbody.appendChild(trIngHeader);

  conceptosCargadosProy.ingresos.forEach((p) => {
    tbody.appendChild(crearFilaPartidaProy(p));
  });

  // EGRESOS header
  const trEgrHeader = document.createElement("tr");
  trEgrHeader.classList.add("separator-row");
  const tdEgr = document.createElement("td");
  tdEgr.colSpan = 16;
  tdEgr.textContent = "EGRESOS";
  tdEgr.classList.add("separator-cell");
  trEgrHeader.appendChild(tdEgr);
  tbody.appendChild(trEgrHeader);

  conceptosCargadosProy.egresos.forEach((p) => {
    tbody.appendChild(crearFilaPartidaProy(p));
  });

  // NETO
  const trNet = document.createElement("tr");
  trNet.classList.add("separator-row", "separator-neto");
  const tdNet = document.createElement("td");
  tdNet.textContent = "FLUJO DE CAJA NETO PROYECTADO";
  tdNet.classList.add("separator-cell");
  trNet.appendChild(tdNet);

  for (let i = 0; i < 15; i++) {
    const td = createNumberCell(0);
    td.dataset.colIndex = i;
    if (i < 12) {
      td.dataset.mes = i + 1;   // <- importante para recalcular por mes
    }
    trNet.appendChild(td);
  }
  tbody.appendChild(trNet);

  if (!conceptosCargadosProy.ingresos.length && !conceptosCargadosProy.egresos.length) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 16;
    td.textContent = "Sin conceptos para este proyecto.";
    tr.appendChild(td);
    tbody.appendChild(tr);
  }
}
function recalcularFilaPartidaProy(tr) {
  const tds = Array.from(tr.querySelectorAll("td"));
  const celdasMes = tds.filter(td => td.dataset.mes);

  let suma = 0;
  celdasMes.forEach(td => {
    const txt = (td.textContent || "").replace(/,/g, "").trim();
    const num = parseFloat(txt);
    if (!isNaN(num)) suma += num;
  });

  const tdSuma  = tds.find(td => td.dataset.colIndex === "12");
  const tdAcum  = tds.find(td => td.dataset.colIndex === "13");
  const tdTotal = tds.find(td => td.dataset.colIndex === "14");

  if (tdSuma) tdSuma.textContent = formatNumber(suma);

  const acumAnt = tdAcum
    ? parseFloat((tdAcum.textContent || "").replace(/,/g, "").trim()) || 0
    : 0;

  if (tdAcum)  tdAcum.textContent  = formatNumber(acumAnt);
  if (tdTotal) tdTotal.textContent = formatNumber(suma + acumAnt);
}

// =========================
// RENDER VALORES PROYECTADOS (desde backend)
// =========================
function construirFilasParaAnnoProy(anio) {
  const tbody = document.getElementById("bodyRowsProy");
  const filas = [];

  if (!tbody || !proyectoSeleccionadoProy) return filas;

  let orden = 1;
  const rows = tbody.querySelectorAll("tr.data-row-proy");

  rows.forEach(tr => {
    const first = tr.querySelector("td.concepto-column");
    if (!first) return;

    const codPartida = parseInt(first.dataset.codPartida, 10);
    const ingEgr = tr.dataset.ingEgr || "";
    if (!codPartida || !ingEgr) return;

    const desPartida = (first.textContent || "").trim();

    const tds = tr.querySelectorAll("td");
    const impMes = [];

    // columnas 1..12 = meses proyectados en la grilla
    for (let i = 1; i <= 12 && i < tds.length; i++) {
      const td = tds[i];
      const txt = (td.textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      impMes.push(isNaN(num) ? 0 : num);
    }

    // 👉 SIEMPRE guardamos la fila, incluso si todos los meses son 0
    filas.push({
      anno: anio,
      codCia: proyectoSeleccionadoProy.codCia,
      codPyto: proyectoSeleccionadoProy.codPyto,
      ingEgr,
      codPartida,
      orden: orden++,
      tipo: "M",          // tipo de fila (detalle)
      desPartida,
      impMes              // array de 12 posiciones
    });
  });

  return filas;
}

// Lee un año ya guardado desde el backend y lo convierte a DTO de guardado
async function construirFilasParaAnnoProyDesdeBackend(anio) {
  if (!proyectoSeleccionadoProy) return [];

  const { codCia, codPyto } = proyectoSeleccionadoProy;
  const url = `${API_BASE}/flujo-proyectado/valores?codCia=${codCia}&codPyto=${codPyto}&anno=${anio}`;
  console.log("[PROY] Cargando valores proyectados para año", anio, "=>", url);

  const res = await fetch(url, { mode: "cors" });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    console.error("Error al obtener valores proyectados para año", anio, txt);
    return [];
  }

  const data = await res.json(); // lista de FilaFlujoDTO
  const filas = [];
  let orden = 1;

  (data || []).forEach(f => {
    // saltamos la fila de NETO (IngEgr = 'N' u otro distinto de I/E)
    if (f.ingEgr !== "I" && f.ingEgr !== "E") return;

    const mesesSrc = (f.valores && f.valores.mes) || [];
    const impMes = [];
    for (let i = 0; i < 12; i++) {
      const v = mesesSrc[i];
      const num = typeof v === "number" ? v : parseFloat(v);
      impMes.push(isNaN(num) ? 0 : num);
    }

    filas.push({
      anno: anio,
      codCia,
      codPyto,
      ingEgr: f.ingEgr,
      codPartida: f.codPartida,
      orden: orden++,
      tipo: "M",
      desPartida: f.desPartida || "",
      impMes
    });
  });

  return filas;
}


function cachearTablaProyParaAnno(anio) {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;
  if (!proyectoSeleccionadoProy) return;

  const rows = tbody.querySelectorAll("tr.data-row-proy");
  const filas = [];

  rows.forEach(tr => {
    const conceptCell = tr.querySelector("td.concepto-column");
    if (!conceptCell) return;

    const codPartida = parseInt(conceptCell.dataset.codPartida, 10);
    const ingEgr = tr.dataset.ingEgr || "";
    if (!codPartida || !ingEgr) return;

    const tds = tr.querySelectorAll("td");
    const valores = [];

    // tomamos 15 columnas numéricas: 12 meses + suma + acum + total
    for (let i = 1; i < tds.length && i <= 15; i++) {
      const txt = (tds[i].textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      valores.push(isNaN(num) ? 0 : num);
    }

    filas.push({ codPartida, ingEgr, valores });
  });

  cacheValoresProyPorAnno[anio] = filas;
}

function restaurarTablaProyDesdeCache(anio) {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;
  if (!proyectoSeleccionadoProy) return;

  const filas = cacheValoresProyPorAnno[anio];

  // Si no hay nada guardado para ese año, dejamos todo en 0.00
  if (!filas) {
    const celdas = tbody.querySelectorAll("tr.data-row-proy td:not(.concepto-column)");
    celdas.forEach(td => {
      td.textContent = formatNumber(0);
    });

    // 🔹 Muy importante: recalcular el Neto en base a esos ceros
    recalcularFilaNetoProy();
    return;
  }

  // Si SÍ hay cache, restauramos cada fila
  filas.forEach(f => {
    const selector = `tr.data-row-proy[data-ing-egr="${f.ingEgr}"] td.concepto-column[data-cod-partida="${f.codPartida}"]`;
    const conceptCell = tbody.querySelector(selector);
    if (!conceptCell) return;

    const tr = conceptCell.parentElement;
    const tds = tr.querySelectorAll("td");

    f.valores.forEach((v, idx) => {
      const pos = idx + 1; // porque [0] es el concepto
      if (pos < tds.length) {
        tds[pos].textContent = formatNumber(v);
      }
    });
  });

  // 🔹 Después de restaurar las filas, recalculamos el Neto
  recalcularFilaNetoProy();
}
// Construye las filas DTO para un año usando el cacheValoresProyPorAnno
function construirFilasParaAnnoProyDesdeCache(anio) {
  const filasCache = cacheValoresProyPorAnno[anio];
  const filas = [];

  if (!proyectoSeleccionadoProy || !filasCache) return filas;

  let orden = 1;

  filasCache.forEach(reg => {
    const { codPartida, ingEgr, valores } = reg;
    if (!codPartida || !ingEgr) return;

    // 12 meses (posiciones 0..11 del array valores)
    const impMes = [];
    for (let i = 0; i < 12; i++) {
      const v = valores && valores[i] != null ? valores[i] : 0;
      const num = parseFloat(String(v).replace(/,/g, "").trim());
      impMes.push(isNaN(num) ? 0 : num);
    }

    // **MISMA REGLA** que en construirFilasParaAnnoProy:
    // si no hay ningún valor distinto de 0, no mandamos esa fila
    const tieneDatos = impMes.some(v => v !== 0);
    if (!tieneDatos) return;

    filas.push({
      anno: anio,
      codCia: proyectoSeleccionadoProy.codCia,
      codPyto: proyectoSeleccionadoProy.codPyto,
      ingEgr,
      codPartida,
      orden: orden++,
      tipo: "M",
      desPartida: "",     // opcional, si quieres podrías guardar el texto también
      impMes
    });
  });

  return filas;
}


function recalcularPadresDesdeFilaProy(tr) {
  const tbody = tr.parentElement;
  const filas = Array.from(tbody.querySelectorAll("tr.data-row-proy"));
  let idx = filas.indexOf(tr);
  if (idx === -1) return;

  let nivelHijo = Number(tr.dataset.nivel || 1);
  const ingEgrHijo = tr.dataset.ingEgr || "";

  for (let i = idx - 1; i >= 0; i--) {
    const posiblePadre = filas[i];
    const nivelPadre = Number(posiblePadre.dataset.nivel || 1);

    if (nivelPadre < nivelHijo && posiblePadre.dataset.ingEgr === ingEgrHijo) {
      sumarHijosEnPadreProy(posiblePadre, filas, i);
      nivelHijo = nivelPadre; // seguir subiendo por la rama
    }
  }
}

// Suma todas las hojas descendientes de 'padre' y pone el resultado en la fila padre
function sumarHijosEnPadreProy(padre, filas, idxPadre) {
  const nivelPadre = Number(padre.dataset.nivel || 1);
  const meses = new Array(12).fill(0);

  for (let i = idxPadre + 1; i < filas.length; i++) {
    const f = filas[i];
    const nivelFila = Number(f.dataset.nivel || 1);
    if (nivelFila <= nivelPadre) break; // ya se acabaron los hijos

    if (f.dataset.esHoja !== "1") continue; // solo sumar hojas

    const celdasMes = Array.from(f.querySelectorAll("td[data-mes]"));
    celdasMes.forEach(td => {
      const mesIdx = Number(td.dataset.mes) - 1;
      const txt = (td.textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      if (!isNaN(num)) meses[mesIdx] += num;
    });
  }

  const celdasMesPadre = Array.from(padre.querySelectorAll("td[data-mes]"));
  let suma = 0;
  celdasMesPadre.forEach(td => {
    const mesIdx = Number(td.dataset.mes) - 1;
    const val = meses[mesIdx] || 0;
    td.textContent = formatNumber(val);
    suma += val;
  });

  const tds = Array.from(padre.querySelectorAll("td"));
  const tdSuma = tds.find(td => td.dataset.colIndex === "12");
  const tdAcum = tds.find(td => td.dataset.colIndex === "13");
  const tdTotal = tds.find(td => td.dataset.colIndex === "14");

  if (tdSuma) tdSuma.textContent = formatNumber(suma);
  const acumAnt = tdAcum
    ? parseFloat((tdAcum.textContent || "").replace(/,/g, "").trim()) || 0
    : 0;

  if (tdAcum) tdAcum.textContent = formatNumber(acumAnt);
  if (tdTotal) tdTotal.textContent = formatNumber(suma + acumAnt);
}

function recalcularFilaNetoProy() {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  // 🔹 ANTES: tomabas TODAS las filas data-row-proy
  // const filas = Array.from(tbody.querySelectorAll("tr.data-row-proy"));

  // 🔹 AHORA: solo filas de NIVEL 1 (Venta de Energía, Mantenimiento, Pago de Personal)
  const filas = Array.from(
    tbody.querySelectorAll('tr.data-row-proy[data-nivel="1"]')
  );

  const netoMes = new Array(12).fill(0);

  filas.forEach(tr => {
    const signo =
      tr.dataset.ingEgr === "I"
        ? 1
        : tr.dataset.ingEgr === "E"
        ? -1
        : 0;
    if (!signo) return;

    const celdasMes = Array.from(tr.querySelectorAll("td[data-mes]"));
    celdasMes.forEach(td => {
      const idxMes = Number(td.dataset.mes) - 1;
      const txt = (td.textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      if (!isNaN(num)) netoMes[idxMes] += signo * num;
    });
  });

  const trNet = tbody.querySelector("tr.separator-neto");
  if (!trNet) return;

  const celdasMesNet = Array.from(trNet.querySelectorAll("td[data-mes]"));
  let suma = 0;
  celdasMesNet.forEach((td, idx) => {
    const val = netoMes[idx] || 0;
    td.textContent = formatNumber(val);
    suma += val;
  });

  const tdsNet = Array.from(trNet.querySelectorAll("td"));
  const tdSumaNet = tdsNet.find(td => td.dataset.colIndex === "12");
  const tdAcumNet = tdsNet.find(td => td.dataset.colIndex === "13");
  const tdTotalNet = tdsNet.find(td => td.dataset.colIndex === "14");

  if (tdSumaNet) tdSumaNet.textContent = formatNumber(suma);
  const acumAnt =
    tdAcumNet
      ? parseFloat(
          (tdAcumNet.textContent || "").replace(/,/g, "").trim()
        ) || 0
      : 0;

  if (tdAcumNet) tdAcumNet.textContent = formatNumber(acumAnt);
  if (tdTotalNet) tdTotalNet.textContent = formatNumber(suma + acumAnt);
}



function activarEdicionValoresProy() {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  if (edicionProyActiva) {
    setStatusProy("La edición de valores ya está activa.");
    return;
  }

  edicionProyActiva = true;
  valoresProyActivo = true;

  // Empezamos en cero
  limpiarValoresTablaProy();

  // Hacer editables solo las hojas
  tbody.querySelectorAll("td.editable-mes-proy").forEach(td => {
    td.contentEditable = "true";
    td.classList.add("proy-editable-activa");
  });

  // Escuchar cambios una sola vez
  tbody.addEventListener("blur", onBlurCeldaProy, true);

  setStatusProy("Edición de valores proyectados activada. Ingrese montos en las partidas de nivel más bajo.");
}

function onBlurCeldaProy(ev) {
  const td = ev.target.closest("td.editable-mes-proy");
  if (!td) return;

  let txt = td.textContent || "";
  txt = txt.replace(/[^0-9,.\-]/g, "").replace(",", ".");
  let num = parseFloat(txt);
  if (isNaN(num)) num = 0;

  td.textContent = formatNumber(num);

  const tr = td.closest("tr");
  if (!tr) return;

  recalcularFilaPartidaProy(tr);
  recalcularPadresDesdeFilaProy(tr);
  recalcularFilaNetoProy();
}

// =========================
// EVENTOS
// =========================

function setupEventListenersProy() {
  const selectProyecto = document.getElementById("selectProyectoProy");
  if (!selectProyecto) return; // no estamos en la pantalla proyectada

  const btnProyectos        = document.getElementById("btnProyectosProy");
  const yearSelect          = document.getElementById("yearSelectProy");
  const fechaInicio         = document.getElementById("fechaInicioProy");
  const fechaFin            = document.getElementById("fechaFinProy");
  const btnConceptoProy     = document.getElementById("btnConceptoProy");
  const btnValoresProy      = document.getElementById("btnValoresProy");
  const btnGuardarProy      = document.getElementById("btnGuardarProy");
  const btnGuardarTodosProy = document.getElementById("btnGuardarTodosProy");

  // ===============================
  // BOTÓN PROYECTOS
  // ===============================
  if (btnProyectos) {
    btnProyectos.addEventListener("click", async (ev) => {
      const btn = ev.currentTarget;

      if (!btn.classList.contains("btn-off")) return;

      btn.disabled = true;
      const oldText = btn.textContent;
      btn.textContent = "Cargando...";

      try {
        await cargarProyectosProy();
        btn.classList.remove("btn-off");
        btn.textContent = "Proyectos";
        setStatusProy("Proyectos cargados. Seleccione uno.");
        selectProyecto.focus();
      } catch (err) {
        console.error(err);
        alert("No se pudieron cargar los proyectos: " + err.message);
        btn.textContent = oldText;
      } finally {
        btn.disabled = false;
      }
    });
  }

  // ===============================
  // CAMBIO DE PROYECTO
  // ===============================
  selectProyecto.addEventListener("change", (e) => {
    const value = e.target.value;

    if (!value) {
      resetTablaProy(true);
      return;
    }

    resetTablaProy(false);
    valoresProyActivo = false;
    edicionProyActiva = false;
    cacheValoresProyPorAnno = {};  // 🆕 limpiamos todo el cache al cambiar proyecto

    const optSel  = e.target.options[e.target.selectedIndex];
    const codPyto = parseInt(value, 10);
    const codCia  = 1;
    const annoIni = parseInt(optSel.dataset.annoIni, 10);
    const annoFin = parseInt(optSel.dataset.annoFin, 10);

    proyectoSeleccionadoProy = { codCia, codPyto, annoIni, annoFin };

    if (codCiaProyHidden)  codCiaProyHidden.value  = codCia;
    if (codPytoProyHidden) codPytoProyHidden.value = codPyto;
    if (proyectoInfoProyEl) {
      proyectoInfoProyEl.textContent = `Proyecto: ${optSel.textContent}`;
    }

    // Poblar selector de años
    if (yearSelect) {
      yearSelect.innerHTML = '<option value="">Año</option>';
      for (let y = annoIni; y <= annoFin; y++) {
        const op = document.createElement("option");
        op.value = String(y);
        op.textContent = y;
        yearSelect.appendChild(op);
      }
      yearSelect.disabled = false;

      annoSeleccionadoProy = annoIni;
      yearSelect.value = String(annoIni);
    }

    if (fechaInicio) fechaInicio.value = `${annoIni}-01-01`;
    if (fechaFin)    fechaFin.value    = `${annoFin}-12-31`;

    setStatusProy("Proyecto seleccionado. Cargue conceptos y luego active Valores.");
  });

  // ===============================
  // CAMBIO DE AÑO CON CACHE
  // ===============================
  if (yearSelect) {
    yearSelect.addEventListener("change", (e) => {
      if (!proyectoSeleccionadoProy) return;

      const nuevoAnno = parseInt(e.target.value, 10);
      if (!nuevoAnno) return;

      // Guardar el año anterior antes de cambiar
      if (annoSeleccionadoProy != null) {
        cachearTablaProyParaAnno(annoSeleccionadoProy);
      }

      // Cambiar año
      annoSeleccionadoProy = nuevoAnno;
      setStatusProy(`Año ${annoSeleccionadoProy} seleccionado.`);

      // Restaurar valores desde cache
      restaurarTablaProyDesdeCache(annoSeleccionadoProy);
    });
  }

  // ===============================
  // BOTÓN CONCEPTO
  // ===============================
  if (btnConceptoProy) {
    btnConceptoProy.addEventListener("click", async (ev) => {
      if (!proyectoSeleccionadoProy) {
        alert("Seleccione primero un proyecto.");
        return;
      }

      const btn = ev.currentTarget;
      btn.disabled = true;
      const old = btn.textContent;
      btn.textContent = "Cargando conceptos...";

      try {
        await cargarConceptosProy(proyectoSeleccionadoProy.codPyto);
        setStatusProy("Conceptos cargados. Ahora puede activar 'Valores'.");
      } catch (err) {
        console.error("ERROR cargarConceptosProy:", err);
        alert("No se pudieron cargar los conceptos: " + err.message);
      } finally {
        btn.textContent = old;
        btn.disabled = false;
      }
    });
  }

  // ===============================
  // BOTÓN VALORES → activar edición
  // ===============================
  if (btnValoresProy) {
    btnValoresProy.addEventListener("click", () => {
      if (!proyectoSeleccionadoProy) {
        alert("Seleccione primero un proyecto y cargue los conceptos.");
        return;
      }

      if (!conceptosCargadosProy.ingresos.length &&
          !conceptosCargadosProy.egresos.length) {
        alert("Primero cargue los conceptos (botón Concepto).");
        return;
      }

      activarEdicionValoresProy();
    });
  }

  // ===============================
  // BOTONES GUARDAR
  // ===============================
  if (btnGuardarProy) {
    btnGuardarProy.addEventListener("click", () => {
      guardarFlujoProyectado();
    });
  }

  if (btnGuardarTodosProy) {
    btnGuardarTodosProy.addEventListener("click", () => {
      guardarTodosLosAniosProy();
    });
  }
}



async function guardarFlujoProyectado() {
  if (!proyectoSeleccionadoProy || !annoSeleccionadoProy) {
    alert("Seleccione proyecto y año antes de guardar.");
    return;
  }

  const filas = construirFilasParaAnnoProy(annoSeleccionadoProy);

  if (!filas.length) {
    alert("No hay filas de valores para guardar.");
    return;
  }

  const btnGuardar = document.getElementById("btnGuardarProy");
  const oldText = btnGuardar ? btnGuardar.textContent : "";

  try {
    if (btnGuardar) {
      btnGuardar.disabled = true;
      btnGuardar.textContent = "Guardando...";
    }

    setStatusProy("Guardando flujo proyectado...");

    // 🔹 AQUÍ EL CAMBIO: endpoint correcto del backend
    const res = await fetch(`${API_BASE}/flujo-proyectado/guardar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(filas)
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || `HTTP ${res.status}`);
    }

    setStatusProy("Flujo de caja proyectado guardado correctamente.");
    alert("Flujo de caja proyectado guardado correctamente ✅");
  } catch (err) {
    console.error("Error al guardar flujo proyectado:", err);
    setStatusProy("Error al guardar flujo proyectado.");
    alert("Ocurrió un error al guardar el flujo proyectado.");
  } finally {
    if (btnGuardar) {
      btnGuardar.disabled = false;
      btnGuardar.textContent = oldText || "Guardar";
    }
  }
}


// 🔄 Guardar TODOS los años (usando el cacheValoresProyPorAnno)
async function guardarTodosLosAniosProy() {
  if (!proyectoSeleccionadoProy) {
    alert("Seleccione un proyecto primero.");
    return;
  }

  const { annoIni, annoFin } = proyectoSeleccionadoProy;
  if (!annoIni || !annoFin || annoIni > annoFin) {
    alert("Rango de años inválido para el proyecto.");
    return;
  }

  // 1️⃣ Actualizamos el cache del año que está visible ahora
  if (annoSeleccionadoProy != null) {
    cachearTablaProyParaAnno(annoSeleccionadoProy);
  }

  const btnGuardarTodos = document.getElementById("btnGuardarTodosProy");
  const oldText = btnGuardarTodos ? btnGuardarTodos.textContent : "";

  try {
    if (btnGuardarTodos) {
      btnGuardarTodos.disabled = true;
      btnGuardarTodos.textContent = "Guardando...";
    }

    setStatusProy("Guardando flujo proyectado de todos los años...");

    const filas = [];

    // 2️⃣ Para cada año usamos el snapshot almacenado en cacheValoresProyPorAnno
    for (let anio = annoIni; anio <= annoFin; anio++) {
      const filasAnno = construirFilasParaAnnoProyDesdeCache(anio);
      if (filasAnno && filasAnno.length) {
        filas.push(...filasAnno);
      }
    }

    if (!filas.length) {
      alert("No hay datos proyectados para guardar.");
      setStatusProy("No hay datos proyectados para guardar.");
      return;
    }

    // 3️⃣ Enviamos TODO al mismo endpoint que el botón Guardar
    const res = await fetch(`${API_BASE}/flujo-proyectado/guardar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(filas),
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || `HTTP ${res.status}`);
    }

    setStatusProy("Flujo proyectado de todos los años guardado correctamente.");
    alert("Flujo proyectado de todos los años guardado correctamente ✅");
  } catch (err) {
    console.error("Error al guardar todos los años proyectados:", err);
    setStatusProy("Error al guardar todos los años proyectados.");
    alert("Ocurrió un error al guardar todos los años proyectados.");
  } finally {
    if (btnGuardarTodos) {
      btnGuardarTodos.disabled = false;
      btnGuardarTodos.textContent = oldText || "Guardar todos";
    }
  }
}




// =========================
// INIT SOLO EN PANTALLA PROYECTADA
// =========================

function initProyectadoFlow() {
  const tieneTablaProy =
    document.getElementById("headerRowProy") &&
    document.getElementById("bodyRowsProy");

  if (!tieneTablaProy) return;

  crearHeaderTablaProy();
  agregarFilasBaseProy();
  setupEventListenersProy();
}

document.addEventListener("DOMContentLoaded", initProyectadoFlow);
