package org.grobid.core.data;

import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BiblioItemTest {

    private static BiblioItem biblioItem;

    @BeforeClass
    public static void testSetUp() throws Exception {
        MockContext.setInitialContext();
        GrobidProperties.getInstance();
    }

    @Before
    public void testSetUpBiblio() throws Exception {
        biblioItem = new BiblioItem();
    }

    @Test
    public void Test_toTEIAuthorBlock_should_have_suffix_tag() {

        // ... Arrange
        int nTb = 6;
        Person person = new Person();
        person.setFirstName("Gajendra");
        person.setLastName("Pratap");
        person.setSuffix("Jr");
        String expectedResult = "<suffix>" + person.getSuffix() + "</suffix>";
        List<Person> fullAuthors = new ArrayList<>();
        fullAuthors.add(person);
        biblioItem.setFullAuthors(fullAuthors);

        // ... Act
        String actualAuthorBlock = biblioItem.toTEIAuthorBlock(nTb);

        // ... Assert
        assertTrue(actualAuthorBlock.contains(expectedResult));
    }

    @Test
    public void Test_toTEIAuthorBlock_should_have_particle_tag() {

        // ... Arrange
        int nTb = 6;
        Person person = new Person();
        person.setFirstName("Gajendra");
        person.setLastName("Pratap");
        person.setSuffix("Jr");
        person.setParticle("Van");
        String expectedResult = "<particle>" + person.getParticle() + "</particle>";
        List<Person> fullAuthors = new ArrayList<>();
        fullAuthors.add(person);
        biblioItem.setFullAuthors(fullAuthors);

        // ... Act
        String actualAuthorBlock = biblioItem.toTEIAuthorBlock(nTb);

        // ... Assert
        assertTrue(actualAuthorBlock.contains(expectedResult));
    }

    @Test
    public void Test_toTEIAuthorBlock_should_have_authorbiography_tag() {

        // ... Arrange
        int nTb = 6;
        Person person = new Person();
        person.setFirstName("Gajendra");
        person.setLastName("Pratap");
        person.setSuffix("Jr");
        person.setParticle("Van");
        person.setBiography("biographytext");
        String expectedResult = "<authorbiography>" + person.getBiography() + "</authorbiography>";
        List<Person> fullAuthors = new ArrayList<>();
        fullAuthors.add(person);
        biblioItem.setFullAuthors(fullAuthors);

        // ... Act
        String actualAuthorBlock = biblioItem.toTEIAuthorBlock(nTb);

        // ... Assert
        assertTrue(actualAuthorBlock.contains(expectedResult));
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_and_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setLastName("Guvenc");

        Boolean isduplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isduplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_but_different_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setLastName("Guven");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setLastName("Guvenc");

        Boolean isduplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isduplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_have_first_name_but_unique_author_have_first_name_and_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setLastName("Guvenc");

        Boolean isduplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isduplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_have_first_name_and_last_name_but_unique_author_have_only_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setLastName("Guvenc");

        Boolean isduplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isduplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_middle_name_and_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("abc");
        newAuthor.setLastName("Guvenc");

        Boolean isduplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isduplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_and_last_name_but_middle_name_is_different() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("Abcd");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_have_first_name_last_name_and_middle_name_but_unique_author_have_first_name_and_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_middle_name_and_last_name_but_new_author_particle_has_dot_and_unique_do_not_have() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Van.");
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setParticle("van");
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("abc");
        newAuthor.setLastName("Guvenc");

        Boolean isduplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isduplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_last_name_and_middle_name_but_particle_is_different() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Van");
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setParticle("Von.");
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("Abc");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_last_name_and_middle_name_but_particle_is_present_at_new_author_only() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Van.");
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("Abc");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_middle_name_last_name_particle_and_suffix() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setSuffix("Jr.");
        uniqueAuthor.setParticle("Van");
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setSuffix("Jr");
        newAuthor.setParticle("Va'n");
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("Abc");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name_middle_name_last_name_and_particle_but_suffix_is_different() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setSuffix("Jr.");
        uniqueAuthor.setParticle("Van");
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setSuffix("Sr");
        newAuthor.setParticle("Va'n");
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("Abc");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_have_first_name_middle_name_last_name_particle_and_suffix_and_unique_author_have_first_name_middle_name_last_name_and_particle() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Van");
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setMiddleName("Abc");
        uniqueAuthor.setLastName("Guvenc");

        Person newAuthor = new Person();
        newAuthor.setSuffix("Sr");
        newAuthor.setParticle("Va'n");
        newAuthor.setFirstName("Gulten");
        newAuthor.setMiddleName("Abc");
        newAuthor.setLastName("Guvenc");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_first_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_different_first_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulte");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_middle_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setMiddleName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setMiddleName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_different_middle_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setMiddleName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setMiddleName("Gulte");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_different_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulte");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_suffix() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setSuffix("Gulten");

        Person newAuthor = new Person();
        newAuthor.setSuffix("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_suffix_and_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");
        uniqueAuthor.setSuffix("Jr.");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");
        newAuthor.setSuffix("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_suffix_and_first_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setSuffix("Jr.");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setSuffix("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_suffix_and_middle_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setMiddleName("Gulten");
        uniqueAuthor.setSuffix("Jr.");

        Person newAuthor = new Person();
        newAuthor.setMiddleName("Gulten");
        newAuthor.setSuffix("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_suffix_and_particle() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Gulten");
        uniqueAuthor.setSuffix("Jr.");

        Person newAuthor = new Person();
        newAuthor.setParticle("Gulten");
        newAuthor.setSuffix("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_different_suffix() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setSuffix("Gulten");

        Person newAuthor = new Person();
        newAuthor.setSuffix("Gulte");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_particle() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Gulten");

        Person newAuthor = new Person();
        newAuthor.setParticle("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_particle_and_last_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");
        uniqueAuthor.setParticle("Jr.");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");
        newAuthor.setParticle("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_particle_and_first_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setFirstName("Gulten");
        uniqueAuthor.setParticle("Jr.");

        Person newAuthor = new Person();
        newAuthor.setFirstName("Gulten");
        newAuthor.setParticle("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_pasrticle_and_middle_name() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setMiddleName("Gulten");
        uniqueAuthor.setParticle("Jr.");

        Person newAuthor = new Person();
        newAuthor.setMiddleName("Gulten");
        newAuthor.setParticle("Jr.");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_different_particle() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setParticle("Gulten");

        Person newAuthor = new Person();
        newAuthor.setParticle("Gulte");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertFalse(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_space_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_space_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_end_preiod_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul.ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_end_period_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul.ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_colon_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul:ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_colon_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul:ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_comma_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul,ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_comma_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul,ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_semicolon_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul;ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_semicolon_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul;ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_hyphen_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul-ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_hyphen_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul-ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_n_dash_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul–ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_n_dash_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul–ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_em_dash_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul—ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_em_dash_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul—ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_single_straight_quote_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul'ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_single_straight_quote_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul'ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_single_opening_quote_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul‘ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_single_opening_quote_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul‘ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_new_author_have_single_closing_quote_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gulten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gul’ten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    @Test
    public void test_isDuplicateAuthor_when_new_author_and_unique_author_have_same_last_name_but_unique_author_have_single_closing_quote_in_between() {
        Person uniqueAuthor = new Person();
        uniqueAuthor.setLastName("Gul’ten");

        Person newAuthor = new Person();
        newAuthor.setLastName("Gulten");

        Boolean isDuplicateAuthor = biblioItem.isDuplicateAuthor(newAuthor, uniqueAuthor);

        assertTrue(isDuplicateAuthor);
    }

    private void After(List<Person> expectedAuthors) {
        List<Person> fullAuthors = biblioItem.getFullAuthors();
        for (int i = 0; i < fullAuthors.size(); i++) {
            Person uniqueAuthor = fullAuthors.get(i);
            Person expectedAuthor = expectedAuthors.get(i);
            if (expectedAuthor.getPrefix() != null) {
                assertNotNull(uniqueAuthor.getPrefix());
                assertEquals(expectedAuthor.getPrefix(), uniqueAuthor.getPrefix());
            }

            if (expectedAuthor.getAuthorDegree() != null) {
                assertNotNull(uniqueAuthor.getAuthorDegree());
                assertEquals(expectedAuthor.getAuthorDegree(), uniqueAuthor.getAuthorDegree());
            }
        }
    }

    @Test
    public void test_addUniqueAuthor_when_duplicate_author_is_present_and_prefix_is_present_in_new_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");
        newAuthor.setPrefix("Mr.");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(newAuthor);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_duplicate_author_is_present_and_prefix_is_not_present_in_new_author_but_present_in_unique_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");
        uniqueAuthor2.setPrefix("Mrs.");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(uniqueAuthor2);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_duplicate_author_is_present_and_degree_is_present_in_new_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");
        newAuthor.setAuthorDegree("Dr., Prof");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(newAuthor);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_duplicate_author_is_present_and_degree_is_not_present_in_new_author_but_present_in_unique_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");
        uniqueAuthor2.setAuthorDegree("Dr., Prof");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(uniqueAuthor2);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_there_is_duplicate_author_and_prefix_and_degree_is_present_only_in_new_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");
        newAuthor.setPrefix("Mr.");
        newAuthor.setAuthorDegree("Dr., Prof");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(newAuthor);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_there_is_duplicate_author_and_prefix_is_present_at_unique_author_and_degree_is_present_at_new_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");
        uniqueAuthor2.setPrefix("Mr.");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");
        newAuthor.setAuthorDegree("Dr., Prof");

        Person expectedAuthor = new Person();
        expectedAuthor.setLastName("Abc");
        expectedAuthor.setAuthorDegree("Dr., Prof");
        expectedAuthor.setPrefix("Mr.");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(expectedAuthor);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_there_is_duplicate_author_and_degree_is_present_at_unique_author_and_prefix_is_present_at_new_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");
        uniqueAuthor2.setAuthorDegree("Dr., Prof");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");
        newAuthor.setPrefix("Mr.");

        Person expectedAuthor = new Person();
        expectedAuthor.setLastName("Abc");
        expectedAuthor.setAuthorDegree("Dr., Prof");
        expectedAuthor.setPrefix("Mr.");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(expectedAuthor);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void test_addUniqueAuthor_when_there_is_duplicate_author_and_prefix_and_degree_is_present_only_in_unique_author() {
        List<Person> fullAuthors = new ArrayList<>();
        List<Person> expectedAuthors = new ArrayList<>();

        Person uniqueAuthor1 = new Person();
        uniqueAuthor1.setLastName("Gulten");

        Person uniqueAuthor2 = new Person();
        uniqueAuthor2.setLastName("Abc");
        uniqueAuthor2.setPrefix("Mr.");
        uniqueAuthor2.setAuthorDegree("Dr., Prof");

        fullAuthors.add(uniqueAuthor1);
        fullAuthors.add(uniqueAuthor2);

        biblioItem.setFullAuthors(fullAuthors);

        Person newAuthor = new Person();
        newAuthor.setLastName("Abc");

        expectedAuthors.add(uniqueAuthor1);
        expectedAuthors.add(uniqueAuthor2);

        biblioItem.addUniqueAuthor(newAuthor);

        After(expectedAuthors);
    }

    @Test
    public void Test_doesAnyAuthorHasAstrik_attribute() {

        //...Act
        biblioItem.setdoesAnyAuthorHasAstrik(true);

        //...Assert
        assertTrue(biblioItem.doesAnyAuthorHasAstrik());
    }

    @Test
    public void Test_doesHasContextualWord_attribute() {

        //...Act
        biblioItem.setContextualWord(true);

        //...Assert
        assertTrue(biblioItem.doesHaveContextualWord());
    }

    @Test
    public void Test_corresponding_authors_when_authors_have_incremented_symbol_marker_and_email_has_same_marker_and_have_contextual() {

        /*
            Author 1*, Author 2
            Correspondence email:
            *123@gmail.com
            456@gmail.com
         */

        //...Arrange
        List<Person> fullAuthors = new ArrayList<>();

        Person person1 = new Person();
        person1.setFirstName("Brad");
        person1.setLastName("Forenza");
        person1.addMarker("1");
        person1.addMarker(",");
        person1.addMarker("2");
        person1.addMarker(",");
        person1.addMarker("*");
        fullAuthors.add(person1);

        Person person2 = new Person();
        person2.setFirstName("Autumn");
        person2.setLastName("Bermea");
        person2.addMarker("1");
        fullAuthors.add(person2);

        biblioItem.setFullAuthors(fullAuthors);

        biblioItem.setContextualWord(true);
        biblioItem.setCorresEmails("email:*brfo@mail.montclair.edu,aube@mail.montclair.edu");
        biblioItem.setEmail("email: * brfo@mail.montclair.edu, ; aube@mail.montclair.edu");

        String expectedPerson1Email1 = "brfo@mail.montclair.edu";
        String expectedPerson1Email2 = "aube@mail.montclair.edu";


        //...Act
        biblioItem.attachEmails();
        biblioItem.checkAuthorMarkerHasAstrik();
        biblioItem.setCorrespondingAuthor();

        String actualPerson1Email1 = biblioItem.getFullAuthors().get(0).getEmail().get(0);
        String actualPerson1Email2 = biblioItem.getFullAuthors().get(0).getEmail().get(1);
        List<String> actualPerson2Email = biblioItem.getFullAuthors().get(1).getEmail();

        int expectedPerson1CorrSeq = 1;
        int expectedPerson2CorrSeq = 0;

        //...Assert

        assertEquals("first person mail is not equal to expected email", expectedPerson1Email1, actualPerson1Email1);
        assertEquals("first person mail is not equal to expected email", expectedPerson1Email2, actualPerson1Email2);
        assertNull("second person mail is not equal to expected email", actualPerson2Email);


        assertTrue("first person corresp attrubute is not true", biblioItem.getFullAuthors().get(0).getCorresp());
        assertFalse("second person corresp attrubute is true", biblioItem.getFullAuthors().get(1).getCorresp());


        assertEquals("first person correspseq attrubute is not equal to 1", expectedPerson1CorrSeq, biblioItem.getFullAuthors().get(0).getSeq().intValue());
        assertEquals("second person has correspseq attrubute", expectedPerson2CorrSeq, biblioItem.getFullAuthors().get(1).getSeq().intValue());

    }

    @Test
    public void Test_corresponding_authors_when_authors_have_incremented_symbol_marker_and_email_has_same_marker_and_not_have_contextual() {

        /*
            Author 1*, Author 2**, Author 3***
            *Email: 123@gmail.com
            **Email: 456@gmail.com
            ***Email: 789@gmail.com

         */

        //...Arrange
        List<Person> fullAuthors = new ArrayList<>();
        Person person1 = new Person();
        person1.setFirstName("Brad");
        person1.setLastName("Forenza");
        person1.addMarker("1");
        person1.addMarker(",");
        person1.addMarker("*");
        fullAuthors.add(person1);

        Person person2 = new Person();
        person2.setFirstName("Autumn");
        person2.setLastName("Bermea");
        person2.addMarker("1");
        person2.addMarker(",");
        person2.addMarker("*");
        person2.addMarker("*");
        fullAuthors.add(person2);

        Person person3 = new Person();
        person3.setFirstName("Briana");
        person3.setLastName("Rogers");
        person3.addMarker("1");
        person3.addMarker(",");
        person3.addMarker("*");
        person3.addMarker("*");
        person3.addMarker("*");
        fullAuthors.add(person3);

        biblioItem.setFullAuthors(fullAuthors);

        biblioItem.setEmail("* brfo@mail.montclair.edu, ; ** aube@mail.montclair.edu *** brro@mail.montclair.edu");


        //...Act
        biblioItem.attachEmails();
        biblioItem.checkAuthorMarkerHasAstrik();
        biblioItem.setCorrespondingAuthor();

        String expectedPerson1Email = "brfo@mail.montclair.edu";
        String expectedPerson2Email = "aube@mail.montclair.edu";
        String expectedPerson3Email = "brro@mail.montclair.edu";

        String actualPerson1Email = biblioItem.getFullAuthors().get(0).getEmail().get(0);
        String actualPerson2Email = biblioItem.getFullAuthors().get(1).getEmail().get(0);
        String actualPerson3Email = biblioItem.getFullAuthors().get(2).getEmail().get(0);

        int expectedPerson1CorrSeq = 1;
        int expectedPerson2CorrSeq = 2;
        int expectedPerson3CorrSeq = 3;

        //...Assert

        assertEquals("first person mail is not equal to expected email", expectedPerson1Email, actualPerson1Email);
        assertEquals("second person mail is not equal to expected email", expectedPerson2Email, actualPerson2Email);
        assertEquals("third person mail is not equal to expected email", expectedPerson3Email, actualPerson3Email);


        assertTrue("first person corresp attrubute is not true", biblioItem.getFullAuthors().get(0).getCorresp());
        assertTrue("second person corresp attrubute is not true", biblioItem.getFullAuthors().get(1).getCorresp());
        assertTrue("third person corresp attrubute is not true", biblioItem.getFullAuthors().get(2).getCorresp());


        assertEquals("first person correspseq attrubute is not equal to 1", expectedPerson1CorrSeq, biblioItem.getFullAuthors().get(0).getSeq().intValue());
        assertEquals("second person correspseq attrubute is not equal to 2", expectedPerson2CorrSeq, biblioItem.getFullAuthors().get(1).getSeq().intValue());
        assertEquals("third person correspseq attrubute is not equal to 3", expectedPerson3CorrSeq, biblioItem.getFullAuthors().get(2).getSeq().intValue());

    }

    @Test
    public void Test_corresponding_authors_when_authors_have_no_symbol_marker_and_email_match_with_author_and_have_contextual() {

        /*
           Author 1, author 2
           Corresponding email:
           Author1@gmail.com
           Author2@gmail.com
         */

        //...Arrange
        List<Person> fullAuthors = new ArrayList<>();
        Person person1 = new Person();
        person1.setFirstName("Brad");
        person1.setLastName("Forenza");
        person1.addMarker("1");
        person1.addMarker(",");
        person1.addMarker("2");
        fullAuthors.add(person1);

        Person person2 = new Person();
        person2.setFirstName("Autumn");
        person2.setLastName("Bermea");
        person2.addMarker("1");
        fullAuthors.add(person2);

        Person person3 = new Person();
        person3.setFirstName("Briana");
        person3.setLastName("Rogers");
        person3.addMarker("1");
        fullAuthors.add(person3);

        biblioItem.setFullAuthors(fullAuthors);

        biblioItem.setContextualWord(true);
        biblioItem.setCorresEmails("email:bradforenza@mail.montclair.edu,autumnbermea@mail.montclair.edu");
        biblioItem.setEmail("email:bradforenza@mail.montclair.edu,autumnbermea@mail.montclair.edu");

        String expectedPerson1Email = "bradforenza@mail.montclair.edu";
        String expectedPerson2Email = "autumnbermea@mail.montclair.edu";


        //...Act
        biblioItem.attachEmails();
        biblioItem.checkAuthorMarkerHasAstrik();
        biblioItem.setCorrespondingAuthor();

        String actualPerson1Email = biblioItem.getFullAuthors().get(0).getEmail().get(0);
        String actualPerson2Email = biblioItem.getFullAuthors().get(1).getEmail().get(0);
        List<String> actualPerson3Email = biblioItem.getFullAuthors().get(2).getEmail();

        int expectedPerson1CorrSeq = 1;
        int expectedPerson2CorrSeq = 2;
        int expectedPerson3CorrSeq = 0;

        //...Assert

        assertEquals("first person mail is not equal to expected email", expectedPerson1Email, actualPerson1Email);
        assertEquals("second person mail is not equal to expected email", expectedPerson2Email, actualPerson2Email);
        assertNull("third person mail is not equal to expected email", actualPerson3Email);


        assertTrue("first person corresp attrubute is not true", biblioItem.getFullAuthors().get(0).getCorresp());
        assertTrue("second person corresp attrubute is not true", biblioItem.getFullAuthors().get(1).getCorresp());
        assertFalse("third person corresp attrubute is true", biblioItem.getFullAuthors().get(2).getCorresp());


        assertEquals("first person correspseq attrubute is not equal to 1", expectedPerson1CorrSeq, biblioItem.getFullAuthors().get(0).getSeq().intValue());
        assertEquals("second person correspseq attrubute is not equal to 1", expectedPerson2CorrSeq, biblioItem.getFullAuthors().get(1).getSeq().intValue());
        assertEquals("third person has correspseq attrubute", expectedPerson3CorrSeq, biblioItem.getFullAuthors().get(2).getSeq().intValue());

    }

    @Test
    public void Test_corresponding_authors_when_authors_have_no_symbol_marker_and_email_not_match_with_author_and_have_contextual() {

        /*
           Author 1, author 2
           Corresponding email:
           Author1@gmail.com
           Author2@gmail.com
         */

        //...Arrange
        List<Person> fullAuthors = new ArrayList<>();
        Person person1 = new Person();
        person1.setFirstName("Brad");
        person1.setLastName("Forenza");
        person1.addMarker("1");
        person1.addMarker(",");
        person1.addMarker("2");
        fullAuthors.add(person1);

        Person person2 = new Person();
        person2.setFirstName("Autumn");
        person2.setLastName("Bermea");
        person2.addMarker("1");
        fullAuthors.add(person2);

        Person person3 = new Person();
        person3.setFirstName("Briana");
        person3.setLastName("Rogers");
        person3.addMarker("1");
        fullAuthors.add(person3);

        biblioItem.setFullAuthors(fullAuthors);

        biblioItem.setContextualWord(true);
        biblioItem.setCorresEmails("email:123@gmail.com,456@gmail.com");
        biblioItem.setEmail("email:123@gmail.com,456@gmail.com");


        //...Act
        biblioItem.attachEmails();
        biblioItem.checkAuthorMarkerHasAstrik();
        biblioItem.setCorrespondingAuthor();

        List<String> actualPerson1Email = biblioItem.getFullAuthors().get(0).getEmail();
        List<String> actualPerson2Email = biblioItem.getFullAuthors().get(1).getEmail();
        List<String> actualPerson3Email = biblioItem.getFullAuthors().get(2).getEmail();

        int expectedPersonCorrSeq = 0;

        //...Assert

        assertNull("first person does not have any mail", actualPerson1Email);
        assertNull("second person does not have any mail", actualPerson2Email);
        assertNull("third person does not have any mail", actualPerson3Email);


        assertFalse("first person is corresponding author", biblioItem.getFullAuthors().get(0).getCorresp());
        assertFalse("second person is corresponding author", biblioItem.getFullAuthors().get(1).getCorresp());
        assertFalse("second person is corresponding author", biblioItem.getFullAuthors().get(2).getCorresp());


        assertEquals("first person has correspseq attrubute", expectedPersonCorrSeq, biblioItem.getFullAuthors().get(0).getSeq().intValue());
        assertEquals("second person has correspseq attrubute", expectedPersonCorrSeq, biblioItem.getFullAuthors().get(1).getSeq().intValue());
        assertEquals("third person has correspseq attrubute", expectedPersonCorrSeq, biblioItem.getFullAuthors().get(2).getSeq().intValue());

    }

    @Test
    public void Test_corresponding_authors_when_authors_have_no_symbol_marker_and_email_match_with_author_and_have_one_author_with_contextual() {

        /*
           Author 1, Author 2
           Corresponding email:
           Author1@gmail.com
           Author2@gmail.com
           Author 3
           Author3@gmail.com
         */

        //...Arrange
        List<Person> fullAuthors = new ArrayList<>();
        Person person1 = new Person();
        person1.setFirstName("Fusun");
        person1.setLastName("Terzioglu");
        fullAuthors.add(person1);

        Person person2 = new Person();
        person2.setFirstName("Gulsah");
        person2.setLastName("Kok");
        fullAuthors.add(person2);

        Person person3 = new Person();
        person3.setFirstName("Gulten");
        person3.setLastName("Guvenc");
        fullAuthors.add(person3);

        biblioItem.setFullAuthors(fullAuthors);

        biblioItem.setContextualWord(true);
        biblioItem.setCorresEmails("E-mail:fusunterzioglu@gmail.comE-mail:gulsah.kok@sbu.edu.trE-mail:gulten.guvenc@sbu.edu.tr");
        biblioItem.setEmail("E-mail:fusunterzioglu@gmail.com E-mail: gulsah.kok@sbu.edu.tr ; E-mail: gulten.guvenc@sbu.edu.tr");

        String expectedPerson1Email = "fusunterzioglu@gmail.com";
        String expectedPerson2Email = "gulsah.kok@sbu.edu.tr";
        String expectedPerson3Email = "gulten.guvenc@sbu.edu.tr";


        //...Act
        biblioItem.attachEmails();
        biblioItem.checkAuthorMarkerHasAstrik();
        biblioItem.setCorrespondingAuthor();

        String actualPerson1Email = biblioItem.getFullAuthors().get(0).getEmail().get(0);
        String actualPerson2Email = biblioItem.getFullAuthors().get(1).getEmail().get(0);
        String actualPerson3Email = biblioItem.getFullAuthors().get(2).getEmail().get(0);

        int expectedPerson1CorrSeq = 1;
        int expectedPerson2CorrSeq = 2;
        int expectedPerson3CorrSeq = 3;

        //...Assert

        assertEquals("first person mail is not equal to expected email", expectedPerson1Email, actualPerson1Email);
        assertEquals("second person mail is not equal to expected email", expectedPerson2Email, actualPerson2Email);
        assertEquals("third person mail is not equal to expected email", expectedPerson3Email, actualPerson3Email);


        assertTrue("first person corresp attrubute is not true", biblioItem.getFullAuthors().get(0).getCorresp());
        assertTrue("second person corresp attrubute is not true", biblioItem.getFullAuthors().get(1).getCorresp());
        assertTrue("third person corresp attrubute is not true", biblioItem.getFullAuthors().get(2).getCorresp());


        assertEquals("first person correspseq attrubute is not equal to 1", expectedPerson1CorrSeq, biblioItem.getFullAuthors().get(0).getSeq().intValue());
        assertEquals("second person correspseq attrubute is not equal to 2", expectedPerson2CorrSeq, biblioItem.getFullAuthors().get(1).getSeq().intValue());
        assertEquals("third person has attrubute is not equal to 3", expectedPerson3CorrSeq, biblioItem.getFullAuthors().get(2).getSeq().intValue());

    }

    @Test
    public void Test_corresponding_authors_when_authors_has_no_symbol_marker_and_not_have_contextual() {

        /*
           Author 1, Author 2, Author 3
           Author1@gmail.com
           Author2@gmail.com
           Author3@gmail.com
         */

        //...Arrange
        List<Person> fullAuthors = new ArrayList<>();
        Person person1 = new Person();
        person1.setFirstName("Fusun");
        person1.setLastName("Terzioglu");
        fullAuthors.add(person1);

        Person person2 = new Person();
        person2.setFirstName("Gulsah");
        person2.setLastName("Kok");
        fullAuthors.add(person2);

        Person person3 = new Person();
        person3.setFirstName("Gulten");
        person3.setLastName("Guvenc");
        fullAuthors.add(person3);

        biblioItem.setFullAuthors(fullAuthors);


        biblioItem.setEmail("E-mail:fusunterzioglu@gmail.com E-mail: gulsah.kok@sbu.edu.tr ; E-mail: gulten.guvenc@sbu.edu.tr");


        String expectedPerson1Email = "fusunterzioglu@gmail.com";
        String expectedPerson2Email = "gulsah.kok@sbu.edu.tr";
        String expectedPerson3Email = "gulten.guvenc@sbu.edu.tr";


        //...Act
        biblioItem.attachEmails();
        biblioItem.checkAuthorMarkerHasAstrik();
        biblioItem.setCorrespondingAuthor();


        String actualPerson1Email = biblioItem.getFullAuthors().get(0).getEmail().get(0);
        String actualPerson2Email = biblioItem.getFullAuthors().get(1).getEmail().get(0);
        String actualPerson3Email = biblioItem.getFullAuthors().get(2).getEmail().get(0);

        int expectedPersonCorrSeq = 0;

        //...Assert

        assertEquals("first person mail is not match expected", expectedPerson1Email, actualPerson1Email);
        assertEquals("second person mail is not match expected", expectedPerson2Email, actualPerson2Email);
        assertEquals("third person mail is not match expected", expectedPerson3Email, actualPerson3Email);


        assertFalse("first person corresp attrubute is not true", biblioItem.getFullAuthors().get(0).getCorresp());
        assertFalse("second person corresp attrubute is not true", biblioItem.getFullAuthors().get(1).getCorresp());
        assertFalse("third person corresp attrubute is not true", biblioItem.getFullAuthors().get(2).getCorresp());


        assertEquals("first person correspseq attrubute is not equal to 0", expectedPersonCorrSeq, biblioItem.getFullAuthors().get(0).getSeq().intValue());
        assertEquals("second person correspseq attrubute is not equal to 0", expectedPersonCorrSeq, biblioItem.getFullAuthors().get(1).getSeq().intValue());
        assertEquals("third person correspseq attrubute is not equal to 0", expectedPersonCorrSeq, biblioItem.getFullAuthors().get(2).getSeq().intValue());

    }

    @Test
    public void test_isCountryUnique_when_country_name_is_not_present_in_new_affiliation_and_unique_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_country_name_is_present_in_new_affiliation_but_not_in_unique_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("India");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_country_name_is_present_in_unique_affiliation_but_not_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setCountry("India");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_country_names_are_exact_match() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("India");
        uniqueAffiliation.setCountry("India");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_uk_and_unique_affiliation_country_is_united_kingdom() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Uk");
        uniqueAffiliation.setCountry("United Kingdom");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_uk_and_unique_affiliation_country_is_us() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Uk");
        uniqueAffiliation.setCountry("US");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_abc_and_unique_affiliation_country_is_us() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Abc");
        uniqueAffiliation.setCountry("US");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_us_and_unique_affiliation_country_is_abc() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("US");
        uniqueAffiliation.setCountry("Abc");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_xyz_and_unique_affiliation_country_is_abc() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Xyz");
        uniqueAffiliation.setCountry("Abc");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_pr_china_and_unique_affiliation_country_is_china() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("PR china");
        uniqueAffiliation.setCountry("China");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_p_dot_r_dot_china_and_unique_affiliation_country_is_china() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("P. R. china");
        uniqueAffiliation.setCountry("China");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_USA_and_unique_affiliation_country_is_united_states_of_america() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("U.S.");
        uniqueAffiliation.setCountry("United states of America");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    //vereinigte arabische emirate

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_vereinigte_arabische_emirate_and_unique_affiliation_country_is_uae() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Vereinigte Arabische Emirate");
        uniqueAffiliation.setCountry("UAE");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isCountryUnique_when_new_affiliation_country_is_vereinigte_arabische_emirate_and_unique_affiliation_country_is_usa() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Vereinigte Arabische Emirate");
        uniqueAffiliation.setCountry("USA");

        boolean isCountryUnique = biblioItem.isCountryUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_country_is_different_from_unique_affiliation_country() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Vereinigte Arabische Emirate");
        uniqueAffiliation.setCountry("USA");

        boolean isCountryUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_country_is_same_as_unique_affiliation_country() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("People's republic of China");
        uniqueAffiliation.setCountry("China");

        boolean isCountryUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_country_is_people_apostrophe_s_republic_of_china_and_unique_affiliation_country_is_china() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("People's republic of China");
        uniqueAffiliation.setCountry("China");

        boolean isCountryUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_country_is_peoples_republic_of_china_dot_and_unique_affiliation_country_is_china() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Peoples republic of China.");
        uniqueAffiliation.setCountry("China");

        boolean isCountryUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_country_is_hong_hyphen_kong_and_unique_affiliation_country_is_hong_space_kong() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Hong-Kong");
        uniqueAffiliation.setCountry("Hong Kong");

        boolean isCountryUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_country_is_peoples_republic_china_comma_and_unique_affiliation_country_is_china() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("Peoples republic China,");
        uniqueAffiliation.setCountry("China");

        boolean isCountryUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isCountryUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_city_is_different_from_unique_affiliation_city() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setSettlement("Pune");
        uniqueAffiliation.setSettlement("Puna");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_city_is_same_as_unique_affiliation_city() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setSettlement("Pune");
        uniqueAffiliation.setSettlement("Pune");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_does_not_have_city_but_unique_affiliation_have_city() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setSettlement("Pune");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_does_not_have_city_but_new_affiliation_have_city() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setSettlement("Pune");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_state_is_different_from_unique_affiliation_state() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setRegion("MH");
        uniqueAffiliation.setRegion("Maharashtra");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_state_is_same_as_unique_affiliation_state() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setRegion("Maharashtra");
        uniqueAffiliation.setRegion("Maharashtra");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_does_not_have_state_but_unique_affiliation_have_state() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setRegion("Maharashtra");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_does_not_have_state_but_new_affiliation_have_state() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setRegion("Maharashtra");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_post_code_is_different_from_unique_affiliation_post_code() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setPostCode("EC1A");
        uniqueAffiliation.setPostCode("EC4A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_post_code_is_same_as_unique_affiliation_post_code() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setPostCode("EC1A");
        uniqueAffiliation.setPostCode("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_does_not_have_post_code_but_unique_affiliation_have_post_code() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setPostCode("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_does_not_have_post_code_but_new_affiliation_have_post_code() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setPostCode("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_post_box_is_different_from_unique_affiliation_post_box() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC2A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_post_box_is_same_as_unique_affiliation_post_box() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_does_not_have_post_box_but_unique_affiliation_have_post_box() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_does_not_have_post_box_but_new_affiliation_have_post_box() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_city_post_code_post_box() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("Tempa");
        uniqueAffiliation.setSettlement("Tempa");

        newAffiliation.setPostBox("ABC");
        uniqueAffiliation.setPostBox("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_city_post_code_but_post_box_is_different() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("Tempa");
        uniqueAffiliation.setSettlement("Tempa");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC2A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_city_post_box_but_post_code_is_different() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("Tempa");
        uniqueAffiliation.setSettlement("Tempa");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABCD");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_post_box_post_code_but_city_is_different() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("Tempa");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_city_post_box_post_code_but_state_is_different() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Texas");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_state_city_post_box_post_code_but_country_is_different() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United Kingdom");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_city_post_box_but_post_code_is_missing_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_city_post_code_but_post_box_is_missing_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_state_post_box_post_code_but_city_is_missing_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_country_post_box_city_post_code_but_state_is_missing_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setCountry("USA");
        uniqueAffiliation.setCountry("United states");

        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_post_box_state_city_post_code_but_country_is_missing_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Florida");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertFalse(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_unique_affiliation_and_new_affiliation_have_same_post_box_city_post_code_but_state_is_differnet_and_country_is_missing_in_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        uniqueAffiliation.setCountry("United states");

        newAffiliation.setRegion("Florida");
        uniqueAffiliation.setRegion("Texas");

        newAffiliation.setSettlement("LakeLand");
        uniqueAffiliation.setSettlement("LakeLand");

        newAffiliation.setPostCode("ABC");
        uniqueAffiliation.setPostCode("ABC");

        newAffiliation.setPostBox("EC1A");
        uniqueAffiliation.setPostBox("EC1A");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAddressUnique_when_new_affiliation_state_is_m_h_and_unique_affiliation_state_is_m_dot_h_dot() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();

        newAffiliation.setRegion("MH");
        uniqueAffiliation.setRegion("M.H.");

        boolean isUnique = biblioItem.isAddressUnique(newAffiliation, uniqueAffiliation);
        assertTrue(isUnique);
    }

    @Test
    public void test_isAffiliationUnique_when_new_affiliation_has_addressline_city_country_and_unique_affiliation_has_only_country_same_as_new_affiliation() {
        Affiliation newAffiliation = new Affiliation();
        Affiliation uniqueAffiliation = new Affiliation();
        List<Affiliation> uniqueAffiliations = new ArrayList<>();

        newAffiliation.setAddrLine("123 street");
        newAffiliation.setSettlement("Pune");
        newAffiliation.setCountry("India");
        uniqueAffiliation.setCountry("India");
        uniqueAffiliations.add(uniqueAffiliation);

        boolean isUnique = biblioItem.isAffiliationUnique(newAffiliation, uniqueAffiliations);
        assertFalse(isUnique);

        assertEquals(uniqueAffiliations.get(0).getAddrLine(), newAffiliation.getAddrLine());
        assertEquals(uniqueAffiliations.get(0).getSettlement(), newAffiliation.getSettlement());
        assertEquals(uniqueAffiliations.get(0).getCountry(), newAffiliation.getCountry());
    }
}