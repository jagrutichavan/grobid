package org.grobid.core.data.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aman on 12/12/17.
 */
public class FindSuperScript {
    /*
    find superscript based on specific patter of selected terms and numerals
    eg st, nd , rd, th
     */

    public static boolean isNumeral(String str) {
        char[] ch = str.toCharArray();
        if ('0' <= ch[ch.length - 1] && ch[ch.length - 1] <= '9') {
            return true;
        }
        return false;
    }

    public static String FindSSAndModify(String text) {
        String[] tokens = null;
        String [] superScripts = new String[]{"nd", "th", "st", "rd"} ;
        tokens = text.split(" ");
        String tok = "" ;
        StringBuilder sb = new StringBuilder() ;
        boolean found = false ;
        for (int i =0 ; i<tokens.length; i++) {
            tok = tokens[i] ;
            for (String ss : superScripts) {
                String tokTrimmed = tok.replaceAll("\\s*[,.]\\s*$", "") ;
                if (tokTrimmed.equals(ss)) {
                    if(i>0 && isNumeral(tokens[i-1])) {
                        found = true ;
                        sb.deleteCharAt(sb.length()-1) ;
                        sb.append("<superscript>") ;
                        sb.append(tokTrimmed) ;
                        sb.append("</superscript>") ;
                        if (tok.length()>tokTrimmed.length()) {
                            sb.append(tok.substring(tokTrimmed.length())) ;
                        }
                        break ;
                    }
                }
            }
            if (found) {
                found = false ;
                continue;
            } else {
                sb.append(tok) ;
                sb.append(" ") ;
            }
        }
        text = sb.toString().trim() ;
        return text ;
    }
}
