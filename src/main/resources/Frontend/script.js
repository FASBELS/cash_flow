// Frontend/script.js
// Asume Live Server en http://127.0.0.1:5501
// Backend en http://localhost:8080

// Bases de API
const PROJECTS_BASE = "http://localhost:8080/api/proyectos";
const API_BASE      = "http://localhost:8080/api";

let proyectos = [];
let conceptosCargados = { ingresos: [], egresos: [] };
let proyectoSeleccionado = null;
let annoSeleccionado = null;

// Mapa para comparar contra flujo proyectado
let mapaProyectado = {};
let comparacionActiva = false; // toggle del botón

// --- referencias de la UI ---
const proyectoInfoEl = document.getElementById("proyectoInfo");
const codCiaHidden   = document.getElementById("codCiaHidden");
const codPytoHidden  = document.getElementById("codPytoHidden");
const statusMsgEl    = document.getElementById("statusMsg");

function setStatus(msg) {
  if (statusMsgEl) statusMsgEl.textContent = msg || "";
}

// ======== Helpers para Árbol de Partidas ========
const MESES = ["ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic"];

/** Lee versión si existe un <select id="versionSelect">; si no, usa "1" */
function getVersionActual() {
  const sel = document.getElementById("versionSelect");
  const v = sel?.value?.trim();
  return v || "1";
}

/**
 * Construye el árbol desde un array plano:
 * - Ruta A (preferida): usa CodPartidaPadre / PadCodPartida si viene del backend.
 * - Ruta B (fallback): si no hay padre, usa pila por Nivel/Orden.
 *   (Esta ruta ahora es solo de respaldo, el caso principal es porIngEgr.)
 */
function ensureTree(data) {
  if (!Array.isArray(data)) return [];

  const rows = data.map(it => ({
    codPartida: it.CodPartida ?? it.codPartida,
    codPadre:   it.PadCodPartida ?? it.padCodPartida ??
                it.CodPartidaPadre ?? it.codPartidaPadre ??
                it.Padre ?? it.padre ?? null,
    ingEgr:     (it.IngEgr ?? it.ingEgr ?? "").toUpperCase(),
    desPartida: it.DesPartida ?? it.desPartida ?? "",
    nivel:      Number(it.Nivel ?? it.nivel ?? 1),
    orden:      Number(it.Orden ?? it.orden ?? 0),
  }));

  const hayPadres = rows.some(r => r.codPadre != null && r.codPadre !== "");

  if (hayPadres) {
    const map = new Map(rows.map(r => [r.codPartida, { ...r, children: [] }]));
    const roots = [];
    rows.forEach(r => {
      const nodo = map.get(r.codPartida);
      const padre = r.codPadre ? map.get(r.codPadre) : null;
      if (!padre) {
        roots.push(nodo);
      } else {
        if (!padre.ingEgr || !nodo.ingEgr || padre.ingEgr === nodo.ingEgr) {
          padre.children.push(nodo);
        } else {
          // defensa si mezclan I/E
          roots.push(nodo);
        }
      }
    });
    const setNivel = (n, l = 1) => {
      n.nivel = l;
      (n.children || []).forEach(c => setNivel(c, l + 1));
    };
    roots.forEach(r => setNivel(r, 1));
    const sortTree = (n) => {
      if (n.children?.length) {
        n.children.sort((a, b) => (a.orden || 0) - (b.orden || 0));
        n.children.forEach(sortTree);
      }
    };
    roots.sort((a, b) => (a.orden || 0) - (b.orden || 0));
    roots.forEach(sortTree);
    return roots;
  }

  // fallback por pila si no hay campo padre
  const flat = rows.slice().sort((a,b)=>(a.orden||0)-(b.orden||0));
  const root = [], stack = [];
  flat.forEach(item => {
    const nodo = { ...item, children: [] };
    while (stack.length && (stack[stack.length-1].nivel >= nodo.nivel)) stack.pop();
    (stack.length ? stack[stack.length-1].children : root).push(nodo);
    stack.push(nodo);
  });
  return root;
}

/** Mapea una entrada del formato { partida:{...}, hijos:[...] } al nodo usado en el front */
function mapEntradaPartida(entry, nivelBase = 1) {
  if (!entry) return null;

  // puede venir como {partida:{...}, hijos:[...]} o ya ser el objeto partida
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
      .map(h => mapEntradaPartida(h, (nodo.nivel || 1) + 1))
      .filter(Boolean);
  }

  return nodo;
}

