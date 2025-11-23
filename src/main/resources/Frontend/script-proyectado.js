const PROJECTS_BASE = "http://localhost:8080/api/proyectos";
const API_BASE      = "http://localhost:8080/api";

let proyectosProy = [];
let conceptosCargadosProy = { ingresos: [], egresos: [] };
let proyectoSeleccionadoProy = null;
let annoSeleccionadoProy = null;
let valoresProyActivo = false; 
let edicionProyActiva = false; 
let cacheValoresProyPorAnno = {};


const proyectoInfoProyEl = document.getElementById("proyectoInfoProy");
const codCiaProyHidden   = document.getElementById("codCiaProyHidden");
const codPytoProyHidden  = document.getElementById("codPytoProyHidden");
const statusMsgProyEl    = document.getElementById("statusMsgProy");

function setStatusProy(msg) {
  if (statusMsgProyEl) statusMsgProyEl.textContent = msg || "";
}

async function cargarCiasProy() {
  const selectCia = document.getElementById("selectCiaProy");
  if (!selectCia) return;

  try {
    selectCia.disabled = true;
    selectCia.innerHTML = '<option value="">Seleccione compañía</option>';

    const res = await fetch(`${API_BASE}/cias`, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(`Error al obtener compañías: ${res.status} ${txt}`);
    }

    const cias = await res.json();

    cias.forEach(c => {
      const opt = document.createElement("option");
      const cod = c.codCia ?? c.CODCIA ?? c.codcia;
      const desc =
        c.desCorta ?? c.DesCorta ?? c.descorta ??
        c.descia ?? c.DesCia ?? `CIA ${cod}`;

      opt.value = String(cod);
      opt.textContent = desc;
      selectCia.appendChild(opt);
    });

    selectCia.disabled = false;
  } catch (err) {
    console.error(err);
    alert("No se pudieron cargar las compañías (Proyectado): " + err.message);
    selectCia.disabled = true;
  }
}

function getVersionActualProy() {
  const sel = document.getElementById("versionSelect");
  const v = sel?.value?.trim();
  return v || "1";
}


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



function ordenarPorPartidaProy(a, b) {
  const ca = Number(a.codPartida || 0);
  const cb = Number(b.codPartida || 0);

  if (ca !== cb) return ca - cb;
  return (a.nivel || 0) - (b.nivel || 0);
}


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
  valoresProyActivo = false;

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

function limpiarValoresTablaProy() {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  tbody.querySelectorAll("tr.data-row-proy td[data-col-index], tr.separator-neto td[data-col-index]")
    .forEach(td => {
      td.textContent = formatNumber(0);
    });
}

