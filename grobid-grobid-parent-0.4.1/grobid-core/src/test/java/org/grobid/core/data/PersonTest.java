package org.grobid.core.data;

import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertTrue;

public class PersonTest {
    private static Person person;

    @BeforeClass
    public  static void testSetUp() throws Exception {
        MockContext.setInitialContext();
        GrobidProperties.getInstance();

    }

    @Before
    public void testSetUpPerson() throws Exception {
        person = new Person();
    }
    @Test
    public void Test_getParticle_should_exist() {
       String actual = person.getParticle();
       assertNull(actual);
    }

    @Test
    public void Test_setParticle_should_exist() {
        person.setParticle("particleText");
        String actual = person.getParticle();
        assertTrue(actual.contains("particleText"));
    }

    @Test
    public void Test_toString_should_have_particle_text() {
        person.setParticle("particleText");
        String actual = person.toString();
        assertTrue(actual.contains("particleText"));
    }

    @Test
    public void Test_toTEI_should_have_particle_tag() {
        person.setFirstName("Gajendra");
        person.setLastName("Pratap");
        person.setSuffix("Jr");
        person.setParticle("Van");
        String expected = "<particle>" + person.getParticle() +"</particle>";
        String actual = person.toTEI();
        assertTrue(actual.contains(expected));
    }

    @Test
    public void Test_toTEI_should_have_degree_tag() {
        person.setFirstName("Gajendra");
        person.setLastName("Pratap");
        person.setSuffix("Jr");
        person.setParticle("Van");
        person.setAuthorDegree("M.SC");
        String expected = "<authordegree>" + person.getAuthorDegree() +"</authordegree>";
        String actual = person.toTEI();
        assertTrue(actual.contains(expected));
    }
}