/** Recorrido preorden para aplanar el árbol con info de nivel */
function flattenTree(tree, out = []) {
  const visit = (n) => {
    out.push(n);
    (n.children || []).forEach(ch => visit(ch));
  };
  (tree || []).forEach(visit);
  return out;
}

/** Divide raíces por tipo I/E (solo para el caso genérico plano) */
function splitByTipo(tree) {
  return {
    ingresos: (tree || []).filter(n => (n.ingEgr || "").toUpperCase() === "I"),
    egresos:  (tree || []).filter(n => (n.ingEgr || "").toUpperCase() === "E"),
  };
}

// (esta primera init queda sobrescrita por la de abajo, pero la mantengo intacta)
async function init() {
  crearHeaderTabla();
  agregarFilasBase();
  setupEventListeners();
}

function crearHeaderTabla() {
  const headerRow = document.getElementById("headerRow");
  headerRow.innerHTML = "";
  const cols = [
    "Concepto","Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio",
    "Agosto","Septiembre","Octubre","Noviembre","Diciembre","Suma","Acum. Ant.","Total",
  ];
  cols.forEach((c) => {
    const th = document.createElement("th");
    th.textContent = c;
    headerRow.appendChild(th);
  });
}

function agregarFilasBase() {
  const tbody = document.getElementById("bodyRows");
  tbody.innerHTML = "";

  // INGRESOS
  const trIngHeader = document.createElement("tr");
  trIngHeader.classList.add("separator-row");
  const tdIng = document.createElement("td");
  tdIng.colSpan = 16;
  tdIng.textContent = "INGRESOS";
  trIngHeader.appendChild(tdIng);
  tbody.appendChild(trIngHeader);

  // EGRESOS
  const trEgrHeader = document.createElement("tr");
  trEgrHeader.classList.add("separator-row");
  const tdEgr = document.createElement("td");
  tdEgr.colSpan = 16;
  tdEgr.textContent = "EGRESOS";
  trEgrHeader.appendChild(tdEgr);
  tbody.appendChild(trEgrHeader);

  // NETO
  const trNet = document.createElement("tr");
  trNet.classList.add("separator-neto");
  const tdNet = document.createElement("td");
  tdNet.textContent = "FLUJO DE CAJA NETO";
  trNet.appendChild(tdNet);
  for (let i = 0; i < 15; i++) {
    const td = document.createElement("td");
    td.textContent = "0";
    trNet.appendChild(td);
  }
  tbody.appendChild(trNet);
}

// 🔧 acepta flag para decidir si borra selección del proyecto
function resetTabla(resetProyecto = false) {
  conceptosCargados = { ingresos: [], egresos: [] };
  proyectoSeleccionado = null;
  annoSeleccionado = null;
  mapaProyectado = {};
  comparacionActiva = false;

  if (resetProyecto) {
    document.getElementById("selectProyecto").value = "";
    const yd = document.getElementById("yearDisplay"); // opcional
    if (yd) yd.textContent = "";
    if (proyectoInfoEl) proyectoInfoEl.textContent = "";
    if (codCiaHidden) codCiaHidden.value = "";
    if (codPytoHidden) codPytoHidden.value = "";
    setStatus("");
  }

  // 🆕 al resetear tabla, también reseteamos comparación visual
  resetComparacionVisual();

  agregarFilasBase();
}

async function cargarProyectos() {
  try {
    const res = await fetch(PROJECTS_BASE);
    if (!res.ok) throw new Error("Error al obtener proyectos");
    proyectos = await res.json();

    const select = document.getElementById("selectProyecto");
    select.innerHTML = '<option value="">-- Seleccione proyecto --</option>';

    proyectos.forEach((p) => {
      const opt = document.createElement("option");
      opt.value = String(p.codPyto);
      opt.textContent = p.nombPyto;
      opt.dataset.annoIni = p.annoIni;
      opt.dataset.annoFin = p.annoFin;
      select.appendChild(opt);
    });
    select.disabled = false;
  } catch (err) {
    console.error(err);
    alert("No se pudieron cargar los proyectos: " + err.message);
    document.getElementById("selectProyecto").disabled = true;
    throw err;
  }
}