async function cargarProyectosProy(codCia) {
  const select = document.getElementById("selectProyectoProy");
  if (!select) return;

  if (!codCia) {
    throw new Error("Debe seleccionar una compañía.");
  }

  const res = await fetch(`${PROJECTS_BASE}?codCia=${codCia}`, { mode: "cors" });
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


async function cargarConceptosProy(codPyto) {
  try {
    if (!proyectoSeleccionadoProy) {
      throw new Error("Seleccione primero un proyecto.");
    }

    const codCia = proyectoSeleccionadoProy.codCia;
    const ver = getVersionActualProy();

    const url = `${API_BASE}/proyectos/${codCia}/${codPyto}/${ver}/arbol-proyectado`;
    console.log("[PROY] FETCH árbol proyectado:", url);

    const res = await fetch(url, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(`HTTP ${res.status} ${txt}`);
    }

    const data = await res.json();
    console.log("[PROY] Árbol proyectado:", data);

    const arrI = data?.porIngEgr?.I ?? [];
    const arrE = data?.porIngEgr?.E ?? [];

    const ingresosRoots = arrI.map(e => mapEntradaPartidaProy(e, 1)).filter(Boolean);
    const egresosRoots  = arrE.map(e => mapEntradaPartidaProy(e, 1)).filter(Boolean);

    conceptosCargadosProy.ingresos = flattenTreeProy(ingresosRoots, []);
    conceptosCargadosProy.egresos  = flattenTreeProy(egresosRoots,  []);

    renderConceptosProy();
    setStatusProy("Árbol proyectado cargado.");
  } catch (err) {
    console.error("ERROR cargarConceptosProy:", err);
    alert("No se pudo cargar el árbol de partidas proyectadas: " + err.message);
  }
}




function crearFilaPartidaProy(partida) {
  const tr = document.createElement("tr");

  tr.classList.add("data-row-proy");
  if (partida.ingEgr) {
    tr.dataset.ingEgr = partida.ingEgr;   
  }

  const nivel = Number(partida.nivel ?? 1);
  const esHoja = !partida.children || partida.children.length === 0;
  tr.dataset.nivel = String(nivel);
  tr.dataset.esHoja = esHoja ? "1" : "0";

  const tdConcepto = document.createElement("td");
  tdConcepto.textContent = partida.desPartida;
  tdConcepto.dataset.codPartida = partida.codPartida;
  tdConcepto.classList.add("concepto-column");


  tdConcepto.style.paddingLeft = `${Math.max(0, nivel - 1) * 16}px`;

  tr.appendChild(tdConcepto);

  for (let i = 0; i < 15; i++) {
    const td = createNumberCell(0);
    td.dataset.codPartida = partida.codPartida;
    td.dataset.colIndex = i;

    if (i < 12) {
      td.dataset.mes = i + 1;
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
      td.dataset.mes = i + 1;  
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

    for (let i = 1; i <= 12 && i < tds.length; i++) {
      const td = tds[i];
      const txt = (td.textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      impMes.push(isNaN(num) ? 0 : num);
    }

    filas.push({
      anno: anio,
      codCia: proyectoSeleccionadoProy.codCia,
      codPyto: proyectoSeleccionadoProy.codPyto,
      ingEgr,
      codPartida,
      orden: orden++,
      tipo: "M",        
      desPartida,
      impMes   
    });
  });

  return filas;
}

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

  const data = await res.json();
  const filas = [];
  let orden = 1;

  (data || []).forEach(f => {
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

async function cargarValoresProyectadosDesdeBackend(anio, pintarEnTabla = true) {
  if (!proyectoSeleccionadoProy) {
    alert("Seleccione un proyecto.");
    return;
  }

  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  const { codCia, codPyto } = proyectoSeleccionadoProy;

  const url = `${API_BASE}/flujo-proyectado/valores?codCia=${codCia}&codPyto=${codPyto}&anno=${anio}`;
  console.log("[PROY] Cargando valores proyectados desde backend:", url);

  const res = await fetch(url, { mode: "cors" });
  if (!res.ok) {
    console.error("Error al cargar valores proyectados:", await res.text());
    alert("Error cargando valores.");
    return;
  }

  const data = await res.json();

  const filasCache = [];

  (data || []).forEach(f => {
    if (f.ingEgr !== "I" && f.ingEgr !== "E") return;

    const valores = [];

    for (let i = 0; i < 12; i++) {
      const v = f.valores?.mes?.[i] ?? 0;
      const num = typeof v === "number" ? v : parseFloat(v);
      valores.push(isNaN(num) ? 0 : num);
    }

    const suma    = f.valores?.suma    ?? 0;
    const acumAnt = f.valores?.acumAnt ?? 0;
    const total   = f.valores?.total   ?? 0;

    const nSuma    = typeof suma    === "number" ? suma    : parseFloat(suma)    || 0;
    const nAcumAnt = typeof acumAnt === "number" ? acumAnt : parseFloat(acumAnt) || 0;
    const nTotal   = typeof total   === "number" ? total   : parseFloat(total)   || 0;

    valores.push(nSuma, nAcumAnt, nTotal);

    filasCache.push({
      codPartida: f.codPartida,
      ingEgr: f.ingEgr,
      valores
    });
  });

  cacheValoresProyPorAnno[anio] = filasCache;

  if (!pintarEnTabla) {
    return;
  }

  limpiarValoresTablaProy();

  filasCache.forEach(reg => {
    const selector = `tr.data-row-proy[data-ing-egr="${reg.ingEgr}"] td.concepto-column[data-cod-partida="${reg.codPartida}"]`;
    const cell = tbody.querySelector(selector);
    if (!cell) return;

    const tr = cell.parentElement;
    const tds = tr.querySelectorAll("td");


    for (let i = 0; i < 12; i++) {
      tds[i + 1].textContent = formatNumber(reg.valores[i] || 0);
    }

    tds[13].textContent = formatNumber(reg.valores[12] || 0);
    tds[14].textContent = formatNumber(reg.valores[13] || 0);
    tds[15].textContent = formatNumber(reg.valores[14] || 0);
  });

  recalcularFilaNetoProy();

  setStatusProy(`Datos proyectados cargados desde BD para ${anio}.`);
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

    for (let i = 1; i < tds.length && i <= 15; i++) {
      const txt = (tds[i].textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      valores.push(isNaN(num) ? 0 : num);
    }

    filas.push({ codPartida, ingEgr, valores });
  });

  cacheValoresProyPorAnno[anio] = filas;
}
function copiarAcumuladoAnteriorDesdeAnno(anioAnterior, anioActual) {
  const filasPrev = cacheValoresProyPorAnno[anioAnterior];
  if (!filasPrev) return;

  const tbody = document.getElementById("bodyRowsProy");

  filasPrev.forEach(reg => {
    const selector = `tr.data-row-proy[data-ing-egr="${reg.ingEgr}"] td.concepto-column[data-cod-partida="${reg.codPartida}"]`;
    const conceptCell = tbody.querySelector(selector);
    if (!conceptCell) return;

    const tr = conceptCell.parentElement;
    const tds = tr.querySelectorAll("td");


    const totalPrev = reg.valores[14] || 0;


    tds[14].textContent = formatNumber(totalPrev);


    const sumaActual = parseFloat((tds[13].textContent || "0").replace(/,/g, ""));
    tds[15].textContent = formatNumber(sumaActual + totalPrev);
  });
}


function restaurarTablaProyDesdeCache(anio) {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;
  if (!proyectoSeleccionadoProy) return;

  const filas = cacheValoresProyPorAnno[anio];

  if (!filas) {
    const celdas = tbody.querySelectorAll("tr.data-row-proy td:not(.concepto-column)");
    celdas.forEach(td => {
      td.textContent = formatNumber(0);
    });

    recalcularFilaNetoProy();
    return;
  }

  filas.forEach(f => {
    const selector = `tr.data-row-proy[data-ing-egr="${f.ingEgr}"] td.concepto-column[data-cod-partida="${f.codPartida}"]`;
    const conceptCell = tbody.querySelector(selector);
    if (!conceptCell) return;

    const tr = conceptCell.parentElement;
    const tds = tr.querySelectorAll("td");

    f.valores.forEach((v, idx) => {
      const pos = idx + 1; 
      if (pos < tds.length) {
        tds[pos].textContent = formatNumber(v);
      }
    });
  });

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

    const impMes = [];
    for (let i = 0; i < 12; i++) {
      const v = valores && valores[i] != null ? valores[i] : 0;
      const num = parseFloat(String(v).replace(/,/g, "").trim());
      impMes.push(isNaN(num) ? 0 : num);
    }

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
      desPartida: "", 
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
      nivelHijo = nivelPadre;
    }
  }
}

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
    edicionProyActiva = false;
    valoresProyActivo = false;

    // dejar de ser editables las celdas verdes (pero SIN cambiar valores)
    tbody.querySelectorAll("td.editable-mes-proy").forEach(td => {
      td.contentEditable = "false";
      td.classList.remove("proy-editable-activa");
    });

    // quitamos el listener de blur
    tbody.removeEventListener("blur", onBlurCeldaProy, true);

    setStatusProy("Edición de valores proyectados desactivada.");
    return;
  }

  edicionProyActiva = true;
  valoresProyActivo = true;

  tbody.querySelectorAll("td.editable-mes-proy").forEach(td => {
    td.contentEditable = "true";
    td.classList.add("proy-editable-activa");
  });

  // escuchamos blur para formatear y recalcular
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


