package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;
import org.grobid.core.document.Document;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.AbstractEngineFactory;
import org.grobid.core.mock.MockContext;
import org.junit.*;
import java.io.*;

import static org.junit.Assert.assertTrue;

public class HeaderParserTest {

    private static HeaderParser headerParser;
    private static Segmentation target;


    @BeforeClass
    public static void setInitialContext() throws Exception {
        MockContext.setInitialContext();
        AbstractEngineFactory.init();
        headerParser =  new HeaderParser(new EngineParsers());
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
    public void Test_processing_should_have_articlenote() {

        // ...Arrange
        BiblioItem biblioItem = new BiblioItem();
        biblioItem.setArticleNote("article note");

        // ...Act
        Pair<String, Document> doc = headerParser.processing2(new File(
                        "src/test/resources/org/grobid/core/engines/patent/ReferenceExtractor/sample-1.pdf")
                        .getAbsolutePath(),
                false, biblioItem, GrobidAnalysisConfig.defaultInstance());


        // ...Assert
        assertTrue(doc.getLeft().contains("<articlenote>"));
    }

}