function setupEventListeners() {
  const selectProyecto = document.getElementById("selectProyecto");
  if (!selectProyecto) {
    // No es pantalla de flujo real
    return;
  }

  const btnProyectos = document.getElementById("btnProyectos");
  if (btnProyectos) {
    btnProyectos.addEventListener("click", async (ev) => {
      const btn = ev.currentTarget;
      const select = document.getElementById("selectProyecto");

      if (!btn.classList.contains("btn-off")) {
        console.log("Proyectos ya cargados.");
        return;
      }

      btn.disabled = true;
      btn.textContent = "Cargando...";

      try {
        await cargarProyectos();
        btn.classList.remove("btn-off");
        btn.textContent = "Proyectos";
        select.focus();
        setStatus("Proyectos cargados. Seleccione uno.");
      } catch (err) {
        btn.textContent = "Proyectos";
        alert("No se pudieron cargar los proyectos.");
      } finally {
        btn.disabled = false;
      }
    });
  }

  document.getElementById("selectProyecto").addEventListener("change", (e) => {
    const yearSelect  = document.getElementById("yearSelect");
    const fechaInicio = document.getElementById("fechaInicio");
    const fechaFin    = document.getElementById("fechaFin");
    const yearDisplay = document.getElementById("yearDisplay"); // puede no existir

    if (!e.target.value) {
      resetTabla(true);
      yearSelect.innerHTML = '<option value="">Año</option>';
      fechaInicio.value = "";
      fechaFin.value = "";
      if (yearDisplay) yearDisplay.textContent = "--";
      return;
    }

    resetTabla(false);

    const codPyto = parseInt(e.target.value, 10);
    const codCia  = 1;

    const optSel  = e.target.options[e.target.selectedIndex];
    const annoIni = parseInt(optSel.dataset.annoIni, 10);
    const annoFin = parseInt(optSel.dataset.annoFin, 10);

    proyectoSeleccionado = { codCia, codPyto, annoIni, annoFin };

    if (codCiaHidden)  codCiaHidden.value  = proyectoSeleccionado.codCia;
    if (codPytoHidden) codPytoHidden.value = proyectoSeleccionado.codPyto;
    if (proyectoInfoEl) {
      proyectoInfoEl.textContent = `Proyecto: ${optSel.textContent}`;
    }

    const yearSelectEl = document.getElementById("yearSelect");
    yearSelectEl.innerHTML = "";
    for (let y = annoIni; y <= annoFin; y++) {
      const opt = document.createElement("option");
      opt.value = y;
      opt.textContent = y;
      yearSelectEl.appendChild(opt);
    }

    annoSeleccionado = annoIni;
    yearSelectEl.value = annoSeleccionado;
    if (yearDisplay) yearDisplay.textContent = String(annoSeleccionado);

    fechaInicio.value = `${annoIni}-01-01`;
    fechaFin.value    = `${annoFin}-12-31`;
    setStatus("Proyecto cargado.");
  });

  document.getElementById("btnConcepto").addEventListener("click", async (ev) => {
    if (!proyectoSeleccionado) {
      alert("Seleccione primero un proyecto");
      return;
    }
    const btn = ev.currentTarget;
    btn.disabled = true;
    btn.textContent = "Cargando...";
    try {
      await cargarConceptos(proyectoSeleccionado.codPyto);
    } finally {
      btn.disabled = false;
      btn.textContent = "Concepto";
    }
  });

  const btnPrev = document.getElementById("btnYearPrev");
  if (btnPrev) {
      btnPrev.addEventListener("click", () => {
      if (!proyectoSeleccionado) return;
      if (annoSeleccionado > proyectoSeleccionado.annoIni) {
      annoSeleccionado--;
      const yearDisplay = document.getElementById("yearDisplay");
      if (yearDisplay) yearDisplay.textContent = String(annoSeleccionado);

    // solo limpiamos la tabla y colores
    resetCeldasNumericas();
    resetComparacionVisual();
  }
});

  }

  const btnNext = document.getElementById("btnYearNext");
  if (btnNext) {
    btnNext.addEventListener("click", () => {
    if (!proyectoSeleccionado) return;
    if (annoSeleccionado < proyectoSeleccionado.annoFin) {
      annoSeleccionado++;
      const yearDisplay = document.getElementById("yearDisplay");
      if (yearDisplay) yearDisplay.textContent = String(annoSeleccionado);

    // solo limpiamos la tabla y colores
    resetCeldasNumericas();
    resetComparacionVisual();
  }
});

  }

  const btnValores = document.getElementById("btnValores");
  if (btnValores) {
    btnValores.addEventListener("click", async () => {
      if (!proyectoSeleccionado || !annoSeleccionado) {
        alert("Seleccione proyecto y año.");
        return;
      }
      btnValores.disabled = true;
      const old = btnValores.textContent;
      btnValores.textContent = "Calculando...";
      try {
        await cargarValoresReales(
          proyectoSeleccionado.codCia,
          proyectoSeleccionado.codPyto,
          annoSeleccionado
        );
        setStatus(`Valores reales ${annoSeleccionado} cargados.`);
      } catch (e) {
        console.error(e);
        alert("No se pudieron cargar los valores: " + e.message);
      } finally {
        btnValores.textContent = old;
        btnValores.disabled = false;
      }
    });
  }

  const yearSelectEl2 = document.getElementById("yearSelect");
  if (yearSelectEl2) {
    yearSelectEl2.addEventListener("change", e => {
    if (!proyectoSeleccionado) return;
    annoSeleccionado = parseInt(e.target.value, 10);
    const yd = document.getElementById("yearDisplay");
    if (yd) yd.textContent = String(annoSeleccionado);

  // al cambiar de año, tabla limpia hasta que tú pulses "Valores"
  resetCeldasNumericas();
  resetComparacionVisual();
});

  }

  // Botón Guardar
  const btnGuardar = document.getElementById("btnGuardar");
  if (btnGuardar) {
    btnGuardar.addEventListener("click", guardarFlujoReal);
  }

  // Botón Guardar todos los años
  const btnGuardarTodos = document.getElementById("btnGuardarTodos");
  if (btnGuardarTodos) {
    btnGuardarTodos.addEventListener("click", guardarTodosLosAnios);
  }

  // 🆕 Botón Ver diferencias con el proyectado (toggle)
  const btnComparar = document.getElementById("btnComparar");
  if (btnComparar) {
    btnComparar.addEventListener("click", async () => {
      if (!proyectoSeleccionado || !annoSeleccionado) {
        alert("Seleccione proyecto y año primero.");
        return;
      }
      if (!conceptosCargados.ingresos.length && !conceptosCargados.egresos.length) {
        alert("Primero cargue los conceptos.");
        return;
      }

      // Activar comparación
      if (!comparacionActiva) {
        btnComparar.disabled = true;
        const old = btnComparar.textContent;
        btnComparar.textContent = "Comparando...";

        try {
          await cargarMapaProyectado(
            proyectoSeleccionado.codCia,
            proyectoSeleccionado.codPyto,
            annoSeleccionado
          );

          if (!mapaProyectado || Object.keys(mapaProyectado).length === 0) {
            alert("No se encontraron valores proyectados para este proyecto y año.");
            btnComparar.textContent = old;
          } else {
            aplicarComparacionCeldas();
            comparacionActiva = true;
            btnComparar.textContent = "Ocultar diferencias";
          }
        } catch (e) {
          console.error("Error al comparar con proyectado:", e);
          alert("Error al comparar con el proyectado.");
          btnComparar.textContent = old;
        } finally {
          btnComparar.disabled = false;
        }

      // Desactivar comparación
      } else {
        limpiarComparacionCeldas();
        comparacionActiva = false;
        btnComparar.textContent = "Ver diferencias con el proyectado";
      }
    });
  }

  const btnInicio = document.getElementById("btnInicio");
  if (btnInicio) {
    btnInicio.addEventListener('click', () => {
        window.location.href = '/'; // Redirige a la página principal
    });
  }
  
}

