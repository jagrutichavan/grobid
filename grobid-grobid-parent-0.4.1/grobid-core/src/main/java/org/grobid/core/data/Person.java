package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing and exchanging person information, e.g. author or editor.
 *
 * @author Patrice Lopez
 */
public class Person {
    private String firstName = null;
    private String middleName = null;
    private String lastName = null;
    private String title = null;
    private String suffix = null;
    private String prefix = null;
    private String rawName = null; // raw full name if relevant, e.g. name exactly as displayed
    private String particle = null;
    private String authorDegree = null;
    private boolean corresp = false;

    private List<String> affiliationBlocks = null;
    private List<Affiliation> affiliations = null;
    private List<String> affiliationMarkers = null;
    private List<String> markers = null;

    private List<String> email = null;

    private String biography = null;

    private List<String> phones =null;

    private List<String> urls = null;

    private Integer correspondingSeq = 0 ;

    private Integer contextualCorresSeq = 0 ;

    private boolean doesAstrikPresent = false;


    public void copy(Person original){
        this.firstName = original.firstName;
        this.middleName = original.middleName;
        this.lastName = original.lastName;
        this.title = original.title;
        this.suffix = original.suffix;
        this.prefix = original.prefix;
        this.rawName = original.rawName; // raw full name if relevant, e.g. name exactly as displayed
        this.particle = original.particle;
        this.authorDegree = original.authorDegree;
        this.corresp = original.corresp;

        this.affiliationBlocks = original.affiliationBlocks;
        this.affiliations = original.affiliations;
        this.affiliationMarkers = original.affiliationMarkers;
        this.markers = original.markers;

        this.email = original.email;

        this.biography = original.biography;

        this.phones = original.phones;

        this.urls = original.urls;

        this.correspondingSeq = original.correspondingSeq;

        this.contextualCorresSeq = original.contextualCorresSeq;

    }

    public void incrementSeq() {
        correspondingSeq += 1;
    }

    public void setCorrSeq(int i) {
        contextualCorresSeq = i;
    }

    public Integer getSeq() {
        return correspondingSeq;
    }

    public void setCorrespondingSeq(int seq) {
        correspondingSeq = seq;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String f) {
//        firstName = normalizeName(f);
        firstName = f;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String f) {
//        middleName = normalizeName(f);
        middleName = f;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String f) {
//        lastName = normalizeName(f);
        lastName = f;
    }

    public String getRawName() {
         return rawName;
    }

    public void setRawName(String name) {
         rawName = name;
    }

    public String getParticle() {
        return particle;
    }

    public void setParticle(String f) {
//        particle = normalizeName(f);
        particle = f;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String f) {
//        title = normalizeName(f);
        title = f;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String s) {
//        suffix = normalizeName(s);
        suffix = s;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String s) {
        prefix = s;
    }

    public boolean getCorresp() {
        return corresp;
    }

    public void setCorresp(boolean b) {
        corresp = b;
    }

    public String getAuthorDegree() {
        return authorDegree;
    }

    public void setAuthorDegree(String f) {
//        authorDegree = normalizeName(f);
        authorDegree = f;
    }

    public List<String> getAffiliationBlocks() {
        return affiliationBlocks;
    }

    public void addAffiliationBlocks(String f) {
        if (affiliationBlocks == null)
            affiliationBlocks = new ArrayList<String>();
        affiliationBlocks.add(f);
    }

    public List<org.grobid.core.data.Affiliation> getAffiliations() {
        return affiliations;
    }

    public void addAffiliation(org.grobid.core.data.Affiliation f) {
        if (affiliations == null)
            affiliations = new ArrayList<org.grobid.core.data.Affiliation>();
        affiliations.add(f);
    }

    public List<String> getAffiliationMarkers() {
        return affiliationMarkers;
    }

    public void addAffiliationMarker(String s) {
        if (affiliationMarkers == null)
            affiliationMarkers = new ArrayList<String>();
        affiliationMarkers.add(s);
    }

    public void setAffiliations(List<org.grobid.core.data.Affiliation> f) {
        if (affiliations == null)
            affiliations = new ArrayList<Affiliation>();
        affiliations.addAll(f);
    }

