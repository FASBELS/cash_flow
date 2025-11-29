package com.dsi.spring.flujoreal.spring_cashflow.utils;

import java.util.Optional;

public final class PartidaHierarchyResolver {

    private PartidaHierarchyResolver() {}

    public static class ParentCode {
        public final Optional<Integer> codPartidaPadreNum;   // si usas numérico
        public final Optional<String>  codPartidasPadreStr;  // si usas texto

        public ParentCode(Optional<Integer> n, Optional<String> s) {
            this.codPartidaPadreNum = n;
            this.codPartidasPadreStr = s;
        }
    }
    public static ParentCode deriveParent(String codPartidas, Integer codPartida) {


        if (codPartidas != null && !codPartidas.isBlank()) {
            int idxGuion = codPartidas.indexOf('-');

            if (idxGuion > 0 && idxGuion < codPartidas.length() - 1) {
                String prefijo = codPartidas.substring(0, idxGuion + 1);  // "ING-"
                String numStr  = codPartidas.substring(idxGuion + 1);     // "000", "100", "101", "1101", etc.

                boolean soloDigitos = numStr.chars().allMatch(Character::isDigit);

                if (soloDigitos) {
                    try {
                        int num = Integer.parseInt(numStr);
                        if (num <= 10) {
                            return new ParentCode(Optional.empty(), Optional.empty());
                        }

                        if (num % 100 == 0) {
                            int grupo = num / 100;        
                            int indiceNivel1 = grupo - 1; 

                            if (indiceNivel1 < 0) {
                                return new ParentCode(Optional.empty(), Optional.empty());
                            }

                            String padreNivel1 = prefijo + String.format("%03d", indiceNivel1);
                            return new ParentCode(Optional.empty(), Optional.of(padreNivel1));
                        }

                        int padre2Num = (num / 100) * 100; 
                        if (padre2Num >= 100) {
                            String padreNivel2 = prefijo + String.format("%03d", padre2Num);
                            return new ParentCode(Optional.empty(), Optional.of(padreNivel2));
                        } else {
                            return new ParentCode(Optional.empty(), Optional.empty());
                        }

                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        if (codPartidas != null && codPartidas.contains("-")) {
            // Quita el último segmento después del guión
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