// ========= MODIFICADO: cargar conceptos desde /proyectos/{cia}/{pyto}/{ver}/arbol =========
async function cargarConceptos(codPyto) {
  try {
    if (!proyectoSeleccionado) throw new Error("Seleccione primero un proyecto.");
    const codCia = proyectoSeleccionado.codCia;
    const ver = getVersionActual();

    const url = `${API_BASE}/proyectos/${codCia}/${codPyto}/${ver}/arbol`;
    console.log("FETCH árbol:", url);

    const res = await fetch(url);
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(`HTTP ${res.status} ${txt}`);
    }

    const data = await res.json();
    console.log("Árbol crudo:", data);

    const arrI = data?.porIngEgr?.I ?? [];
    const arrE = data?.porIngEgr?.E ?? [];

    const ingresosRoots = arrI.map(e => mapEntradaPartida(e, 1));
    const egresosRoots  = arrE.map(e => mapEntradaPartida(e, 1));

    // Aplanar preorden
    conceptosCargados.ingresos = flattenTree(ingresosRoots);
    conceptosCargados.egresos  = flattenTree(egresosRoots);

    // Pintar
    renderArbolEnTabla();
    setStatus("Árbol de partidas cargado.");

    /*
    if (annoSeleccionado) {
      await cargarValoresReales(
        proyectoSeleccionado.codCia,
        proyectoSeleccionado.codPyto,
        annoSeleccionado
      );
    }
      */
     
  } catch (err) {
    console.error("ERROR cargarConceptos:", err);
    alert("No se pudo cargar conceptos: " + err.message);
  }
}


