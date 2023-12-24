package org.keke.chemistry.utils;

import java.util.Objects;

public class calculateColor {
    public static int calculateColor(String formula){
        if (Objects.equals(formula, "H2O")){
            return 0x205FAC;
        }else if(Objects.equals(formula, "")){
            return 0x00000000;
        }else{
            return 0x777777;
        }
    }
}
