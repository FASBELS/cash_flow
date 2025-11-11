// Frontend/script-proyectado.js
// Misma lógica base que Flujo Real, pero aislada con IDs terminados en "Proy"

const PROJECTS_BASE = "http://localhost:8080/api/proyectos";
const API_BASE      = "http://localhost:8080/api";

let proyectosProy = [];
let conceptosCargadosProy = { ingresos: [], egresos: [] };
let proyectoSeleccionadoProy = null;
let annoSeleccionadoProy = null;
let valoresProyActivo = false; // <-- NUEVO: indica si ya se activó el modo "valores proyectados"

// --- Referencias UI Proyectado ---
const proyectoInfoProyEl = document.getElementById("proyectoInfoProy");
const codCiaProyHidden   = document.getElementById("codCiaProyHidden");
const codPytoProyHidden  = document.getElementById("codPytoProyHidden");
const statusMsgProyEl    = document.getElementById("statusMsgProy");

function setStatusProy(msg) {
  if (statusMsgProyEl) statusMsgProyEl.textContent = msg || "";
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

async function cargarConceptosProy(codPyto) {
  const url = `${API_BASE}/conceptos?codPyto=${codPyto}`;
  const res = await fetch(url, { mode: "cors" });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} ${res.statusText} ${txt}`);
  }

  const data = await res.json();

  const ingresos = data
    .filter(d => d.ingEgr === "I")
    .sort((a,b)=>(a.orden ?? a.codPartida)-(b.orden ?? b.codPartida));

  const egresos = data
    .filter(d => d.ingEgr === "E")
    .sort((a,b)=>(a.orden ?? a.codPartida)-(b.orden ?? b.codPartida));

  conceptosCargadosProy.ingresos = ingresos;
  conceptosCargadosProy.egresos  = egresos;

  renderConceptosProy();
}

function crearFilaPartidaProy(partida) {
  const tr = document.createElement("tr");

  // 🔹 Identificación para guardado
  tr.classList.add("data-row-proy");
  if (partida.ingEgr) {
    tr.dataset.ingEgr = partida.ingEgr;    // 'I' o 'E'
  }

  const tdConcepto = document.createElement("td");
  tdConcepto.textContent = partida.desPartida;
  tdConcepto.dataset.codPartida = partida.codPartida;
  tdConcepto.classList.add("concepto-column");

  if (typeof partida.nivel === "number" && !isNaN(partida.nivel)) {
    tdConcepto.style.paddingLeft = `${Math.max(0, partida.nivel - 1) * 16}px`;
  }

  tr.appendChild(tdConcepto);

  // 12 meses + Suma + Acum Ant. + Total
  for (let i = 0; i < 15; i++) {
    const td = createNumberCell(0);
    td.dataset.codPartida = partida.codPartida;
    td.dataset.colIndex = i;
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
    trNet.appendChild(createNumberCell(0));
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

// =========================
// RENDER VALORES PROYECTADOS (desde backend)
// =========================

function renderFlujoProyectado(filas) {
  const tbody = document.getElementById("bodyRowsProy");
  if (!tbody) return;

  tbody.innerHTML = "";

  if (!Array.isArray(filas) || filas.length === 0) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 16;
    td.textContent = "Sin información proyectada para el año seleccionado.";
    td.classList.add("text-center");
    tr.appendChild(td);
    tbody.appendChild(tr);
    return;
  }

  // INGRESOS
  const trIngHeader = document.createElement("tr");
  trIngHeader.classList.add("separator-row");
  const tdIng = document.createElement("td");
  tdIng.colSpan = 16;
  tdIng.textContent = "INGRESOS";
  tdIng.classList.add("separator-cell");
  trIngHeader.appendChild(tdIng);
  tbody.appendChild(trIngHeader);

  filas
    .filter(f => f.ingEgr === "I")
    .forEach(f => {
      const tr = document.createElement("tr");

      tr.classList.add("data-row-proy");
      tr.dataset.ingEgr = "I";

      const tdConcepto = document.createElement("td");
      tdConcepto.textContent = f.desPartida;
      tdConcepto.dataset.codPartida = f.codPartida;
      tdConcepto.classList.add("concepto-column");
      tr.appendChild(tdConcepto);

      const vals = f.valores || {};
      const meses = vals.mes || vals.meses || [];

      for (let i = 0; i < 12; i++) {
        const td = createNumberCell(meses[i] ?? 0);
        td.dataset.codPartida = f.codPartida;
        td.dataset.colIndex = i;
        tr.appendChild(td);
      }

      const tdSuma = createNumberCell(vals.suma ?? 0);
      tdSuma.dataset.codPartida = f.codPartida;
      tdSuma.dataset.colIndex = 12;
      tr.appendChild(tdSuma);

      const tdAcum = createNumberCell(vals.acumAnt ?? 0);
      tdAcum.dataset.codPartida = f.codPartida;
      tdAcum.dataset.colIndex = 13;
      tr.appendChild(tdAcum);

      const tdTotal = createNumberCell(vals.total ?? 0);
      tdTotal.dataset.codPartida = f.codPartida;
      tdTotal.dataset.colIndex = 14;
      tr.appendChild(tdTotal);

      tbody.appendChild(tr);
    });

  // EGRESOS
  const trEgrHeader = document.createElement("tr");
  trEgrHeader.classList.add("separator-row");
  const tdEgr = document.createElement("td");
  tdEgr.colSpan = 16;
  tdEgr.textContent = "EGRESOS";
  tdEgr.classList.add("separator-cell");
  trEgrHeader.appendChild(tdEgr);
  tbody.appendChild(trEgrHeader);

  filas
    .filter(f => f.ingEgr === "E")
    .forEach(f => {
      const tr = document.createElement("tr");

      tr.classList.add("data-row-proy");
      tr.dataset.ingEgr = "E";

      const tdConcepto = document.createElement("td");
      tdConcepto.textContent = f.desPartida;
      tdConcepto.dataset.codPartida = f.codPartida;
      tdConcepto.classList.add("concepto-column");
      tr.appendChild(tdConcepto);

      const vals = f.valores || {};
      const meses = vals.mes || vals.meses || [];

      for (let i = 0; i < 12; i++) {
        const td = createNumberCell(meses[i] ?? 0);
        td.dataset.codPartida = f.codPartida;
        td.dataset.colIndex = i;
        tr.appendChild(td);
      }

      const tdSuma = createNumberCell(vals.suma ?? 0);
      tdSuma.dataset.codPartida = f.codPartida;
      tdSuma.dataset.colIndex = 12;
      tr.appendChild(tdSuma);

      const tdAcum = createNumberCell(vals.acumAnt ?? 0);
      tdAcum.dataset.codPartida = f.codPartida;
      tdAcum.dataset.colIndex = 13;
      tr.appendChild(tdAcum);

      const tdTotal = createNumberCell(vals.total ?? 0);
      tdTotal.dataset.codPartida = f.codPartida;
      tdTotal.dataset.colIndex = 14;
      tr.appendChild(tdTotal);

      tbody.appendChild(tr);
    });

  // FLUJO NETO (ingEgr = 'N')
  const filaNeto = filas.find(f => f.ingEgr === "N");
  if (filaNeto) {
    const trNet = document.createElement("tr");
    trNet.classList.add("separator-row", "separator-neto");
    trNet.appendChild(createTextCell(filaNeto.desPartida || "FLUJO DE CAJA NETO PROYECTADO"));

    const vals = filaNeto.valores || {};
    const meses = vals.mes || vals.meses || [];
    for (let i = 0; i < 12; i++) {
      trNet.appendChild(createNumberCell(meses[i] ?? 0));
    }
    trNet.appendChild(createNumberCell(vals.suma ?? 0));
    trNet.appendChild(createNumberCell(vals.acumAnt ?? 0));
    trNet.appendChild(createNumberCell(vals.total ?? 0));

    tbody.appendChild(trNet);
  }
}

// =========================
// CONSTRUIR FILAS PARA AÑO VISIBLE (TABLA)
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

    const tds = tr.querySelectorAll("td");
    const impRealMes = [];

    // columnas 1..12 = meses proyectados en la grilla
    for (let i = 1; i <= 12 && i < tds.length; i++) {
      const td = tds[i];
      const txt = (td.textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      impRealMes.push(isNaN(num) ? 0 : num);
    }

    const tieneDatos = impRealMes.some(v => v !== 0);
    if (!tieneDatos) return;

    filas.push({
      anno: anio,
      codCia: proyectoSeleccionadoProy.codCia,
      codPyto: proyectoSeleccionadoProy.codPyto,
      ingEgr,
      tipo: "P",
      codPartida,
      orden: orden++,
      impRealMes
    });
  });

  return filas;
}

// 🆕 NUEVO: CONSTRUIR FILAS PARA AÑO DESDE BACKEND (OTROS AÑOS)
async function construirFilasParaAnnoProyDesdeBackend(anio) {
  if (!proyectoSeleccionadoProy) return [];

  const codCia = proyectoSeleccionadoProy.codCia;
  const codPyto = proyectoSeleccionadoProy.codPyto;
  const url = `${API_BASE}/flujo-proyectado/valores?codCia=${codCia}&codPyto=${codPyto}&anno=${anio}`;

  try {
    const res = await fetch(url, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      console.error(`Error al obtener valores proyectados ${anio}:`, txt || `HTTP ${res.status}`);
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

      const vals = row.valores || {};
      const meses = vals.mes || vals.meses || row.impRealMes || row.impProyMes || [];

      const impRealMes = [];
      for (let i = 0; i < 12; i++) {
        const v = Number(meses[i] || 0);
        impRealMes.push(isNaN(v) ? 0 : v);
      }

      const tieneDatos = impRealMes.some(v => v !== 0);
      if (!tieneDatos) return;

      filas.push({
        anno: anio,
        codCia,
        codPyto,
        ingEgr,
        tipo: "P",
        codPartida,
        orden: orden++,
        impRealMes
      });
    });

    return filas;
  } catch (err) {
    console.error(`Error al construir filas proyectadas desde backend para ${anio}:`, err);
    return [];
  }
}

// =========================
// CARGA (FETCH) DE VALORES PROYECTADOS (reutilizable)
// =========================

async function cargarValoresProyectados() {
  if (!proyectoSeleccionadoProy) {
    alert("Seleccione primero un proyecto.");
    return;
  }

  if (!annoSeleccionadoProy) {
    const yearSelectProy = document.getElementById("yearSelectProy");
    if (yearSelectProy && yearSelectProy.value) {
      annoSeleccionadoProy = parseInt(yearSelectProy.value, 10);
    }
  }

  if (!annoSeleccionadoProy) {
    alert("Seleccione un año para ver el flujo proyectado.");
    return;
  }

  const codCia = proyectoSeleccionadoProy.codCia;
  const codPyto = proyectoSeleccionadoProy.codPyto;
  const url = `${API_BASE}/flujo-proyectado/valores?codCia=${codCia}&codPyto=${codPyto}&anno=${annoSeleccionadoProy}`;

  setStatusProy(`Cargando valores proyectados (${annoSeleccionadoProy})...`);
  const resp = await fetch(url, { mode: "cors" });

  if (!resp.ok) {
    const errorText = await resp.text().catch(() => "");
    console.error("Error backend flujo proyectado:", errorText);
    alert("Error al obtener el flujo de caja proyectado.");
    setStatusProy("Error al cargar valores proyectados.");
    return;
  }

  const filas = await resp.json();
  renderFlujoProyectado(filas);
  setStatusProy(`Valores proyectados cargados para ${annoSeleccionadoProy}.`);
}

// =========================
// EVENTOS
// =========================

function setupEventListenersProy() {
  const selectProyecto = document.getElementById("selectProyectoProy");
  if (!selectProyecto) return; // no estamos en la pantalla proyectada

  const btnProyectos    = document.getElementById("btnProyectosProy");
  const yearSelect      = document.getElementById("yearSelectProy");
  const fechaInicio     = document.getElementById("fechaInicioProy");
  const fechaFin        = document.getElementById("fechaFinProy");
  const btnConceptoProy = document.getElementById("btnConceptoProy");
  const btnValoresProy  = document.getElementById("btnValoresProy");
  const btnGuardarProy      = document.getElementById("btnGuardarProy");
  const btnGuardarTodosProy = document.getElementById("btnGuardarTodosProy");

  // Botón PROYECTOS
  if (btnProyectos) {
    btnProyectos.addEventListener("click", async (ev) => {
      const btn = ev.currentTarget;

      if (!btn.classList.contains("btn-off")) {
        return;
      }

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

  // Cambio de proyecto
  selectProyecto.addEventListener("change", (e) => {
    const value = e.target.value;

    if (!value) {
      resetTablaProy(true);
      return;
    }

    resetTablaProy(false);
    valoresProyActivo = false;

    const optSel  = e.target.options[e.target.selectedIndex];
    const codPyto = parseInt(value, 10);
    const codCia  = 1; // fijo como en real
    const annoIni = parseInt(optSel.dataset.annoIni, 10);
    const annoFin = parseInt(optSel.dataset.annoFin, 10);

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

    setStatusProy("Proyecto seleccionado. Puede cargar conceptos y valores proyectados.");
  });

  // Botón CONCEPTO
  if (btnConceptoProy) {
    btnConceptoProy.addEventListener("click", async (ev) => {
      if (!proyectoSeleccionadoProy) {
        alert("Seleccione primero un proyecto.");
        return;
      }

      const btn = ev.currentTarget;
      btn.disabled = true;
      const old = btn.textContent;
      btn.textContent = "Cargando...";

      try {
        await cargarConceptosProy(proyectoSeleccionadoProy.codPyto);
        setStatusProy("Conceptos cargados. Ahora puede consultar valores proyectados.");
      } catch (err) {
        console.error("ERROR cargarConceptosProy:", err);
        alert("No se pudieron cargar los conceptos: " + err.message);
      } finally {
        btn.textContent = old;
        btn.disabled = false;
      }
    });
  }

  // Cambio de año
  if (yearSelect) {
    yearSelect.addEventListener("change", async (e) => {
      if (!proyectoSeleccionadoProy) return;
      if (!e.target.value) return;

      annoSeleccionadoProy = parseInt(e.target.value, 10);
      setStatusProy(`Año ${annoSeleccionadoProy} seleccionado.`);

      if (valoresProyActivo) {
        try {
          await cargarValoresProyectados();
        } catch (err) {
          console.error(err);
          setStatusProy("Error al actualizar valores para el año seleccionado.");
        }
      }
    });
  }

  // Botón VALORES
  if (btnValoresProy) {
    btnValoresProy.addEventListener("click", async () => {
      try {
        if (!proyectoSeleccionadoProy) {
          alert("Seleccione primero un proyecto.");
          return;
        }

        valoresProyActivo = true;

        await cargarValoresProyectados();
        setStatusProy("Valores proyectados cargados. Al cambiar de año se actualizarán automáticamente.");
      } catch (err) {
        console.error(err);
        alert("Ocurrió un error al cargar el flujo proyectado.");
        setStatusProy("Error al cargar valores proyectados.");
      }
    });
  }

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

    const res = await fetch(`${API_BASE}/valores/guardar`, {
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

// 🔄 MODIFICADO: Guardar todos los años proyectados correctamente
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
      if (anio === annoSeleccionadoProy) {
        // Año visible: usa la grilla (incluye cambios del usuario)
        filas.push(...construirFilasParaAnnoProy(anio));
      } else {
        // Otros años: usa la foto oficial desde backend
        const filasAnno = await construirFilasParaAnnoProyDesdeBackend(anio);
        filas.push(...filasAnno);
      }
    }

    if (!filas.length) {
      alert("No hay filas para guardar.");
      setStatusProy("No hay datos proyectados para guardar.");
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