function setupEventListenersProy() {
  const selectCiaProy   = document.getElementById("selectCiaProy");
  const selectProyecto  = document.getElementById("selectProyectoProy");
  if (!selectProyecto || !selectCiaProy) return; // no estamos en pantalla proyectada

  const btnProyectos        = document.getElementById("btnProyectosProy");
  const yearSelect          = document.getElementById("yearSelectProy");
  const fechaInicio         = document.getElementById("fechaInicioProy");
  const fechaFin            = document.getElementById("fechaFinProy");
  const btnConceptoProy     = document.getElementById("btnConceptoProy");
  const btnValoresProy      = document.getElementById("btnValoresProy");
  const btnGuardarProy      = document.getElementById("btnGuardarProy");
  const btnGuardarTodosProy = document.getElementById("btnGuardarTodosProy");
  selectProyecto.disabled = true;

  // CAMBIO DE COMPAÑÍA
  selectCiaProy.addEventListener("change", (e) => {
    const codCia = parseInt(e.target.value, 10) || 0;

    // reset de proyectos y tabla
    selectProyecto.innerHTML = '<option value="">Seleccione proyecto</option>';
    selectProyecto.disabled = !codCia;

    resetTablaProy(true);
    cacheValoresProyPorAnno = {};

    if (codCiaProyHidden)  codCiaProyHidden.value  = codCia ? String(codCia) : "";
    if (codPytoProyHidden) codPytoProyHidden.value = "";
    if (proyectoInfoProyEl) proyectoInfoProyEl.textContent = "";

    if (btnProyectos) btnProyectos.classList.add("btn-off");

    setStatusProy(
      codCia
        ? "Presione 'Proyectos' para cargar los proyectos de la compañía seleccionada."
        : "Seleccione una compañía."
    );
  });

  // BOTÓN PROYECTOS
  if (btnProyectos) {
    btnProyectos.addEventListener("click", async (ev) => {
      const btn = ev.currentTarget;

      if (!btn.classList.contains("btn-off")) return;

      btn.disabled = true;
      const oldText = btn.textContent;
      btn.textContent = "Cargando...";

           try {
        const codCia = parseInt(selectCiaProy.value, 10) || 0;
        if (!codCia) {
          alert("Seleccione primero una compañía.");
          btn.textContent = oldText;
          return;
        }

        await cargarProyectosProy(codCia);
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

  // CAMBIO DE PROYECTO
  selectProyecto.addEventListener("change", (e) => {
    const value = e.target.value;

    if (!value) {
      resetTablaProy(true);
      return;
    }

    resetTablaProy(false);
    valoresProyActivo = false;
    edicionProyActiva = false;
    cacheValoresProyPorAnno = {};

    const optSel       = e.target.options[e.target.selectedIndex];
    const codPyto      = parseInt(value, 10);
    const codCia       = parseInt(selectCiaProy.value, 10) || 0;
    const annoIni      = parseInt(optSel.dataset.annoIni, 10);
    const annoFin      = parseInt(optSel.dataset.annoFin, 10);

    if (!codCia) {
      alert("Seleccione una compañía antes de elegir proyecto.");
      resetTablaProy(true);
      return;
    }


    proyectoSeleccionadoProy = { codCia, codPyto, annoIni, annoFin };

    if (codCiaProyHidden)  codCiaProyHidden.value  = codCia;
    if (codPytoProyHidden) codPytoProyHidden.value = codPyto;
    if (proyectoInfoProyEl) {
      proyectoInfoProyEl.textContent = `Proyecto: ${optSel.textContent}`;
    }

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

// CAMBIO DE AÑO CON CACHE
if (yearSelect) {
  yearSelect.addEventListener("change", (e) => {
    if (!proyectoSeleccionadoProy) return;

    const nuevoAnno = parseInt(e.target.value, 10);
    if (!nuevoAnno) return;

    if (annoSeleccionadoProy != null) {
      cachearTablaProyParaAnno(annoSeleccionadoProy);
    }

    annoSeleccionadoProy = nuevoAnno;
    setStatusProy(
      `Año ${annoSeleccionadoProy} seleccionado. Use "Cargar datos" si desea traer valores guardados de BD.`
    );

    const tbody = document.getElementById("bodyRowsProy");
    if (!tbody) return;

    if (cacheValoresProyPorAnno[nuevoAnno]) {
      restaurarTablaProyDesdeCache(nuevoAnno);
    } else {
      limpiarValoresTablaProy();
    }

    if (cacheValoresProyPorAnno[nuevoAnno - 1]) {
      copiarAcumuladoAnteriorDesdeAnno(nuevoAnno - 1, nuevoAnno);
      cachearTablaProyParaAnno(nuevoAnno);
    }
    recalcularFilaNetoProy();
  });
}


  // BOTÓN CONCEPTO
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


  // BOTÓN VALORES activar edición

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

  // BOTONES GUARDAR

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

  const btnCargarDatosProy = document.getElementById("btnCargarDatosProy");
  if (btnCargarDatosProy) {
    btnCargarDatosProy.addEventListener("click", async () => {
      if (!proyectoSeleccionadoProy || !annoSeleccionadoProy) {
        alert("Seleccione proyecto y año.");
        return;
      }

      const { annoIni, annoFin } = proyectoSeleccionadoProy;
      if (!annoIni || !annoFin || annoIni > annoFin) {
        alert("Rango de años inválido para el proyecto.");
        return;
      }

      // Cargamos TODOS los años desde BD
      for (let anio = annoIni; anio <= annoFin; anio++) {
        const pintar = (anio === annoSeleccionadoProy); // solo pintamos el año actual
        await cargarValoresProyectadosDesdeBackend(anio, pintar);
      }

      setStatusProy(
        `Datos proyectados cargados desde BD para todos los años (${annoIni}-${annoFin}).`
      );
    });
  }


}

const btnInicioProy = document.getElementById("btnInicioProy");
  if (btnInicioProy) {
    btnInicioProy.addEventListener('click', () => {
        window.location.href = '/'; // Redirige a la página principal
    });
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


// INIT SOLO EN PANTALLA PROYECTADA
async function initProyectadoFlow() {
  const tieneTablaProy =
    document.getElementById("headerRowProy") &&
    document.getElementById("bodyRowsProy");

  if (!tieneTablaProy) return;

  crearHeaderTablaProy();
  agregarFilasBaseProy();
  await cargarCiasProy(); 
  setupEventListenersProy();
}

document.addEventListener("DOMContentLoaded", initProyectadoFlow);