// ========= NUEVO: render del árbol en tu misma tabla =========
function renderArbolEnTabla() {
  const tbody = document.getElementById("bodyRows");
  tbody.innerHTML = "";

  // Header INGRESOS
  const trIngHeader = document.createElement("tr");
  trIngHeader.classList.add("separator-row");
  const tdIng = document.createElement("td");
  tdIng.colSpan = 16;
  tdIng.textContent = "INGRESOS";
  trIngHeader.appendChild(tdIng);
  tbody.appendChild(trIngHeader);

  conceptosCargados.ingresos.forEach(n => tbody.appendChild(crearFilaNodo(n)));

  // Header EGRESOS
  const trEgrHeader = document.createElement("tr");
  trEgrHeader.classList.add("separator-row");
  const tdEgr = document.createElement("td");
  tdEgr.colSpan = 16;
  tdEgr.textContent = "EGRESOS";
  trEgrHeader.appendChild(tdEgr);
  tbody.appendChild(trEgrHeader);

  conceptosCargados.egresos.forEach(n => tbody.appendChild(crearFilaNodo(n)));

  // NETO
  const trNet = document.createElement("tr");
  trNet.classList.add("separator-neto");
  const tdNet = document.createElement("td");
  tdNet.textContent = "FLUJO DE CAJA NETO";
  trNet.appendChild(tdNet);
  for (let i = 0; i < 15; i++) {
    const td = document.createElement("td");
    td.textContent = "0.00";
    trNet.appendChild(td);
  }
  tbody.appendChild(trNet);

  // Mensaje vacío
  if (!conceptosCargados.ingresos.length && !conceptosCargados.egresos.length) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 16;
    td.textContent = "Sin conceptos para este proyecto.";
    tr.appendChild(td);
    tbody.appendChild(tr);
  }
  resetCeldasNumericas();

}

function crearFilaNodo(nodo) {
  const tr = document.createElement("tr");
  tr.classList.add("data-row");
  if (nodo.ingEgr) tr.dataset.ingEgr = nodo.ingEgr;

  const tdConcepto = document.createElement("td");
  tdConcepto.classList.add("concepto-column");
  tdConcepto.dataset.codPartida = nodo.codPartida;

  // Sangría por nivel
  const nivel = Number(nodo.nivel ?? 1);
  tdConcepto.style.paddingLeft = `${Math.max(0, nivel - 1) * 16}px`;

  tdConcepto.textContent = nodo.desPartida || nodo.nombre || "";
  tr.appendChild(tdConcepto);

  // 12 meses + Suma + Acum Ant + Total (compatibles con pintarValores / comparar)
  for (let i = 0; i < 15; i++) {
    const td = document.createElement("td");
    td.textContent = "0.00";
    td.dataset.codPartida = nodo.codPartida;
    td.dataset.colIndex = i;
    if (i < 12) td.dataset.mes = i + 1;
    tr.appendChild(td);
  }
  return tr;
}

