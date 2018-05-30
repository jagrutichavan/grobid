package org.grobid.core.data.util;

import org.grobid.core.data.Person;


public class Marker {

    private String type = null;
    private String text = null;
    private Person person = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

}
