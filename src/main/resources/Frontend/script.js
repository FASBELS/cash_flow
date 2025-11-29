const PROJECTS_BASE = "http://localhost:8080/api/proyectos";
const API_BASE      = "http://localhost:8080/api";
let proyectos = [];
let conceptosCargados = { ingresos: [], egresos: [] };
let proyectoSeleccionado = null;
let annoSeleccionado = null;

let mapaProyectado = {};
let comparacionActiva = false; 
let valoresRealesPorAnno = {}; 
let conceptosYaCargados   = false;
let valoresRealesActivos  = false;
let ciaSeleccionada = null;

const proyectoInfoEl = document.getElementById("proyectoInfo");
const codCiaHidden   = document.getElementById("codCiaHidden");
const codPytoHidden  = document.getElementById("codPytoHidden");
const statusMsgEl    = document.getElementById("statusMsg");

function setStatus(msg) {
  if (statusMsgEl) statusMsgEl.textContent = msg || "";
}

const MESES = ["ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic"];

function getVersionActual() {
  const sel = document.getElementById("versionSelect");
  const v = sel?.value?.trim();
  return v || "1";
}

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

function mapEntradaPartida(entry, nivelBase = 1) {
  console.log("DEBUG mapEntradaPartida ENTRY:", entry);
  if (!entry) return null;
  const p = entry.partida || entry;

  const nodo = {
    codPartida: p.codPartida ?? p.CodPartida,
    codPartidas: p.codPartidas ?? p.CodPartidas ?? null,
    ingEgr: (p.ingEgr ?? p.IngEgr ?? "").toUpperCase(),
    desPartida: p.desPartida ?? p.DesPartida ?? "",
    nivel: Number(p.nivel ?? p.Nivel ?? nivelBase ?? 1),
    orden: p.orden ?? p.Orden ?? null,
    noProyectado:
    (entry.noProyectado ??
     entry.partida?.noProyectado ??
     p.noProyectado ??
     false),
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

function flattenTree(tree, out = []) {
  const visit = (n) => {
    out.push(n);
    (n.children || []).forEach(ch => visit(ch));
  };
  (tree || []).forEach(visit);
  return out;
}

function splitByTipo(tree) {
  return {
    ingresos: (tree || []).filter(n => (n.ingEgr || "").toUpperCase() === "I"),
    egresos:  (tree || []).filter(n => (n.ingEgr || "").toUpperCase() === "E"),
  };
}

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

function resetTabla(resetProyecto = false) {
  conceptosCargados = { ingresos: [], egresos: [] };
  proyectoSeleccionado = null;
  annoSeleccionado = null;
  mapaProyectado = {};
  comparacionActiva = false;
  valoresRealesPorAnno = {};
  conceptosYaCargados  = false;
  valoresRealesActivos = false;

  if (resetProyecto) {
    document.getElementById("selectProyecto").value = "";
    const yd = document.getElementById("yearDisplay"); // opcional
    if (yd) yd.textContent = "";
    if (proyectoInfoEl) proyectoInfoEl.textContent = "";
    if (codCiaHidden) codCiaHidden.value = "";
    if (codPytoHidden) codPytoHidden.value = "";
    setStatus("");
  }

  resetComparacionVisual();

  agregarFilasBase();
}

//CARGA DE COMPAÑÍAS 
async function cargarCias() {
  const selectCia = document.getElementById("selectCia");
  if (!selectCia) return;

  try {
    selectCia.disabled = true;
    selectCia.innerHTML = '<option value="">-- Seleccione compañía --</option>';

    const res = await fetch(`${API_BASE}/cias`);
    if (!res.ok) throw new Error("Error al obtener compañías");

    const cias = await res.json();

    cias.forEach(c => {
      const opt = document.createElement("option");
      const cod = c.codCia ?? c.CODCIA ?? c.codcia;
      const desc =
        c.desCorta ?? c.descorta ?? c.DesCorta ??
        c.descia ?? c.DesCia ?? `CIA ${cod}`;

      opt.value = String(cod);
      opt.textContent = desc;
      selectCia.appendChild(opt);
    });

    selectCia.disabled = false;
  } catch (err) {
    console.error(err);
    alert("No se pudieron cargar las compañías: " + err.message);
    selectCia.disabled = true;
  }
}


async function cargarProyectos(codCia) {
  if (!codCia) {
    throw new Error("Debe seleccionar una compañía.");
  }

  try {

    const res = await fetch(`${PROJECTS_BASE}?codCia=${codCia}`);


    if (!res.ok) throw new Error("Error al obtener proyectos");
    proyectos = await res.json();

    const select = document.getElementById("selectProyecto");
    select.innerHTML = '<option value="">-- Seleccione proyecto --</option>';

    proyectos.forEach((p) => {
      const opt = document.createElement("option");
      const codPyto = p.codPyto ?? p.CodPyto ?? p.codpyto;
      const nombre  = p.nombPyto ?? p.NombPyto ?? p.nombpyto ?? `Proyecto ${codPyto}`;

      opt.value = String(codPyto);
      opt.textContent = nombre;
      opt.dataset.annoIni = p.annoIni ?? p.AnnoIni ?? p.annoini;
      opt.dataset.annoFin = p.annoFin ?? p.AnnoFin ?? p.annofin;

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


async function cargarValoresRealesDeTodosLosAnios() {
  if (!proyectoSeleccionado) {
    setStatus("Seleccione un proyecto primero.");
    return;
  }

  const codCia  = proyectoSeleccionado.codCia;
  const codPyto = proyectoSeleccionado.codPyto;

  const yearSelect = document.getElementById("yearSelect");
  if (!yearSelect) return;

  valoresRealesPorAnno = {};

  const anios = Array.from(yearSelect.options)
    .map(o => parseInt(o.value, 10))
    .filter(n => !isNaN(n));

  for (const anio of anios) {
    const url = `${API_BASE}/valores/real?codCia=${codCia}&codPyto=${codPyto}&anno=${anio}`;
    const res = await fetch(url, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      console.error(`Error cargando valores reales ${anio}:`, txt || `HTTP ${res.status}`);
      continue;
    }
    const data = await res.json();         
    valoresRealesPorAnno[anio] = data;        
  }

  setStatus("Valores reales cargados para todos los años.");
}

async function asegurarValoresParaAnno(anno) {
  if (valoresRealesPorAnno[anno]) return;

  if (!proyectoSeleccionado) return;

  const codCia  = proyectoSeleccionado.codCia;
  const codPyto = proyectoSeleccionado.codPyto;

  const url = `${API_BASE}/valores/real?codCia=${codCia}&codPyto=${codPyto}&anno=${anno}`;
  const res = await fetch(url, { mode: "cors" });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    console.error(`Error cargando valores reales ${anno}:`, txt || `HTTP ${res.status}`);
    return;
  }

  const data = await res.json();
  valoresRealesPorAnno[anno] = data;
}

function calcularSumasJerarquicas() {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;

  // Procesar de nivel 3 hacia arriba (3 -> 2 -> 1)
  for (let nivelActual = 3; nivelActual >= 1; nivelActual--) {
    const filasPadre = tbody.querySelectorAll(`tr.data-row[data-nivel="${nivelActual}"]`);
    
    for (const filaPadre of filasPadre) {
      const codPartidaPadre = filaPadre.querySelector("td.concepto-column")?.dataset.codPartida;
      if (!codPartidaPadre) return;

      // Buscar todas las filas hijas directas
      const filasHijas = Array.from(tbody.querySelectorAll("tr.data-row")).filter(tr => {
        const parentPartida = tr.dataset.parentPartida;
        const nivel = parseInt(tr.dataset.nivel || "1", 10);
        return parentPartida === codPartidaPadre && nivel === nivelActual + 1;
      });

      // Si no tiene hijos, es una hoja (nivel 3), no calculamos nada
      if (filasHijas.length === 0) continue;

      // Sumar los valores de los hijos para cada mes
      const celdasPadre = Array.from(filaPadre.querySelectorAll("td"));
      
      for (let mes = 1; mes <= 12; mes++) {
        let sumaHijos = 0;
        
        for (const filaHija of filasHijas) {
          const tdHijo = filaHija.querySelector(`td[data-mes="${mes}"]`);
          if (tdHijo) {
            const valor = parseFloat((tdHijo.textContent || "").replace(/,/g, "").trim()) || 0;
            sumaHijos += valor;
          }
        };

        // Actualizar el valor del padre
        const tdPadre = filaPadre.querySelector(`td[data-mes="${mes}"]`);
        if (tdPadre) {
          tdPadre.textContent = formatNumber(sumaHijos);
        }
      }

      // Recalcular Suma, Acum.Ant y Total del padre
      recalcularFilaPartidaReal(filaPadre);
    };
  }
}

/**
 * Construye un mapa de relaciones padre-hijo desde el árbol cargado
 */
function construirMapaPadresHijos() {
  const mapa = {
    ingresos: new Map(),
    egresos: new Map()
  };

  function agregarRelaciones(lista, map) {
    lista.forEach(nodo => {
      if (nodo.children && nodo.children.length > 0) {
        const hijos = nodo.children.map(h => h.codPartida);
        map.set(nodo.codPartida, hijos);
        
        // Recursivamente procesar hijos
        agregarRelaciones(nodo.children, map);
      }
    });
  }

  agregarRelaciones(conceptosCargados.ingresos, mapa.ingresos);
  agregarRelaciones(conceptosCargados.egresos, mapa.egresos);

  return mapa;
}

/**
 * Asigna el parentPartida a cada fila basándose en el árbol
 */
function asignarParentPartidaEnDOM() {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;

  // Construir un mapa de codPartida -> nodo con todos los nodos aplanados
  const todosList = [...conceptosCargados.ingresos, ...conceptosCargados.egresos];
  
  function procesarNodo(nodo, parentId = null) {
    const fila = tbody.querySelector(`tr.data-row td.concepto-column[data-cod-partida="${nodo.codPartida}"]`)?.parentElement;
    
    if (fila && parentId) {
      fila.dataset.parentPartida = String(parentId);
    }

    if (nodo.children && nodo.children.length > 0) {
      nodo.children.forEach(hijo => {
        procesarNodo(hijo, nodo.codPartida);
      });
    }
  }

  todosList.forEach(nodo => procesarNodo(nodo));
}

function crearFilaNodo(nodo) {
  const tr = document.createElement("tr");
  tr.classList.add("data-row");

  const nivel = Number(nodo.nivel ?? 1);

  tr.dataset.nivel = String(nivel);
  if (nodo.noProyectado) {
    tr.classList.add("no-proyectado");
    tr.dataset.noProyectado = "true";
  }

  if (nodo.ingEgr) tr.dataset.ingEgr = nodo.ingEgr;
  if (nodo.codPartidas) tr.dataset.codPartidas = nodo.codPartidas;
  if (nodo.codPartida != null) tr.dataset.codPartida = String(nodo.codPartida);

  const tdConcepto = document.createElement("td");
  tdConcepto.classList.add("concepto-column");
  tdConcepto.dataset.codPartida = nodo.codPartida;

  tdConcepto.style.paddingLeft = `${Math.max(0, nivel - 1) * 16}px`;

  tdConcepto.textContent = nodo.desPartida || nodo.descripcion || nodo.nombre || "";
  tr.appendChild(tdConcepto);

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


function encontrarAnclaDeInsercion(tbody, nodo) {
   const ingEgr = nodo.ingEgr;
   const codPartidas = nodo.codPartidas;

   if (!ingEgr || !codPartidas) {
     console.warn("Nodo sin ingEgr o codPartidas", nodo);
     return null; 
   }

   const filas = tbody.querySelectorAll("tr.data-row");
   let mejorAncla = null; 

   if (ingEgr === 'I') {
     mejorAncla = tbody.querySelector("tr.separator-row:first-of-type");
   } else {
      mejorAncla = tbody.querySelector("tr.separator-row:nth-of-type(2)");
  }

   for (const fila of filas) {
     const filaIngEgr = fila.dataset.ingEgr;
     const filaCodPartidas = fila.dataset.codPartidas;

     if (filaIngEgr !== ingEgr || !filaCodPartidas) {
       continue;
      }

     if (filaCodPartidas.localeCompare(codPartidas) < 0) {

       mejorAncla = fila;
     }
}

   return mejorAncla;
}

async function aplicarValoresDesdeCache(anno) {
    await asegurarValoresParaAnno(anno);
    
    const data = valoresRealesPorAnno[anno];
    if (!data) {
        console.warn("No hay datos en cache para el año", anno);
        return;
    }

    const tbody = document.getElementById("bodyRows");
    if (!tbody) return;


    resetCeldasNumericas();
    resetComparacionVisual();

    const filasProyectadasEsteAnno = new Set();
    const filasNoProyectadasEnDOM = tbody.querySelectorAll("tr.no-proyectado");
    
    filasNoProyectadasEnDOM.forEach(fila => {
        fila.style.display = 'none'; 
    });

    data.forEach(nodo => {
        if (nodo.ingEgr === 'N' || nodo.codPartida === 0) return; 

        filasProyectadasEsteAnno.add(String(nodo.codPartida)); 

        const filaExistente = tbody.querySelector(`tr > td[data-cod-partida="${nodo.codPartida}"]`);
        
        if (!filaExistente) {
            console.log("Creando fila faltante para:", nodo.desPartida);
            const nuevaFila = crearFilaNodo(nodo); 
            
            const ancla = encontrarAnclaDeInsercion(tbody, nodo);
            
            if (ancla) {
                ancla.after(nuevaFila); 
            } else {
                tbody.appendChild(nuevaFila);
            }
            
        } else if (filaExistente.parentElement.classList.contains("no-proyectado")) {
            filaExistente.parentElement.style.display = 'table-row';
        }
    });

    data.forEach(row => {
        if (row.ingEgr === "N") {
            pintarFilaNeto(row.valores);
        } else {
            pintarFilaPartidaValores(row.codPartida, row.valores);
        }
    });
}

function setupEventListeners() {
  const selectProyecto = document.getElementById("selectProyecto");
  const selectCia      = document.getElementById("selectCia");

  // Si no hay alguno de los selects, asumimos que no es la pantalla de flujo real
  if (!selectProyecto || !selectCia) {
    return;
  }

  // Al inicio, los proyectos no se pueden elegir hasta cargar
  selectProyecto.disabled = true;

  // Cuando cambio de compañía
  selectCia.addEventListener("change", (e) => {
    const codCia = parseInt(e.target.value, 10) || 0;

    // Limpio proyectos y tabla cada vez que cambio de compañía
    selectProyecto.innerHTML = '<option value="">-- Seleccione proyecto --</option>';
    selectProyecto.disabled  = !codCia;

    // Reseteo toda la tabla y selección de proyecto
    resetTabla(true);

    // Pongo un mensaje de ayuda
    if (codCia) {
      setStatus("Presione el botón Proyectos y luego seleccione un proyecto.");
    } else {
      setStatus("Seleccione una compañía.");
    }

    // Vuelvo a poner el botón Proyectos en estado 'off' (si existe)
    const btnProyectosTmp = document.getElementById("btnProyectos");
    if (btnProyectosTmp) {
      btnProyectosTmp.classList.add("btn-off");
    }
  });

  const btnProyectos = document.getElementById("btnProyectos");
  if (btnProyectos) {
    btnProyectos.addEventListener("click", async (ev) => {
      const btn    = ev.currentTarget;
      const select = document.getElementById("selectProyecto");
      const ciaSel = document.getElementById("selectCia");

      const codCia = parseInt(ciaSel.value, 10);

      if (!codCia) {
        alert("Seleccione primero una compañía.");
        return;
      }

      if (!btn.classList.contains("btn-off")) {
        console.log("Proyectos ya cargados.");
        return;
      }

      btn.disabled = true;
      btn.textContent = "Cargando...";

      try {
        await cargarProyectos(codCia);  
        btn.classList.remove("btn-off");
        btn.textContent = "Proyectos";
        select.disabled = false;
        select.focus();
        setStatus("Proyectos cargados. Seleccione uno.");
      } catch (err) {
        console.error(err);
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
    const yearDisplay = document.getElementById("yearDisplay");
    const selectCia   = document.getElementById("selectCia");

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
    const codCia  = parseInt(selectCia.value, 10) || 0;

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
    btnPrev.addEventListener("click", async () => { 
      if (!proyectoSeleccionado) return;

      if (annoSeleccionado > proyectoSeleccionado.annoIni) {
        annoSeleccionado--;
        const yearDisplay = document.getElementById("yearDisplay");
        if (yearDisplay) yearDisplay.textContent = String(annoSeleccionado);

        if (valoresRealesActivos) {
          await aplicarValoresDesdeCache(annoSeleccionado);
        } else {
          resetCeldasNumericas();
          resetComparacionVisual();
        }
      }
    });
  }

  const btnNext = document.getElementById("btnYearNext");
  if (btnNext) {
    btnNext.addEventListener("click", async () => {  
      if (!proyectoSeleccionado) return;

      if (annoSeleccionado < proyectoSeleccionado.annoFin) {
        annoSeleccionado++;
        const yearDisplay = document.getElementById("yearDisplay");
        if (yearDisplay) yearDisplay.textContent = String(annoSeleccionado);

        if (valoresRealesActivos) {
          await aplicarValoresDesdeCache(annoSeleccionado);
        } else {
          resetCeldasNumericas();
          resetComparacionVisual();
        }
      }
    });
  }

  const btnValores = document.getElementById("btnValores");
  if (btnValores) {
    btnValores.addEventListener("click", async () => {
      if (!proyectoSeleccionado) {
        alert("Seleccione un proyecto.");
        return;
      }

      if (!conceptosYaCargados) {
        alert("Primero cargue los conceptos.");
        return;
      }

      btnValores.disabled = true;
      const old = btnValores.textContent;
      btnValores.textContent = "Calculando...";

      try {
        await cargarValoresRealesDeTodosLosAnios(); // carga TODOS los años
        if (annoSeleccionado) {
          await aplicarValoresDesdeCache(annoSeleccionado); // pinta el año actual
        }

        // marcamos que los valores están activos
        valoresRealesActivos = true;
        setStatus("Valores reales cargados. Navegue entre los años.");
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
    yearSelectEl2.addEventListener("change", async e => {  // async
      if (!proyectoSeleccionado) return;

      annoSeleccionado = parseInt(e.target.value, 10);

      const yd = document.getElementById("yearDisplay");
      if (yd) yd.textContent = String(annoSeleccionado);

      // Solo cargar valores si el usuario ya presionó el botón "Valores"
      if (valoresRealesActivos) {
        await aplicarValoresDesdeCache(annoSeleccionado);
      } else {
        // Si no, solo se limpian los valores (pero mantiene estructura si ya hay conceptos)
        resetCeldasNumericas();
        resetComparacionVisual();
      }
    });
  }
  // Botón Cargar mes (flujo real)
  const btnCargarMes = document.getElementById("btnCargarMes");
  if (btnCargarMes) {
    btnCargarMes.addEventListener("click", cargarMesDesdeBoletas);
  }
  const btnRefactorizar = document.getElementById("btnRefactorizar");
if (btnRefactorizar) {
  btnRefactorizar.addEventListener("click", refactorizarMesReal);
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

  // Botón Ver diferencias con el proyectado (toggle)
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
};

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

    console.log("DEBUG entrada arrI:", arrI);
    console.log("DEBUG entrada arrE:", arrE);
    const ingresosRoots = arrI.map(e => mapEntradaPartida(e, 1));
    const egresosRoots  = arrE.map(e => mapEntradaPartida(e, 1));

    conceptosCargados.ingresos = flattenTree(ingresosRoots);
    conceptosCargados.egresos  = flattenTree(egresosRoots);

    renderArbolEnTabla();
    pintarFilasReal();
    setStatus("Árbol de partidas cargado.");

    conceptosYaCargados  = true;
    valoresRealesActivos = false;
    resetCeldasNumericas();
     
  } catch (err) {
    console.error("ERROR cargarConceptos:", err);
    alert("No se pudo cargar conceptos: " + err.message);
  }
}

function renderArbolEnTabla() {
  const tbody = document.getElementById("bodyRows");
  tbody.innerHTML = "";

  const trIngHeader = document.createElement("tr");
  trIngHeader.classList.add("separator-row");
  const tdIng = document.createElement("td");
  tdIng.colSpan = 16;
  tdIng.textContent = "INGRESOS";
  trIngHeader.appendChild(tdIng);
  tbody.appendChild(trIngHeader);

  conceptosCargados.ingresos.forEach(n => tbody.appendChild(crearFilaNodo(n)));

  const trEgrHeader = document.createElement("tr");
  trEgrHeader.classList.add("separator-row");
  const tdEgr = document.createElement("td");
  tdEgr.colSpan = 16;
  tdEgr.textContent = "EGRESOS";
  trEgrHeader.appendChild(tdEgr);
  tbody.appendChild(trEgrHeader);

  conceptosCargados.egresos.forEach(n => tbody.appendChild(crearFilaNodo(n)));

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

  if (!conceptosCargados.ingresos.length && !conceptosCargados.egresos.length) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 16;
    td.textContent = "Sin conceptos para este proyecto.";
    tr.appendChild(td);
    tbody.appendChild(tr);
  }
  resetCeldasNumericas();

  // Asignar relaciones padre-hijo después de renderizar
  asignarParentPartidaEnDOM();
}

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

function actualizarEstiloCeldaComparacion(celdaEl, codPartida, mes) {
  if (!celdaEl || !codPartida || !mes) return;

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

function resetComparacionVisual() {
  const btnComparar = document.getElementById("btnComparar");
  comparacionActiva = false;
  mapaProyectado = {};
  if (btnComparar) {
    btnComparar.textContent = "Ver diferencias con el proyectado";
  }
  limpiarComparacionCeldas();
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

function recalcularFilaPartidaReal(tr) {
  const tds = Array.from(tr.querySelectorAll("td"));
  const celdasMes = tds.filter(td => td.dataset.mes);

  let suma = 0;
  celdasMes.forEach(td => {
    const txt = (td.textContent || "").replace(/,/g, "").trim();
    const num = parseFloat(txt);
    if (!isNaN(num)) suma += num;
  });

  // Encontrar las celdas de Suma, Acum.Ant y Total
  const tdSuma  = tds.find(td => td.dataset.colIndex === "12");
  const tdAcum  = tds.find(td => td.dataset.colIndex === "13");
  const tdTotal = tds.find(td => td.dataset.colIndex === "14");

  if (tdSuma) tdSuma.textContent = formatNumber(suma);

  // El acumulado anterior se mantiene (no lo recalculamos aquí)
  const acumAnt = tdAcum
    ? parseFloat((tdAcum.textContent || "").replace(/,/g, "").trim()) || 0
    : 0;

  if (tdAcum)  tdAcum.textContent  = formatNumber(acumAnt);
  if (tdTotal) tdTotal.textContent = formatNumber(suma + acumAnt);
}

function recalcularFilaNetoReal() {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return;

  // Buscar la fila del neto
  const netRow = Array.from(tbody.querySelectorAll("tr")).find(tr => {
    const first = tr.querySelector("td");
    return first && first.textContent.trim().toUpperCase() === "FLUJO DE CAJA NETO";
  });
  if (!netRow) return;

  const netCells = Array.from(netRow.querySelectorAll("td"));
  if (netCells.length < 16) return;

  // Acumulado anterior de la fila neto (columna 14)
  const acumAntTxt = (netCells[14].textContent || "").replace(/,/g, "").trim();
  const acumAnt = parseFloat(acumAntTxt) || 0;

  // Totales por mes = sumas solo de partidas NIVEL 1: ingresos - egresos
  const totMes = new Array(12).fill(0);

  const dataRows = tbody.querySelectorAll("tr.data-row");
  dataRows.forEach(tr => {
    // Solo filas de nivel 1 participan en el NETO
    const nivel = tr.dataset.nivel ? parseInt(tr.dataset.nivel, 10) : 1;
    if (nivel !== 1) return;

    const conceptCell = tr.querySelector("td.concepto-column");
    if (!conceptCell) return;

    const ingEgr = tr.dataset.ingEgr;
    if (!ingEgr) return;

    const tds = Array.from(tr.querySelectorAll("td"));
    for (let i = 1; i <= 12; i++) {
      const txt = (tds[i].textContent || "").replace(/,/g, "").trim();
      const num = parseFloat(txt);
      if (isNaN(num)) continue;
      
      if (ingEgr === "I") {
        totMes[i - 1] += num;
      } else if (ingEgr === "E") {
        totMes[i - 1] -= num;
      }
    }
  });

  // Escribir en fila neto
  let suma = 0;
  for (let i = 0; i < 12; i++) {
    suma += totMes[i];
    netCells[i + 1].textContent = formatNumber(totMes[i]);
  }

  netCells[13].textContent = formatNumber(suma);
  netCells[14].textContent = formatNumber(acumAnt);
  netCells[15].textContent = formatNumber(suma + acumAnt);
}


function buscarNodoEnArbol(lista, codPartida) {
  for (const nodo of lista) {
    if (nodo.codPartida === codPartida) return nodo;
    const encontrada = buscarNodoEnArbol(nodo.hijos, codPartida);
    if (encontrada) return encontrada;
  }
  return null;
}

function insertarNuevaPartidaNoProyectada(item) {
  const {
    codPartida,
    desPartida,
    ingEgr,
    nivel,
    codPartidas,
    parentPartida
  } = item;

  // ============================================================
  // 1️⃣ GUARDAR VALORES ACTUALES DEL MES PARA NO PERDERLOS
  // ============================================================
  const backupValores = {};
  const filas = document.querySelectorAll("tr.data-row");

  filas.forEach(tr => {
    const cPartida = tr.querySelector("td.concepto-column")?.dataset.codPartida;
    if (!cPartida) return;

    backupValores[cPartida] = {};

    for (let m = 1; m <= 12; m++) {
      const td = tr.querySelector(`td[data-mes="${m}"]`);
      if (td) {
        backupValores[cPartida][m] = td.textContent;
      }
    }
  });

  const lista = ingEgr === "I"
    ? conceptosCargados.ingresos
    : conceptosCargados.egresos;

  const nuevoNodo = {
    codPartida,
    desPartida,
    ingEgr,
    nivel,
    codPartidas,
    hijos: [],
    noProyectado: true
  };

  if (parentPartida) {
    const padre = buscarNodoEnArbol(lista, parentPartida);
    if (padre) padre.hijos.push(nuevoNodo);
    else lista.push(nuevoNodo);
  } else {
    lista.push(nuevoNodo);
  }

  renderArbolEnTabla();
  const nuevasFilas = document.querySelectorAll("tr.data-row");

  nuevasFilas.forEach(tr => {
    const cPartida = tr.querySelector("td.concepto-column")?.dataset.codPartida;
    if (!cPartida) return;

    if (backupValores[cPartida]) {
      for (let m = 1; m <= 12; m++) {
        const td = tr.querySelector(`td[data-mes="${m}"]`);
        if (td && backupValores[cPartida][m] !== undefined) {
          td.textContent = backupValores[cPartida][m];
        }
      }
    }
  });
}



//Aplicar al DOM los valores de UN MES (data viene del endpoint /real/mes)
function aplicarValoresMesEnTablaReal(mes, data) {
  const tbody = document.getElementById("bodyRows");
  if (!tbody || !Array.isArray(data)) return;

  // 0) Limpieza previa del mes SOLO para nivel 3
  const celdasNivel3 = tbody.querySelectorAll(`tr.data-row[data-nivel="3"] td[data-mes="${mes}"]`);
  celdasNivel3.forEach(td => {
    td.textContent = formatNumber(0);
  });

  // 1) Insertar valores del backend (partidas nivel 3 y crear no proyectadas si existen)
  data.forEach(item => {
    const codPartida = item.codPartida;
    if (!codPartida) return;

    let conceptCell = tbody.querySelector(
      `tr.data-row > td.concepto-column[data-cod-partida="${codPartida}"]`
    );

    if (!conceptCell) {
      console.warn("Partida NO proyectada detectada:", item);
      insertarNuevaPartidaNoProyectada(item);

      conceptCell = tbody.querySelector(
        `tr.data-row > td.concepto-column[data-cod-partida="${codPartida}"]`
      );

      if (!conceptCell) {
        console.error("No se pudo insertar la nueva partida");
        return;
      }
    }

    const tr = conceptCell.parentElement;
    const tdMes = tr.querySelector(`td[data-mes="${mes}"]`);
    if (!tdMes) return;

    const monto = Number(item.monto ?? 0);
    tdMes.textContent = formatNumber(monto);
  });

  // 2) Asegurar que las relaciones padre-hijo estén en el DOM
  asignarParentPartidaEnDOM();

  // 3) Calcular sumas jerárquicas (nivel 3 -> 2 -> 1)
  calcularSumasJerarquicas();

  // 4) Recalcular todas las columnas agregadas (Suma, Acum.Ant, Total)
  const todasLasFilas = tbody.querySelectorAll("tr.data-row");
  todasLasFilas.forEach(tr => recalcularFilaPartidaReal(tr));

  // 5) Recalcular FLUJO DE CAJA NETO
  recalcularFilaNetoReal();
}

function formatNumber(n) {
  const num = Number(n ?? 0);
  if (!isFinite(num)) return "0.00";
  return num.toLocaleString("es-PE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}


const NOMBRES_MESES = [
  "Enero","Febrero","Marzo","Abril","Mayo","Junio",
  "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
];

function nombreMes(mes) {
  return NOMBRES_MESES[mes - 1] || `Mes ${mes}`;
}

// Lee el monto actual de la tabla para una partida y mes
function obtenerMontoMesTabla(codPartida, mes) {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return null;

  const conceptCell = tbody.querySelector(
    `tr.data-row > td.concepto-column[data-cod-partida="${codPartida}"]`
  );
  if (!conceptCell) return null;

  const tr = conceptCell.parentElement;
  const tdMes = tr.querySelector(`td[data-mes="${mes}"]`);
  if (!tdMes) return null;

  const txt = (tdMes.textContent || "").replace(/,/g, "").trim();
  const num = parseFloat(txt);
  return isNaN(num) ? 0 : num;
}

// Descripción de la partida (texto de la primera columna)
function obtenerDescripcionPartida(codPartida) {
  const tbody = document.getElementById("bodyRows");
  if (!tbody) return `Partida ${codPartida}`;

  const conceptCell = tbody.querySelector(
    `tr.data-row > td.concepto-column[data-cod-partida="${codPartida}"]`
  );
  if (!conceptCell) return `Partida ${codPartida}`;
  return conceptCell.textContent.trim();
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

function pintarFilasReal() {
  const filas = document.querySelectorAll("#tablaFlujo tr.data-row");

  filas.forEach(fila => {
    const nivel = fila.dataset.nivel;    // "1", "2" o "3"
    const tipo  = fila.dataset.ingegr;   // "I" o "E"

    if (tipo === "I") {
      if (nivel === "1") fila.classList.add("nivel1-ingresos");
      if (nivel === "2") fila.classList.add("nivel2-ingresos");
    }

    if (tipo === "E") {
      if (nivel === "1") fila.classList.add("nivel1-egresos");
      if (nivel === "2") fila.classList.add("nivel2-egresos");
    }
  });
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

async function cargarMesDesdeBoletas() {
  if (!proyectoSeleccionado || !annoSeleccionado) {
    alert("Seleccione proyecto y año antes de cargar un mes.");
    return;
  }

  if (!conceptosYaCargados) {
    alert("Primero cargue los conceptos.");
    return;
  }

  // Fecha actual del sistema 
  const hoy = new Date();
  const annoActual = hoy.getFullYear();
  const mesActual = hoy.getMonth() + 1; // 1..12

  // Determinar el máximo mes que se puede cargar para el año seleccionado
  let maxMesCargable;

  if (annoSeleccionado < annoActual) {
    maxMesCargable = 12;
  } else if (annoSeleccionado === annoActual) {
    maxMesCargable = mesActual;
  } else {
    alert("No puede cargar meses de años futuros respecto a la fecha actual.");
    return;
  }

  const nombresMes = [
    "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
  ];

  const mensajePrompt =
    "¿Qué mes desea cargar?\n" +
    "Año seleccionado: " + annoSeleccionado + "\n" +
    "Mes máximo permitido para este año: " +
    maxMesCargable + " (" + nombresMes[maxMesCargable] + ")\n\n" +
    "Ingrese un número entre 1 y " + maxMesCargable + ":";

  const mesStr = prompt(mensajePrompt);
  if (mesStr === null) return; // cancelado

  const mes = parseInt(mesStr, 10);

  if (!mes || mes < 1 || mes > maxMesCargable) {
    alert(
      "Mes inválido.\n\n" +
      "Para el año " + annoSeleccionado +
      " solo puede cargar hasta el mes " +
      maxMesCargable + " (" + nombresMes[maxMesCargable] + ")."
    );
    return;
  }

  const btn = document.getElementById("btnCargarMes");
  const oldText = btn ? btn.textContent : "";

  try {
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Cargando mes...";
    }

    const codCia  = proyectoSeleccionado.codCia;
    const codPyto = proyectoSeleccionado.codPyto;

    const url = `${API_BASE}/valores/real/mes` +
      `?codCia=${codCia}&codPyto=${codPyto}&anno=${annoSeleccionado}&mes=${mes}`;

    console.log("[REAL] Cargar mes desde boletas:", url);
    setStatus(`Cargando mes ${mes} desde boletas...`);

    const res = await fetch(url, { mode: "cors" });
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || `HTTP ${res.status}`);
    }

    const data = await res.json();
    aplicarValoresMesEnTablaReal(mes, data);

    // Marcamos que ya hay valores reales en pantalla (para que tenga sentido Guardar)
    valoresRealesActivos = true;

    setStatus(`Mes ${mes} cargado desde boletas. Revise y luego pulse Guardar.`);
    alert(`Mes ${mes} cargado correctamente desde las boletas `);
  } catch (err) {
    console.error("Error al cargar mes desde boletas:", err);
    setStatus("Error al cargar el mes desde boletas.");
    alert("Ocurrió un error al cargar el mes. Revise la consola.");
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = oldText || "Cargar mes";
    }
  }
}

async function refactorizarMesReal() {
  if (!proyectoSeleccionado || !annoSeleccionado) {
    alert("Seleccione proyecto y año antes de refactorizar un mes.");
    return;
  }

  if (!conceptosYaCargados) {
    alert("Primero cargue los conceptos.");
    return;
  }

  // Fecha actual del sistema
  const hoy = new Date();
  const annoActual = hoy.getFullYear();
  const mesActual = hoy.getMonth() + 1;

  // Mes permitido según el año seleccionado
  let maxMes;
  if (annoSeleccionado < annoActual) maxMes = 12;
  else if (annoSeleccionado === annoActual) maxMes = mesActual;
  else {
    alert("No puede refactorizar meses de años futuros.");
    return;
  }

  const mesStr = prompt(
    `¿Qué mes desea refactorizar?\n` +
    `Año: ${annoSeleccionado}\n` +
    `Mes máximo permitido: ${maxMes} (${nombreMes(maxMes)})\n\n` +
    `Ingrese número entre 1 y ${maxMes}:`
  );
  if (mesStr === null) return;

  const mes = parseInt(mesStr, 10);
  if (!mes || mes < 1 || mes > maxMes) {
    alert("Mes inválido.");
    return;
  }

  const btn = document.getElementById("btnRefactorizar");
  const oldText = btn?.textContent;

  try {
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Refactorizando...";
    }

    const { codCia, codPyto } = proyectoSeleccionado;
    const url = `${API_BASE}/valores/real/mes?codCia=${codCia}&codPyto=${codPyto}&anno=${annoSeleccionado}&mes=${mes}`;

    const res = await fetch(url);
    if (!res.ok) throw new Error(await res.text());

    const data = await res.json();
    const cambios = [];

    // Partidas que tienen boletas nuevas
    const nuevosCodPartida = new Set();

    data.forEach(item => {
      const cod = item.codPartida;
      nuevosCodPartida.add(cod);

      const nuevo = Number(item.monto ?? 0);
      const anterior = obtenerMontoMesTabla(cod, mes) ?? 0;

      if (Math.abs(nuevo - anterior) > 0.001) {
        cambios.push({
          cod,
          descripcion: obtenerDescripcionPartida(cod),
          antes: anterior,
          despues: nuevo
        });
      }
    });

    // Partidas que antes tenían valor y ahora desaparecerán
    const tbody = document.getElementById("bodyRows");
    tbody.querySelectorAll("tr.data-row").forEach(tr => {
      const cel = tr.querySelector("td.concepto-column");
      const cod = parseInt(cel?.dataset.codPartida || 0, 10);
      if (!cod || nuevosCodPartida.has(cod)) return;

      const anterior = obtenerMontoMesTabla(cod, mes) ?? 0;
      if (Math.abs(anterior) > 0.001) {
        cambios.push({
          cod,
          descripcion: obtenerDescripcionPartida(cod),
          antes: anterior,
          despues: 0
        });
      }
    });
    let msg = `Cambios detectados para ${nombreMes(mes)} ${annoSeleccionado}:\n\n`;

    if (cambios.length === 0) {
      msg += "(No hay diferencias detectadas)\n\n¿Desea continuar?";
    } else {
      cambios.forEach(c => {
        msg += `• ${c.descripcion}: ${formatNumber(c.antes)} → ${formatNumber(c.despues)}\n`;
      });
      msg += `\nEsto recalculará acumulados de los años siguientes.\n\n¿Desea continuar?`;
    }

    if (!confirm(msg)) {
      setStatus("Refactorización cancelada.");
      return;
    }
    tbody.querySelectorAll(`td[data-mes="${mes}"]`).forEach(td => {
      td.textContent = "0.00";
    });

    aplicarValoresMesEnTablaReal(mes, data);

    await guardarTodosLosAnios();

    alert(`Mes ${nombreMes(mes)} refactorizado correctamente.`);

  } catch (err) {
    console.error(err);
    alert("Error al refactorizar mes.");
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = oldText;
    }
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

    // 🔁 INVALIDAR CACHE de años >= año actual
    Object.keys(valoresRealesPorAnno).forEach(y => {
      const yn = parseInt(y, 10);
      if (!isNaN(yn) && yn >= annoSeleccionado) {
        delete valoresRealesPorAnno[yn];
      }
    });

    //RECARGAR desde backend el año visible
    await asegurarValoresParaAnno(annoSeleccionado);
    await aplicarValoresDesdeCache(annoSeleccionado);
    valoresRealesActivos = true;

    setStatus("Flujo real guardado correctamente.");
    alert("Flujo de caja real guardado correctamente ");
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

async function initRealFlow() {
  const tieneTablaFlujoReal =
    document.getElementById("headerRow") &&
    document.getElementById("bodyRows");

  if (tieneTablaFlujoReal) {
    crearHeaderTabla();
    agregarFilasBase();
    await cargarCias();   
    setupEventListeners();
  }
}

document.addEventListener("DOMContentLoaded", initRealFlow);