// === CARGAR PROYECTADO PARA COMPARAR ===
// Usa FlujoProyectadoController: /api/flujo-proyectado/valores
async function cargarMapaProyectado(codCia, codPyto, anno) {
  mapaProyectado = {};

  const url = `${API_BASE}/flujo-proyectado/valores?codCia=${codCia}&codPyto=${codPyto}&anno=${anno}`;
  console.log("[COMPARE] Fetch proyectado:", url);

  const res = await fetch(url, { mode: "cors" });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    console.warn("[COMPARE] No se pudo cargar flujo proyectado:", txt || `HTTP ${res.status}`);
    return;
  }

  const data = await res.json();
  console.log("[COMPARE] Proyectado crudo:", data);

  const nuevoMapa = {};

  data.forEach(row => {
    if (!row || !row.codPartida) return;

    const codPartida = Number(row.codPartida);
    if (!codPartida) return;

    const meses = row.valores && Array.isArray(row.valores.mes)
      ? row.valores.mes
      : [];

    for (let i = 0; i < 12; i++) {
      const mes = i + 1;
      const v = Number(meses[i] || 0);
      if (!isNaN(v)) {
        const key = `${codPartida}_${mes}`;
        nuevoMapa[key] = (nuevoMapa[key] || 0) + v;
      }
    }
  });

  mapaProyectado = nuevoMapa;
  console.log("[COMPARE] mapaProyectado:", mapaProyectado);
}

// === APLICAR / LIMPIAR COLORES POR COMPARACIÓN ===
function actualizarEstiloCeldaComparacion(celdaEl, codPartida, mes) {
  if (!celdaEl || !codPartida || !mes) return;

  // limpiar estilo previo (incluyendo posibles !important)
  celdaEl.style.removeProperty("color");
  celdaEl.style.fontWeight = "";

  const key = `${codPartida}_${mes}`;
  if (!(key in mapaProyectado)) return;

  const valorProy = Number(mapaProyectado[key]) || 0;
  const txt = (celdaEl.textContent || "").replace(/,/g, "").trim();
  const valorReal = Number(txt || 0);
  if (!isFinite(valorReal)) return;

  if (valorReal > valorProy) {
    celdaEl.style.setProperty("color", "red", "important");
    celdaEl.style.fontWeight = "600";
  } else if (valorReal < valorProy) {
    celdaEl.style.setProperty("color", "green", "important");
    celdaEl.style.fontWeight = "600";
  }
  // igual: sin cambios
}

function aplicarComparacionCeldas() {
  if (!mapaProyectado || Object.keys(mapaProyectado).length === 0) {
    console.log("[COMPARE] Sin datos proyectados -> no se colorea.");
    return;
  }

  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;

  const rows = tbody.querySelectorAll("tr.data-row");
  rows.forEach(tr => {
    const conceptoCell = tr.querySelector("td.concepto-column");
    if (!conceptoCell) return;

    const codPartida = parseInt(conceptoCell.dataset.codPartida, 10);
    if (!codPartida) return;

    const celdasMes = tr.querySelectorAll("td[data-mes]");
    celdasMes.forEach(td => {
      const mes = parseInt(td.dataset.mes, 10);
      actualizarEstiloCeldaComparacion(td, codPartida, mes);
    });
  });
}

function limpiarComparacionCeldas() {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;
  const celdas = tbody.querySelectorAll("tr.data-row td[data-mes]");
  celdas.forEach(td => {
    td.style.removeProperty("color");
    td.style.fontWeight = "";
  });
}

// 🆕 Helper: resetear estado visual de comparación (texto + colores + mapa)
function resetComparacionVisual() {
  const btnComparar = document.getElementById("btnComparar");
  comparacionActiva = false;
  mapaProyectado = {};
  if (btnComparar) {
    btnComparar.textContent = "Ver diferencias con el proyectado";
  }
  limpiarComparacionCeldas();
}