    public List<String> getMarkers() {
        return markers;
    }

    public void addMarker(String f) {
        if (markers == null)
            markers = new ArrayList<String>();
		f = f.replace(" ", "");
        markers.add(f);
    }

    public void update (Person p) {
        if (this.email == null && p.getEmail() != null) {
            this.email = p.getEmail();
        }
        if (this.phones == null && p.getPhones() != null) {
            this.phones = p.getPhones();
        }
        if (this.urls == null && p.getUrls() != null) {
            this.urls = p.getUrls();
        }
        if (this.affiliations == null && p.getAffiliations() != null) {
            this.affiliations = p.getAffiliations();
        }
        if (this.affiliationBlocks == null && p.getAffiliationBlocks() != null) {
            this.affiliationBlocks = p.getAffiliationBlocks();
        }
        if (this.particle == null && p.getParticle() != null) {
            this.particle = p.getParticle();
        }
        if (this.authorDegree == null && p.getAuthorDegree() != null) {
            this.authorDegree = p.getAuthorDegree();
        }
        if (this.suffix == null && p.getSuffix() != null) {
            this.suffix = p.getSuffix();
        }
        if (this.title == null && p.getTitle() != null) {
            this.title = p.getTitle();
        }
        if (this.prefix == null && p.getPrefix() != null) {
            this.prefix = p.getPrefix();
        }

        this.corresp = p.getCorresp();
        this.contextualCorresSeq = p.contextualCorresSeq;
    }

    public List<String> getEmail() {
        return email;
    }

    public List<String> getPhones() {return phones;}

    public void setEmail(String f) {
        if (email == null)
            email = new ArrayList<String>();
        email.add(f);
    }

    public void setEmail(List<String> f) {
        if (email == null)
            email = new ArrayList<String>();
        email.addAll(f);
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String s) {
//        biography = normalizeName(s);
        biography = s;
    }

    public void addPhone(String ph) {
        if (phones == null){
            phones = new ArrayList<String>();
        }
        if (!phones.contains(ph)){
            phones.add(ph);
        }
    }

    public List<String> getUrls() {return urls;}

    public void addUrl(String url) {
        if (urls == null){
            urls = new ArrayList<String>();
        }
        if (!urls.contains(url)){
            urls.add(url);
        }
    }

    public boolean notNull() {
        if ((firstName == null) &&
                (middleName == null) &&
                (lastName == null) &&
                (title == null)
                )
            return false;
        else
            return true;
    }

    public String toString() {
        String res = "";
        if (title != null)
            res += title + " ";
        if (prefix != null)
            res += prefix;
        if (firstName != null)
            res += firstName + " ";
        if (middleName != null)
            res += middleName + " ";
        if (lastName != null)
            res += lastName + " ";
        if (suffix != null)
            res += suffix;
        if (particle != null)
            res += particle + " ";
        if (email != null) {
            res += " (email:" + email + ")";
        }
        if (phones != null) {
            res += " (phones:" + phones + ")";
        }
        if (authorDegree != null) {
            res += authorDegree + " ";
        }
        if (biography != null) {
            res += biography + " ";
        }
        return res.trim();
    }

    public String toTEI() {
        if ( (firstName == null) && (middleName == null) &&
                (lastName == null) ) {
            return null;
        }
        String res = "<persName>";
        if (title != null)
            res += "<roleName>" + title + "</roleName>";
        if (prefix != null)
            res += "<prefix>" + prefix + "</prefix>";
        if (firstName != null)
            res += "<forename type=\"first\">" + firstName + "</forename>";
        if (middleName != null)
            res += "<forename type=\"middle\">" + middleName + "</forename>";
        if (lastName != null)
            res += "<surname>" + lastName + "</surname>";
        if (suffix != null)
            res += "<genName>" + suffix + "</genName>";
        if (particle != null)
            res += "<particle>" + particle + "</particle>";
        if (authorDegree != null)
            res += "<authordegree>" + authorDegree + "</authordegree>";
        res += "</persName>";

        return res;
    }

    // list of character delimiters for capitalising names
 	private static final String NAME_DELIMITERS = "-.,;:/_ ";

//    static public String normalizeName(String inputName) {
//		return TextUtilities.capitalizeFully(inputName, NAME_DELIMITERS);
//    }
	
