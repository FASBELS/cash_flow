package com.dsi.spring.flujoreal.spring_cashflow.utils;

import java.util.Optional;

public final class PartidaHierarchyResolver {

  private PartidaHierarchyResolver() {}

  public static class ParentCode {
    public final Optional<Integer> codPartidaPadreNum;   // si usas numérico
    public final Optional<String>  codPartidasPadreStr;  // si usas texto
    public ParentCode(Optional<Integer> n, Optional<String> s){ this.codPartidaPadreNum=n; this.codPartidasPadreStr=s; }
  }


  public static ParentCode deriveParent(String codPartidas, Integer codPartida) {
    if (codPartidas != null && codPartidas.contains("-")) {

      String padre = codPartidas.replaceFirst("-[^-]+$", "");
      return new ParentCode(Optional.empty(), Optional.of(padre));
    }
    if (codPartidas != null && codPartidas.contains(".")) {

      String padre = codPartidas.replaceFirst("\\.[^.]+$", "");
      return new ParentCode(Optional.empty(), Optional.of(padre));
    }

    if (codPartida != null) {
      return new ParentCode(Optional.of(codPartida / 10), Optional.empty());
    }
    return new ParentCode(Optional.empty(), Optional.empty());
  }
}
