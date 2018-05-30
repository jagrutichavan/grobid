package org.grobid.core.data.util;

import org.grobid.core.data.Person;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Created by aman on 24/3/17.
 */
public class AuthorPhoneAssigner {

    private static String flag = "0";
    public static void assign(List<Person> fullAuthors, String phoneEmail){
        String email = phoneEmail.split("\t")[1].toLowerCase();
        String phone = phoneEmail.split("\t")[0];
        if (fullAuthors != null){
            List<String> autEmail = null;
            if(fullAuthors.size() == 1){
                fullAuthors.get(0).addPhone(phone);
            }
            else{
                //look for the email appearing after and before the mail
                //the phone is most likely to be in association with that
                int i = 0;
                for (Person aut:fullAuthors){
                    autEmail = aut.getEmail();
                    if (autEmail != null) {
                        String eml = null;
                        Boolean doesEmailhas = false;
                        for (String e:autEmail) {
                            eml = e.toLowerCase();
                            if (eml.contains(email)) {
                                fullAuthors.get(i).addPhone(phone);
                                doesEmailhas = true;
                                break;
                            }
                        }
                        if(doesEmailhas) {
                            break;
                        }

                    }
                    i++;
                }

            }
        }

    }
    private static String getEmailName(Stack<String> emLines){
        String tok = null;
        StringBuilder emailStr = new StringBuilder();
        String line = null;
        line = emLines.pop();
        emailStr.append(line.split("\t")[0]);
        String preTok ="";

        while (emLines.size()>0){
            line = emLines.pop();
            tok = line.split("\t")[0];
            if (tok =="." | tok =="_" |tok =="-"){
               emailStr.append(tok);
            }
            else if (preTok =="." | preTok =="_" |preTok =="-"){
                emailStr.append(tok);
            }
            else{
                break;
            }
            preTok = tok;
        }
        return emailStr.toString();
    }

    private static String getCorrespondingEmail(Stack<String> follLines,Stack<String> prevLines){
        String line = null;
        String token = null;
        String prevEmail = null;
        String follEmail = null;
        if (prevLines !=null){
            while (prevLines.size()>0){
                line = prevLines.pop();
                token = line.split("\t")[0];
                if (token.contains("@")){
                    prevLines.push(line);
                    prevEmail = getEmailName(prevLines);
                }
            }
        }
        if (follLines != null){
            while (follLines.size()>0){
                line = follLines.pop();
                token = line.split("\t")[0];
                if (token.contains("@")){
                    follLines.push(line);
                    follEmail = getEmailName(follLines);
                }
            }
        }
        if (prevEmail!=null && follEmail==null){
            return prevEmail;
        }
        else if (prevEmail==null && follEmail!=null){
            return follEmail;
        }
        else if (prevEmail!=null && follEmail!=null){
                if (flag.equals("1")){
                    return prevEmail;
                }
                else if (flag.equals("2")){
                    return follEmail;
                }
        }
        return "notFound";
    }
    public static String mapPhoneEmail(String result,String phone) {
        String correspondingEmail = null;
        String phoneString = null;
        Stack<String> follLines = new Stack<String>();
        SizedStack<String> prevLines = new SizedStack<>(10);
        StringTokenizer st = new StringTokenizer(result, "\n");
        String tok = null;
        String line = null;
        StringBuilder sb = null;
        while (st.hasMoreTokens()) {
            line = st.nextToken();
            if (flag.equals("0")){
                if (line.endsWith("I-<email>")){
                    //email has appeared before phone
                    flag = "1";
                }
            }
            if (line.endsWith("I-<phone>")) {
                if (flag.equals("0")){
                    //phone has appeared before email
                    flag = "2";
                }
                if (sb != null) {
                    if (sb.toString().contains(phone.replaceAll("\\s",""))){
                        int i=0;
                        while (st.hasMoreTokens() && i<10){
                            line = st.nextToken();
                            follLines.push(line);
                        }
                        correspondingEmail = getCorrespondingEmail(follLines,prevLines);
                        break;
                    }
                }
                sb = new StringBuilder();
                tok = line.split("\t")[0];
                sb.append(tok);
            } else if (line.endsWith("<phone>")) {
                tok = line.split("\t")[0];
                //if phone doesnt have starting label as I-web instead just web
                if (sb == null){
                    sb = new StringBuilder();
                }
                sb.append(tok);
                //if no label following phone
                if (!st.hasMoreTokens()){
                    if (sb.toString().contains(phone.replaceAll("\\s",""))){
                        correspondingEmail = getCorrespondingEmail(follLines,prevLines);
                        break;
                    }
                }

            } else {
                if (sb != null) {
                    if (sb.toString().contains(phone.replaceAll("\\s",""))){
                        int i=0;
                        while (st.hasMoreTokens() && i<10){
                            line = st.nextToken();
                            follLines.push(line);
                            i++;
                        }
                        correspondingEmail = getCorrespondingEmail(follLines,prevLines);
                        break;
                    }
                    sb = null;
                }
                //keep preceding line history for email lookup
                prevLines.push(line);
            }
        }
        return correspondingEmail;
    }
}