	/**
	 *  Return true if the person structure is a valid person name, in our case
	 *  with at least a lastname or a raw name.
	 */
	public boolean isValid() {
		if ( (lastName == null) && (rawName == null) )
			return false;
		else 
			return true;
	}

    /**
     *
     * person is identified as corresponding author
     * if it has asterisk marker with
     */
	public boolean isCorresponding() {
	    if (corresp == true){
	        return true;
        }
        return doesHaveSymbolicMarker();
    }

    public boolean doesHaveSymbolicMarker() {
        if (markers != null) {
            for (String m : markers) {
                if (m.contains("*")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void modifyAuthoronEnvelop(){
	    //if envelop set corresp flag true and remove it from the names
	    if(firstName != null && (firstName.contains("✉") || doesHaveEnvelopeChar(firstName))) {
            firstName = firstName.trim().replace("✉","");
            firstName = replaceEnvelopeChar(firstName);
	        if(firstName.isEmpty() && middleName != null){
                firstName = middleName.split(" ")[0];
                middleName =  reArrangeName(middleName);
                if(middleName.trim().isEmpty()) {
                    middleName = null;
                }
            } else if(firstName.isEmpty() && lastName != null) {
                firstName = lastName.split(" ")[0];
                lastName.replace(firstName,"");
            }
	        corresp = true;
        } else if(lastName != null && (lastName.contains("✉") || doesHaveEnvelopeChar(lastName))){
            lastName = lastName.trim().replace("✉","");
            lastName = replaceEnvelopeChar(lastName);
            if(firstName.isEmpty()){
                firstName = lastName.split(" ")[0];
                lastName = reArrangeName(lastName);
            }
            corresp = true;
        }

    }

    public boolean doesHaveEnvelopeChar(String name) {

        for(char c: name.toCharArray()){
            if(c == '\uF02A') {
                return true;
            }
        }

        return false;

    }

    public String replaceEnvelopeChar(String name) {
        int index = 0;
        StringBuilder sb = new StringBuilder(name);
        while (index < name.length()) {
            if(name.charAt(index) == '\uF02A') {
                sb.deleteCharAt(index);
            }

            index += 1;
        }

        return sb.toString();
    }

    public String reArrangeName(String name) {
        int index = 0;
        String[] names = name.split(" ");
        String newName = "";
        for (String s : names){

            if(index == 0) {
                names[index] = "";
            } else {
                newName += s;
            }
            index += 1;
        }

        return newName;
    }

    @Override
    public boolean equals(Object o) {

        boolean lExist = false;
        boolean mExist = false;
        boolean fExist = false;

        Person p = (Person) o;
        if (this.getFirstName() != null) {
            if (!this.getFirstName().equals(p.getFirstName())) {
                return false;
            }
            fExist = true;
        }
        if (this.getMiddleName() != null) {
            if (!this.getMiddleName().equals(p.getMiddleName())) {
                return false;
            }
            mExist = true;
        }
        if (this.getLastName() != null) {
            if (!this.getLastName().equals(p.getLastName())) {
                return false;
            }
            lExist = true;
        }
        if (fExist == false && mExist == false && lExist == false) {
            return false;
        }

        return true;
    }

    public void setContectualAuthorSeq(List<Person> contexPer) {
        int i = 1;
        for(Person p: contexPer) {
            if(this.equals(p)) {
                this.setCorrSeq(i);
                this.setCorresp(true);
            }
            i += 1;
        }
    }

    @Override
    public int hashCode() {
	    int hash = 5;
	    hash = 89*hash + (this.getFirstName() != null ? this.getFirstName().hashCode() : 0);
	    hash = 89*hash + (this.getMiddleName() != null ? this.getMiddleName().hashCode() : 0);
	    hash = 89*hash + (this.getLastName() != null ? this.getLastName().hashCode() : 0);
        return hash;
    }

    public boolean getAstrikPresence() {
        return doesAstrikPresent;
    }

    public void setAstrikPresence(boolean hasAstrik) {
        this.doesAstrikPresent = hasAstrik;
    }
}
