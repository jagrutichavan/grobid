package org.grobid.core.data.util;

import org.grobid.core.data.Person;
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * User: zholudev
 * Date: 10/8/14
 */
public class ClassicAuthorEmailAssigner implements AuthorEmailAssigner {

    @Override

    public void assign(List<Person> fullAuthors, List<String> emails) {
        List<Integer> winners = new ArrayList<Integer>();

        // if 1 email and 1 author, not too hard...
        if (fullAuthors != null) {
            if ((emails.size() == 1) && (fullAuthors.size() == 1)) {
                fullAuthors.get(0).setEmail(emails.get(0));
            } else {
                // we asociate emails to the authors based on string proximity
                for (String mail : emails) {
                    int maxDist = 1000;
                    int best = -1;
                    int ind = mail.indexOf("@");
                    if (ind != -1) {
                        String nam = mail.substring(0, ind).toLowerCase();
                        int k = 0;
                        for (Person aut : fullAuthors) {
                            Integer kk = k;
                            if (!winners.contains(kk)) {
                                List<String> emailVariants = TextUtilities.generateEmailVariants(aut.getFirstName(), aut.getLastName());

                                for (String variant : emailVariants) {
                                    variant = variant.toLowerCase();

                                    int dist = TextUtilities.getLevenshteinDistance(nam, variant);
                                    if (dist < maxDist) {
                                        best = k;
                                        maxDist = dist;
                                    }
                                }
                            }
                            k++;
                        }

                        // make sure that the best candidate found is not too far
                        if (best != -1 && maxDist < nam.length() / 2) {
                            Person winner = fullAuthors.get(best);
                            winner.setEmail(mail);
                            winners.add(best);
                        } else if(winners.size() < emails.size()) {
                            int prevMatchCount = 0;
                            int matchCharCount = 0;
                            int perIndex = -1;
                            int matchinIndex = -1;
                            for(Person per: fullAuthors){ // check if firstName, middleName and lastName match
                                matchCharCount = 0;

                                if(per.getFirstName() != null && per.getFirstName().length() > 1 && nam.contains(per.getFirstName().toLowerCase())){
                                    matchCharCount += per.getFirstName().length();
                                }
                                if(per.getMiddleName() !=null && per.getMiddleName().length() > 1 && nam.contains(per.getMiddleName().toLowerCase())){
                                    matchCharCount += per.getMiddleName().length();
                                }
                                if(per.getLastName() !=null && per.getLastName().length() > 1 && nam.contains(per.getLastName().toLowerCase())){
                                    matchCharCount += per.getLastName().length();
                                }
                                perIndex += 1;

                                if(matchCharCount > prevMatchCount) {
                                    matchinIndex = perIndex;
                                    prevMatchCount = matchCharCount;
                                }
                            }

                            if(matchinIndex != -1) {
                                Person winner = fullAuthors.get(matchinIndex);
                                winner.setEmail(mail);
                                winners.add(matchinIndex);
                            }
                        }
                    }
                }

            }
        }
    }
}
