package org.grobid.core.data.util;

import org.grobid.core.utilities.GrobidProperties;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import java.lang.String;
import java.util.regex.*;

/**
 * Created by aman on 20/12/17.
 */
public class Utility {

    public static List<String> conTextualPhrases = null;

    public Utility() {
        loadcf();
    }

    public List<String> getContextualPhrases() {
        return conTextualPhrases;
    }
    public static void loadcf() {
        addContextualPhrase(GrobidProperties.getGrobidHomePath() + File.separator + "contextualText"+File.separator+"corresponding.authors");
    }
    public static void addContextualPhrase(String path) {
        InputStream ins = null;
        InputStreamReader inr = null;
        BufferedReader bf = null;
        File f  = new File(path);
        if (!f.exists() || !f.canRead()) {
            return ;
        }
        try {
            conTextualPhrases = new ArrayList<>();
            ins = new FileInputStream(f);
            inr = new InputStreamReader(ins);
            bf = new BufferedReader(inr);
            String l =null;
            while((l=bf.readLine()) != null) {
                if (l.length() == 0) {
                    continue;
                }
                conTextualPhrases.add(l);
            }
        }
        catch (IOException e) {
            System.out.print("") ;
        }
        finally {
            try {
                if (ins != null) {
                    ins.close();
                }
            }
            catch (Exception e){
                System.out.println("");
            }
        }



    }
    public static boolean inCorrespList(SizedStack<String> tokenStack) {
        String [] tokens =null;
        StringBuilder sb = new StringBuilder();
        for (String tok : tokenStack) {
//            sb.append(tok+" ");
            sb.append(tok);
        }
//        sb.deleteCharAt(sb.length()-1);
        String sourcePhrase = sb.toString();

        boolean found = false;
        if(conTextualPhrases != null) {
            for (String phrase : conTextualPhrases) {
                if (phrase.replace(" ","").toLowerCase().contains(sourcePhrase.toLowerCase())) {
                    found = true;
                    return found;
                } else {
                    continue;
                }

            }
        }
        return found;
    }


    public static boolean doesStringMatchWithPattern(String symbol, String pat){
        Pattern pattern = Pattern.compile(pat);
        Matcher matcher = pattern.matcher(symbol);
        return matcher.matches();
    }
}