// === cargar valores reales para un año y pintar la tabla actual ===
async function cargarValoresReales(codCia, codPyto, anno) {
  if (!conceptosCargados.ingresos.length && !conceptosCargados.egresos.length) {
    setStatus("Primero carga los conceptos (botón 'Concepto').");
    return;
  }

  const url = `${API_BASE}/valores/real?codCia=${codCia}&codPyto=${codPyto}&anno=${anno}`;
  const res = await fetch(url, { mode: "cors" });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} ${res.statusText} ${txt}`);
  }
  const data = await res.json();

  resetCeldasNumericas();
  // 🆕 cada vez que cambio de año/valores, apago la comparación y dejo el botón coherente
  resetComparacionVisual();

  data.forEach(row => {
    if (row.ingEgr === "N" || row.codPartida === 0) {
      pintarFilaNeto(row.valores);
    } else {
      pintarFilaPartidaValores(row.codPartida, row.valores);
    }
  });

  // Las diferencias solo se muestran cuando el usuario presiona el botón
}

function resetCeldasNumericas() {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;
  tbody.querySelectorAll("tr").forEach(tr => {
    const tds = Array.from(tr.querySelectorAll("td"));
    if (tds.length >= 16) {
      for (let i = 1; i < tds.length; i++) {
        const td = tds[i];
        td.textContent = "0.00";
        td.style.removeProperty("color");
        td.style.fontWeight = "";
      }
    }
  });
}

function pintarFilaPartidaValores(codPartida, valores) {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;

  const conceptCell = tbody.querySelector(
    `tr > td:first-child[data-cod-partida="${codPartida}"]`
  );
  if (!conceptCell) return;

  const tr = conceptCell.parentElement;
  const celdas = Array.from(tr.querySelectorAll("td"));
  if (celdas.length < 16) return;

  for (let i = 0; i < 12; i++) {
    const v = (valores.mes?.[i] ?? 0);
    celdas[i + 1].textContent = formatNumber(v);
  }
  celdas[13].textContent = formatNumber(valores.suma ?? 0);
  celdas[14].textContent = formatNumber(valores.acumAnt ?? 0);
  celdas[15].textContent = formatNumber(valores.total ?? 0);
}

function pintarFilaNeto(valores) {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;

  const netoRow = Array.from(tbody.querySelectorAll("tr")).find(tr => {
    const first = tr.querySelector("td");
    return first && first.textContent.trim().toUpperCase() === "FLUJO DE CAJA NETO";
  });
  if (!netoRow) return;

  const celdas = Array.from(netoRow.querySelectorAll("td"));
  if (celdas.length < 16) return;

  for (let i = 0; i < 12; i++) {
    const v = (valores.mes?.[i] ?? 0);
    celdas[i + 1].textContent = formatNumber(v);
  }
  celdas[13].textContent = formatNumber(valores.suma ?? 0);
  celdas[14].textContent = formatNumber(valores.acumAnt ?? 0);
  celdas[15].textContent = formatNumber(valores.total ?? 0);
}

function formatNumber(n) {
  const num = Number(n ?? 0);
  return isFinite(num) ? num.toFixed(2) : "0.00";
}

// arma las filas para un año dado usando la tabla actual (año visible)
function construirFilasParaAnno(anio) {
  const tbody = document.getElementById("bodyRows");
  const filas = [];
  if (!tbody || !proyectoSeleccionado) return filas;

  let orden = 1;
  const rows = tbody.querySelectorAll("tr.data-row");

  rows.forEach(tr => {
    const first = tr.querySelector("td");
    if (!first) return;

    const codPartida = parseInt(first.dataset.codPartida || tr.dataset.codPartida, 10);
    const ingEgr = tr.dataset.ingEgr || "";

    if (!codPartida || !ingEgr) return;

    const tds = tr.querySelectorAll("td");
    const impRealMes = [];

    for (let i = 1; i <= 12 && i < tds.length; i++) {
      const txt = (tds[i].textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      impRealMes.push(isNaN(num) ? 0 : num);
    }

    const tieneDatos = impRealMes.some(v => v !== 0);
    if (!tieneDatos) return;

    filas.push({
      anno: anio,
      codCia: proyectoSeleccionado.codCia,
      codPyto: proyectoSeleccionado.codPyto,
      ingEgr,
      tipo: "R",
      codPartida,
      orden: orden++,
      impRealMes
    });
  });

  return filas;
}

// arma las filas para un año dado consultando directamente al backend
async function construirFilasParaAnnoDesdeBackend(anio) {
  if (!proyectoSeleccionado) return [];
  const url = `${API_BASE}/valores/real?codCia=${proyectoSeleccionado.codCia}&codPyto=${proyectoSeleccionado.codPyto}&anno=${anio}`;

  try {
    const res = await fetch(url, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      console.error(`Error al obtener valores para el año ${anio}:`, txt || `HTTP ${res.status}`);
      return [];
    }

    const data = await res.json();
    const filas = [];
    let orden = 1;

    data.forEach(row => {
      if (row.ingEgr === "N" || row.codPartida === 0) return;

      const codPartida = parseInt(row.codPartida, 10);
      const ingEgr = row.ingEgr || "";
      if (!codPartida || !ingEgr) return;

      const meses = (row.valores && Array.isArray(row.valores.mes))
        ? row.valores.mes
        : [];

      const impRealMes = [];
      for (let i = 0; i < 12; i++) {
        const v = Number(meses[i] || 0);
        impRealMes.push(isNaN(v) ? 0 : v);
      }

      const tieneDatos = impRealMes.some(v => v !== 0);
      if (!tieneDatos) return;

      filas.push({
        anno: anio,
        codCia: proyectoSeleccionado.codCia,
        codPyto: proyectoSeleccionado.codPyto,
        ingEgr,
        tipo: "R",
        codPartida,
        orden: orden++,
        impRealMes
      });
    });

    return filas;
  } catch (err) {
    console.error(`Error al construir filas desde backend para el año ${anio}:`, err);
    return [];
  }
}

// guardar solo el año visible
async function guardarFlujoReal() {
  if (!proyectoSeleccionado || !annoSeleccionado) {
    alert("Seleccione proyecto y año antes de guardar.");
    return;
  }

  const tbody = document.getElementById("bodyRows");
  if (!tbody) {
    alert("No se encontró la tabla de flujo.");
    return;
  }

  const filas = construirFilasParaAnno(annoSeleccionado);

  if (!filas.length) {
    alert("No hay filas de valores para guardar.");
    return;
  }

  const btnGuardar = document.getElementById("btnGuardar");
  const oldText = btnGuardar ? btnGuardar.textContent : "";

  try {
    if (btnGuardar) {
      btnGuardar.disabled = true;
      btnGuardar.textContent = "Guardando...";
    }
    setStatus("Guardando flujo real...");

    const res = await fetch(`${API_BASE}/valores/guardar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(filas)
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || `HTTP ${res.status}`);
    }

    setStatus("Flujo real guardado correctamente.");
    alert("Flujo de caja real guardado correctamente ✅");
  } catch (err) {
    console.error("Error al guardar flujo real:", err);
    setStatus("Error al guardar flujo real.");
    alert("Ocurrió un error al guardar el flujo real.");
  } finally {
    if (btnGuardar) {
      btnGuardar.disabled = false;
      btnGuardar.textContent = oldText || "Guardar";
    }
  }
}

