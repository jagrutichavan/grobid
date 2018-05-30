package org.grobid.core.engines;

import org.grobid.core.data.Person;
import org.grobid.core.factory.AbstractEngineFactory;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AuthorParserTest {

    private static AuthorParser authorParser;
    private static Segmentation target;


    @BeforeClass
    public static void setInitialContext() throws Exception {
        MockContext.setInitialContext();
        AbstractEngineFactory.init();
        authorParser =  new AuthorParser();
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
        MockContext.destroyInitialContext();
    }

    @Before
    public void setUp() throws Exception {
        target = new Segmentation();
    }

    @Test
    public void Test_processing_should_have_suffix() {

        // ...Arrange
        Person person = new Person();
        person.setFirstName("Gajendra");
        person.setLastName("Pratap");
        person.setSuffix("Jr");
        String input = person.getFirstName() + " " + person.getLastName() + " "  + person.getSuffix();
        List<String> inputs = new ArrayList<>();
        inputs.add(input);
        boolean head = true;

        // ...Act
        List<Person> persons = authorParser.processing(inputs,head);


        // ...Assert
        assertEquals(persons.get(0).getSuffix(), person.getSuffix());
    }

}
