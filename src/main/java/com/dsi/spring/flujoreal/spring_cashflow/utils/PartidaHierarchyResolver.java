// utils/PartidaHierarchyResolver.java
package com.dsi.spring.flujoreal.spring_cashflow.utils;

import java.util.Optional;

public final class PartidaHierarchyResolver {

  private PartidaHierarchyResolver() {}

  public static class ParentCode {
    public final Optional<Integer> codPartidaPadreNum;   // si usas numérico
    public final Optional<String>  codPartidasPadreStr;  // si usas texto
    public ParentCode(Optional<Integer> n, Optional<String> s){ this.codPartidaPadreNum=n; this.codPartidasPadreStr=s; }
  }

  /** Detecta el formato y devuelve el código del padre (sin consultar DB). */
  public static ParentCode deriveParent(String codPartidas, Integer codPartida) {
    if (codPartidas != null && codPartidas.contains("-")) {
      // ING-001-01 -> ING-001
      String padre = codPartidas.replaceFirst("-[^-]+$", "");
      return new ParentCode(Optional.empty(), Optional.of(padre));
    }
    if (codPartidas != null && codPartidas.contains(".")) {
      // 0.0.1.0.1 -> 0.0.1
      String padre = codPartidas.replaceFirst("\\.[^.]+$", "");
      return new ParentCode(Optional.empty(), Optional.of(padre));
    }
    // Numérico compacto: 10011 -> 1001
    if (codPartida != null) {
      return new ParentCode(Optional.of(codPartida / 10), Optional.empty());
    }
    return new ParentCode(Optional.empty(), Optional.empty());
  }
}
