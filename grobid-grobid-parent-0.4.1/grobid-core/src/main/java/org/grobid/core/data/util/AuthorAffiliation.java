package org.grobid.core.data.util;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;
import org.grobid.core.utilities.RegexPattern;
import org.grobid.core.utilities.TagName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class AuthorAffiliation {
    BiblioItem biblioItem = new BiblioItem();
    public boolean doesAnyAffiliationHasMarker(List<ITag> sequenceTags) {
        for(ITag sequenceTag: sequenceTags){
            if(sequenceTag.getITagName().equals(TagName.I_AUTHOR) && sequenceTag.getAffiliations() != null) {
                for(Affiliation affiliation: sequenceTag.getAffiliations()){
                    if(affiliation.getMarker() != null){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void associateAuthorsWithAffiliations(List<ITag> sequenceTags, List<Affiliation> identifiedAffiliations, List<Person> identifiedAuthors) {
        for (int authorIndex = 0; authorIndex < sequenceTags.size(); authorIndex++)
            if (sequenceTags.get(authorIndex).getITagName().equals(TagName.I_AUTHOR))
                associateAuthorWithAffiliations(sequenceTags, identifiedAffiliations, identifiedAuthors, authorIndex);
    }

    private void associateAuthorWithAffiliations(List<ITag> sequenceTags, List<Affiliation> identifiedAffiliations, List<Person> identifiedAuthors, int authorIndex) {
        for (int affiliationIndex = authorIndex + 1; affiliationIndex < sequenceTags.size(); affiliationIndex++) {
            if (sequenceTags.get(affiliationIndex).getITagName().equals(TagName.I_AFFILIATION)
                    || sequenceTags.get(affiliationIndex).getITagName().equals(TagName.I_ADDRESS)) {
                Affiliation matchedAffiliations = getFullAffiliation(sequenceTags.get(affiliationIndex).getITagText(), identifiedAffiliations);

                if (matchedAffiliations != null)
                    if (sequenceTags.get(authorIndex).getPersons() != null)
                        for (Person sequenceTagAuthor : sequenceTags.get(authorIndex).getPersons())
                            associateAuthorWithUniqueAffiliations(identifiedAuthors, matchedAffiliations, sequenceTagAuthor);
            } else if (sequenceTags.get(affiliationIndex).getITagName().equals(TagName.I_AUTHOR))
                break;
        }
    }

    private void associateAuthorWithUniqueAffiliations(List<Person> identifiedAuthors, Affiliation identifiedAffiliation, Person sequenceTagAuthor) {
        int index = getIndexOfAuthorFromOriginalList(sequenceTagAuthor, identifiedAuthors);
        if(index != -1) {
            boolean isAffiliationKeyAssocaitedWithAuthor =
                    isAffiliationKeyAlreadyAssociatedWithAuthor(identifiedAuthors, index, identifiedAffiliation);
            if (!isAffiliationKeyAssocaitedWithAuthor)
                identifiedAuthors.get(index).setAffiliations(Arrays.asList(identifiedAffiliation));
        }
    }

    private boolean isAffiliationKeyAlreadyAssociatedWithAuthor(List<Person> identifiedAuthors, int index, Affiliation identifiedAffiliation) {
        if(identifiedAuthors.get(index).getAffiliations() != null) {
            for (Affiliation identifiedAuthorAffiliation : identifiedAuthors.get(index).getAffiliations()) {
                if (identifiedAuthorAffiliation.getKey().equals(identifiedAffiliation.getKey()))
                    return true;
            }
        }
        return false;
    }

    private int getIndexOfAuthorFromOriginalList(Person sequenceTagAuthor, List<Person> identifiedAuthors) {
        for (int index = 0; index < identifiedAuthors.size(); index++) {
           if(biblioItem.isDuplicateAuthor(sequenceTagAuthor, identifiedAuthors.get(index)))
                return index;
        }
        return -1;
    }

    private Affiliation getFullAffiliation(String sequenceAffiliationText, List<Affiliation> identifiedAffiliations) {
        for (Affiliation identifiedAffiliation : identifiedAffiliations) {
             String identifiedAffiliationText = identifiedAffiliation.getText().replaceAll(";", "");
             String sequenceAffiliationMergedTextInLowerCase = sequenceAffiliationText.replaceAll(
                     RegexPattern.SPACE + RegexPattern.PLUS, "").replaceAll(";", "").toLowerCase();
            if (Pattern.matches(identifiedAffiliationText, sequenceAffiliationMergedTextInLowerCase)){
                identifiedAffiliation.setFailAffiliation(false);
                return identifiedAffiliation;
            }
        }
        return null;
    }
}
