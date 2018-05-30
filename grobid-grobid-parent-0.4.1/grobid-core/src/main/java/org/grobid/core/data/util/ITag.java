package org.grobid.core.data.util;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import java.util.List;

public class ITag {

    private String ITagName = null;
    private String ITagText = null;
    private int ITagSequence = 0;
    private String ITagLableText = null;
    private List<Person> persons = null;
    private List<Affiliation> affiliations = null;


    public String getITagName() {
        return ITagName;
    }

    public void setITagName(String ITagName) {
        this.ITagName = ITagName;
    }

    public String getITagText() {
        return ITagText;
    }

    public void setITagText(String ITagText) {
        this.ITagText = ITagText;
    }

    public int getITagSequence() {
        return ITagSequence;
    }

    public void setITagSequence(int ITagSequence) {
        this.ITagSequence = ITagSequence;
    }

    public String getITagLableText() {
        return ITagLableText;
    }

    public void setITagLableText(String ITagLableText) {
        this.ITagLableText = ITagLableText;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }

    public List<Affiliation> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<Affiliation> affiliations) {
        this.affiliations = affiliations;
    }
}