// Guardar TODOS los años
async function guardarTodosLosAnios() {
  if (!proyectoSeleccionado) {
    alert("Seleccione un proyecto primero.");
    return;
  }

  const { annoIni, annoFin } = proyectoSeleccionado;
  if (!annoIni || !annoFin || annoIni > annoFin) {
    alert("Rango de años inválido para el proyecto.");
    return;
  }

  const btnGuardarTodos = document.getElementById("btnGuardarTodos");
  const oldText = btnGuardarTodos ? btnGuardarTodos.textContent : "";

  const filas = [];

  try {
    if (btnGuardarTodos) {
      btnGuardarTodos.disabled = true;
      btnGuardarTodos.textContent = "Guardando...";
    }
    setStatus("Guardando flujo real de todos los años...");

    for (let anio = annoIni; anio <= annoFin; anio++) {
      if (anio === annoSeleccionado) {
        filas.push(...construirFilasParaAnno(anio));
      } else {
        const filasAnno = await construirFilasParaAnnoDesdeBackend(anio);
        filas.push(...filasAnno);
      }
    }

    if (!filas.length) {
      alert("No hay filas para guardar.");
      setStatus("No hay datos para guardar en ningún año.");
      return;
    }

    const res = await fetch(`${API_BASE}/valores/guardar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(filas)
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || `HTTP ${res.status}`);
    }

    setStatus("Flujo real de todos los años guardado correctamente.");
    alert("Flujo real de todos los años guardado correctamente ✅");
  } catch (err) {
    console.error("Error al guardar todos los años:", err);
    setStatus("Error al guardar todos los años.");
    alert("Ocurrió un error al guardar todos los años.");
  } finally {
    if (btnGuardarTodos) {
      btnGuardarTodos.disabled = false;
      btnGuardarTodos.textContent = oldText || "Guardar todos";
    }
  }
}

// Inicialización final
async function initRealFlow() {
  const tieneTablaFlujoReal =
    document.getElementById("headerRow") &&
    document.getElementById("bodyRows");

  if (tieneTablaFlujoReal) {
    crearHeaderTabla();
    agregarFilasBase();
    setupEventListeners();
  }
}

document.addEventListener("DOMContentLoaded", initRealFlow);
