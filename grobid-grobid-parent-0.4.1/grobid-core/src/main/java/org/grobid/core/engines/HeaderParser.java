package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.utilities.RegexPattern;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Date;
import org.grobid.core.data.util.ITag;
import org.grobid.core.data.util.SizedStack;
import org.grobid.core.data.util.Utility;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorHeader;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Patrice Lopez
 */
public class HeaderParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderParser.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();

    private Consolidation consolidator = null;

    private EngineParsers parsers;

    public HeaderParser(EngineParsers parsers) {
        super(GrobidModels.HEADER);
        this.parsers = parsers;
        GrobidProperties.getInstance();
    }

    /**
     * Processing with application of the segmentation model
     */
    public Pair<String, Document> processing(File input, BiblioItem resHeader, GrobidAnalysisConfig config) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(input, config.getStartPage(), config.getEndPage());
            Document doc = parsers.getSegmentationParser().processing(documentSource, config);

            String tei = processingHeaderSection(config.isConsolidateHeader(), doc, resHeader);
            return new ImmutablePair<String, Document>(tei, doc);
        } finally {
            if (documentSource != null) {
                documentSource.close(true);
            }
        }
    }

    /**
     * Processing without application of the segmentation model, regex are used to identify the header
     * zone.
     */
    public Pair<String, Document> processing2(String pdfInput, boolean consolidate,
                                              BiblioItem resHeader, GrobidAnalysisConfig config) {
        DocumentSource documentSource = null;
        try {
            System.out.println(config.getStartPage()+" -- " +config.getEndPage());
            documentSource = DocumentSource.fromPdf(new File(pdfInput), config.getStartPage(), config.getEndPage());
            Document doc = new Document(documentSource);
            doc.addTokenizedDocument(config);

            if (doc.getBlocks() == null) {
                throw new GrobidException("PDF parsing resulted in empty content");
            }

            String tei = processingHeaderBlockNew(consolidate, doc, resHeader);
            return Pair.of(tei, doc);
        } finally {
            if (documentSource != null) {
                documentSource.close(true);
            }
        }
    }

    /**
     * Header processing after identification of the header blocks with heuristics (old approach)
     */
    public String processingHeaderBlock(boolean consolidate, Document doc, BiblioItem resHeader) {
        String header;
        //if (doc.getBlockDocumentHeaders() == null) {
            header = doc.getHeaderFeatured(true, true);
        /*} else {
            header = doc.getHeaderFeatured(false, true);
        }*/
        List<LayoutToken> tokenizations = doc.getTokenizationsHeader();
//System.out.println(tokenizations.toString());

        if ((header != null) && (header.trim().length() > 0)) {
            String res = label(header);
            resHeader = resultExtraction(res, true, tokenizations, resHeader);

            /**
             * amn-mod
             */
            Boolean flagAdd = false;
            Boolean flagAff = false;
            StringBuilder sb = new StringBuilder();
            String emailClassifiedWrongly = "";
            String regex="(?:[a-z.0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
            Pattern pat= Pattern.compile(regex);
            ArrayList<ArrayList<Integer>> emailIdxAdd =new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> emailIdxAff =new ArrayList<ArrayList<Integer>>();
            //for emails classified as address
            String textAdd=resHeader.getAddress();
            if (textAdd != null) {
                Matcher mAdd = pat.matcher(textAdd.toLowerCase());
                while (mAdd.find()) {
                    flagAdd = true;
                    ArrayList<Integer> arList = new ArrayList<Integer>();
                    arList.add(mAdd.start());
                    arList.add(mAdd.end());
                    emailIdxAdd.add(arList);
                    textAdd = replaceStr(textAdd, emailIdxAdd);
                    resHeader.setAddress(textAdd);
                    emailClassifiedWrongly = mAdd.group();
                    resHeader.setEmail(resHeader.getEmail() + ";" + emailClassifiedWrongly);
                }
            }
            //for emails classified as affliations
            String textAff = resHeader.getAffiliation();
            if (textAff != null) {
                Matcher mAff = pat.matcher(textAff.toLowerCase());
                while (mAff.find()) {
                    flagAff = true;
                    ArrayList<Integer> arList = new ArrayList<Integer>();
                    arList.add(mAff.start());
                    arList.add(mAff.end());
                    emailIdxAff.add(arList);
                    textAff = replaceStr(textAff, emailIdxAff);
                    resHeader.setAffiliation(textAff);
                    emailClassifiedWrongly = mAff.group();
                    resHeader.setEmail(resHeader.getEmail() + ";" + emailClassifiedWrongly);
                }
            }

            for (ArrayList<Integer> idx : emailIdxAdd){
                emailIdxAff.add(idx);
            }



            /**
             * amn-mod
             */

            // language identification
            String contentSample = "";
            if (resHeader.getTitle() != null)
                contentSample += resHeader.getTitle();
            if (resHeader.getAbstract() != null)
                contentSample += "\n" + resHeader.getAbstract();
            if (resHeader.getKeywords() != null)
                contentSample += "\n" + resHeader.getKeywords();
            if (contentSample.length() < 200) {
                // we need more textual content to ensure that the language identification will be
                // correct
                contentSample += doc.getBody();
            }
            Language langu = languageUtilities.runLanguageId(contentSample);
            if (langu != null) {
                String lang = langu.getLangId();
                doc.setLanguage(lang);
                resHeader.setLanguage(lang);
            }

            if (resHeader != null) {
                if (resHeader.getAbstract() != null) {
                    resHeader.setAbstract(TextUtilities.dehyphenizeHard(resHeader.getAbstract()));
                    //resHeader.setAbstract(TextUtilities.dehyphenize(resHeader.getAbstract()));
                }
                BiblioItem.cleanTitles(resHeader);
                if (resHeader.getTitle() != null) {
                    // String temp =
                    // utilities.dehyphenizeHard(resHeader.getTitle());
                    String temp = TextUtilities.dehyphenize(resHeader.getTitle());
                    temp = temp.trim();
                    if (temp.length() > 1) {
                        if (temp.startsWith("1"))
                            temp = temp.substring(1, temp.length());
                        temp = temp.trim();
                    }
                    resHeader.setTitle(temp);
                }
                if (resHeader.getBookTitle() != null) {
                    resHeader.setBookTitle(TextUtilities.dehyphenize(resHeader.getBookTitle()));
                }

                resHeader.setOriginalAuthors(resHeader.getAuthors());
                List<Person> contextualPersonSequence = getContextualAuthorsSequence(resHeader.getCorresAuthors());
                boolean fragmentedAuthors = false;
                boolean hasMarker = false;
                List<Integer> authorsBlocks = new ArrayList<Integer>();
                String[] authorSegments;
                List<Boolean> corresFlags = null;
                if (resHeader.getAuthors() != null) {
                    ArrayList<String> auts;
                    authorSegments = resHeader.getAuthors().split("\n");
                    corresFlags = resHeader.getCorresFlagsList();
                    if (authorSegments.length > 1) {
                        fragmentedAuthors = true;
                    }
                    for (int k = 0; k < authorSegments.length; k++) {
                        auts = new ArrayList<String>();
                        boolean corresFlag = false;
                        auts.add(authorSegments[k]);
                        List<Person> localAuthors = parsers.getAuthorParser().processingHeader(auts);
                        corresFlag = corresFlags.get(k);
                        if (localAuthors != null) {
                            for (Person pers : localAuthors) {
                                if (corresFlag == true) {
                                    pers.setCorresp(true);
                                    pers.setContectualAuthorSeq(contextualPersonSequence);
                                }
                                resHeader.addFullAuthor(pers);
                                if (pers.getMarkers() != null) {
                                    hasMarker = true;
                                }
                                authorsBlocks.add(k);
                            }
                        }
                    }

                    resHeader.setFullAffiliations(
						parsers.getAffiliationAddressParser().processReflow(res, tokenizations));

//                    resHeader.setAffiliationForSequenceTag(resHeader.getFullAffiliations());

                    resHeader.attachEmails();
                    resHeader.attachPhones(res);
                    resHeader.attachUrls(res);
                    boolean attached = false;
                    if (fragmentedAuthors && !hasMarker) {
                        if (resHeader.getFullAffiliations() != null) {
                            if (authorSegments != null) {
                                if (resHeader.getFullAffiliations().size() == authorSegments.length) {
                                    int k = 0;
                                    for (Person pers : resHeader.getFullAuthors()) {
                                        if (k < authorsBlocks.size()) {
                                            int indd = authorsBlocks.get(k);
                                            if (indd < resHeader.getFullAffiliations().size()) {
                                                pers.addAffiliation(resHeader.getFullAffiliations().get(indd));
                                            }
                                        }
                                        k++;
                                    }
                                    attached = true;
                                    resHeader.setFullAffiliations(null);
                                    resHeader.setAffiliation(null);
                                }
                            }
                        }
                    }
                    if (!attached) {
                        resHeader.attachAffiliations();
                    }

                    if (resHeader.getEditors() != null) {
                        ArrayList<String> edits = new ArrayList<String>();
                        edits.add(resHeader.getEditors());

                        resHeader.setFullEditors(parsers.getAuthorParser().processingHeader(edits));
                        // resHeader.setFullEditors(authorParser.processingCitation(edits));
                    }

                    if (resHeader.getReference() != null) {
                        BiblioItem refer = parsers.getCitationParser().processing(resHeader.getReference(), false);
                        if (refer != null)
                            BiblioItem.correct(resHeader, refer);
                    }
                }

				// keyword post-processing
				if (resHeader.getKeyword() != null) {
					String keywords = TextUtilities.dehyphenize(resHeader.getKeyword());
					keywords = BiblioItem.cleanKeywords(keywords);
					resHeader.setKeyword(keywords.replace("\n", " ").replace("  ", " "));
					List<Keyword> keywordsSegmented = BiblioItem.segmentKeywords(keywords);
					if ( (keywordsSegmented != null) && (keywordsSegmented.size() > 0) )
						resHeader.setKeywords(keywordsSegmented);
				}

                // DOI pass
                List<String> dois = doc.getDOIMatches();
                if (dois != null) {
                    if ((dois.size() == 1) && (resHeader != null)) {
                        resHeader.setDOI(dois.get(0));
                    }
                }

                if (consolidate) {
                    resHeader = consolidateHeader(resHeader);
                }

                // normalization of dates
                if (resHeader != null) {
                    if (resHeader.getPublicationDate() != null) {
                        List<Date> dates = parsers.getDateParser().processing(resHeader.getPublicationDate());
                        // most basic heuristic, we take the first date - to be
                        // revised...
                        if (dates != null) {
                            if (dates.size() > 0) {
                                resHeader.setNormalizedPublicationDate(dates.get(0));
                            }
                        }
                    }

                    if (resHeader.getSubmissionDate() != null) {
                        List<Date> dates = parsers.getDateParser().processing(resHeader.getSubmissionDate());
                        if (dates != null) {
                            if (dates.size() > 0) {
                                resHeader.setNormalizedSubmissionDate(dates.get(0));
                            }
                        }
                    }
                }
            }
        } else {
            LOGGER.debug("WARNING: header is empty.");
        }

        TEIFormatter teiFormatter = new TEIFormatter(doc);
        StringBuilder tei = teiFormatter.toTEIHeader(resHeader, null, GrobidAnalysisConfig.builder().consolidateHeader(consolidate).build());
        tei.append("\t</text>\n");
        tei.append("</TEI>\n");
        //LOGGER.debug(tei.toString());
        return tei.toString();
    }

    private void addMissingAffiliation(Person per, Person seqPer) {
        if(seqPer.getAffiliations() != null && per.getAffiliations() != null) {
            for (int i = 0; i < seqPer.getAffiliations().size(); i++) {
                boolean doesAffExist = false;
                for (int j = 0; j < per.getAffiliations().size(); j++) {
                    if (doesAffiliationSame(seqPer.getAffiliations().get(i), per.getAffiliations().get(j))) {
                        doesAffExist = true;
                        break;
                    }
                }

                if (!doesAffExist) {
                    per.getAffiliations().add(seqPer.getAffiliations().get(i));
                }

            }
        }
    }

    private boolean doesAffiliationSame(Affiliation a, Affiliation aff) {

        if (doesArrayStringMatch(a.getInstitutions(), aff.getInstitutions()) &&
                doesArrayStringMatch(a.getDepartments(), aff.getDepartments()) &&
                doesArrayStringMatch(a.getLaboratories(), aff.getLaboratories()) &&
                doesStringMatch(a.getCountry(), aff.getCountry()) &&
                doesStringMatch(a.getPostCode(), aff.getPostCode()) &&
                doesStringMatch(a.getPostBox(), aff.getPostBox()) &&
                doesStringMatch(a.getRegion(), aff.getRegion()) &&
                doesStringMatch(a.getSettlement(), aff.getSettlement()) &&
                doesStringMatch(a.getAddrLine(), aff.getAddrLine())
                ) return true;


            return false;
    }

    private boolean doesStringMatch(String a, String aff) {
        if (a == null && aff == null) {
            return true;
        } else if (a != null && aff == null) {
            return false;
        } else if (a == null && aff != null) {
            return false;
        } else if(a.equals(aff)) {
            return true;
        }
        return false;
    }

    private boolean doesArrayStringMatch(List<String > a, List<String > aff) {
        if (a == null && aff == null) {
            return true;
        } else if (a != null && aff == null) {
            return false;
        } else if (a == null && aff != null) {
            return false;
        } else {
            for (String as : a) {
                boolean doesPropSame = false;
                for (String affs : aff) {
                    if (as.equals(affs)) {
                        doesPropSame = true;
                        break;
                    }
                }
                if (!doesPropSame) {
                    return false;
                }
            }
            return true;
        }
    }

    private void processingAuthorsITag(List<ITag> sequenceTags) {
        try {
            List<Person> listAuthors = null;
            for (ITag tag : sequenceTags) {
                if (tag.getITagName().equals("I-<author>")) {
                    List<String> list = new ArrayList<String>();
                    list.add(tag.getITagText());
                    listAuthors = parsers.getAuthorParser().processingHeader(list);
                    tag.setPersons(listAuthors);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Header processing after application of the segmentation model (new approach)
     */
    public String processingHeaderSection(boolean consolidate, Document doc, BiblioItem resHeader) {
        try {
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(SegmentationLabel.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            if (documentHeaderParts != null) {
                List<LayoutToken> tokenizationsHeader = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.a;
                    DocumentPointer dp2 = docPiece.b;

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsHeader.add(tokenizations.get(i));
                    }
                }

                String header = getSectionHeaderFeatured(doc, documentHeaderParts, true);
                String res = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    res = label(header);
                    resHeader = resultExtraction(res, true, tokenizations, resHeader);
                }

                // language identification
                String contentSample = "";
                if (resHeader.getTitle() != null)
                    contentSample += resHeader.getTitle();
                if (resHeader.getAbstract() != null)
                    contentSample += "\n" + resHeader.getAbstract();
                if (contentSample.length() < 200) {
                    // we need more textual content to ensure that the language identification will be
                    // correct
                    SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(SegmentationLabel.BODY);
                    StringBuilder contentBuffer = new StringBuilder();
                    for (DocumentPiece docPiece : documentBodyParts) {
                        DocumentPointer dp1 = docPiece.a;
                        DocumentPointer dp2 = docPiece.b;

                        int tokens = dp1.getTokenDocPos();
                        int tokene = dp2.getTokenDocPos();
                        for (int i = tokens; i < tokene; i++) {
                            contentBuffer.append(tokenizations.get(i));
                            contentBuffer.append(" ");
                        }
                    }
                    contentSample += " " + contentBuffer.toString();
                }
                Language langu = languageUtilities.runLanguageId(contentSample);
                if (langu != null) {
                    String lang = langu.getLangId();
                    doc.setLanguage(lang);
                    resHeader.setLanguage(lang);
                }

                if (resHeader != null) {
                    if (resHeader.getAbstract() != null) {
                        resHeader.setAbstract(TextUtilities.dehyphenizeHard(resHeader.getAbstract()));
                        //resHeader.setAbstract(TextUtilities.dehyphenize(resHeader.getAbstract()));
                    }
                    BiblioItem.cleanTitles(resHeader);
                    if (resHeader.getTitle() != null) {
                        // String temp =
                        // utilities.dehyphenizeHard(resHeader.getTitle());
                        String temp = TextUtilities.dehyphenize(resHeader.getTitle());
                        temp = temp.trim();
                        if (temp.length() > 1) {
                            if (temp.startsWith("1"))
                                temp = temp.substring(1, temp.length());
                            temp = temp.trim();
                        }
                        resHeader.setTitle(temp);
                    }
                    if (resHeader.getBookTitle() != null) {
                        resHeader.setBookTitle(TextUtilities.dehyphenize(resHeader.getBookTitle()));
                    }

                    resHeader.setOriginalAuthors(resHeader.getAuthors());
                    boolean fragmentedAuthors = false;
                    boolean hasMarker = false;
                    List<Integer> authorsBlocks = new ArrayList<Integer>();
                    String[] authorSegments = null;
                    if (resHeader.getAuthors() != null) {
                        List<String> auts;
                        authorSegments = resHeader.getAuthors().split("\n");
                        if (authorSegments.length > 1) {
                            fragmentedAuthors = true;
                        }
                        for (int k = 0; k < authorSegments.length; k++) {
                            auts = new ArrayList<String>();
                            auts.add(authorSegments[k]);
                            List<Person> localAuthors = parsers.getAuthorParser().processingHeader(auts);
                            if (localAuthors != null) {
                                for (Person pers : localAuthors) {
                                    resHeader.addFullAuthor(pers);
                                    if (pers.getMarkers() != null) {
                                        hasMarker = true;
                                    }
                                    authorsBlocks.add(k);
                                }
                            }
                        }
                    }

                    resHeader.setFullAffiliations(
						parsers.getAffiliationAddressParser().processReflow(res, tokenizations));
                    resHeader.attachEmails();
                    boolean attached = false;
                    if (fragmentedAuthors && !hasMarker) {
                        if (resHeader.getFullAffiliations() != null) {
                            if (authorSegments != null) {
                                if (resHeader.getFullAffiliations().size() == authorSegments.length) {
                                    int k = 0;
                                    for (Person pers : resHeader.getFullAuthors()) {
                                        if (k < authorsBlocks.size()) {
                                            int indd = authorsBlocks.get(k);
                                            if (indd < resHeader.getFullAffiliations().size()) {
                                                pers.addAffiliation(resHeader.getFullAffiliations().get(indd));
                                            }
                                        }
                                        k++;
                                    }
                                    attached = true;
                                    resHeader.setFullAffiliations(null);
                                    resHeader.setAffiliation(null);
                                }
                            }
                        }
                    }
                    if (!attached) {
                        resHeader.attachAffiliations();
                    }

                    if (resHeader.getEditors() != null) {
                        List<String> edits = new ArrayList<String>();
                        edits.add(resHeader.getEditors());

                        resHeader.setFullEditors(parsers.getAuthorParser().processingHeader(edits));
                        // resHeader.setFullEditors(authorParser.processingCitation(edits));
                    }

                    if (resHeader.getReference() != null) {
                        BiblioItem refer = parsers.getCitationParser().processing(resHeader.getReference(), false);
                        BiblioItem.correct(resHeader, refer);
                    }
                }

				// keyword post-processing
				if (resHeader.getKeyword() != null) {
					String keywords = TextUtilities.dehyphenize(resHeader.getKeyword());
					keywords = BiblioItem.cleanKeywords(keywords);
					resHeader.setKeyword(keywords.replace("\n", " ").replace("  ", " "));
					List<Keyword> keywordsSegmented = BiblioItem.segmentKeywords(keywords);
					if ( (keywordsSegmented != null) && (keywordsSegmented.size() > 0) )
						resHeader.setKeywords(keywordsSegmented);
				}

                // DOI pass
                List<String> dois = doc.getDOIMatches();
                if (dois != null) {
                    if ((dois.size() == 1) && (resHeader != null)) {
                        resHeader.setDOI(dois.get(0));
                    }
                }

                if (consolidate) {
                    resHeader = consolidateHeader(resHeader);
                }

                // normalization of dates
                if (resHeader != null) {
                    if (resHeader.getPublicationDate() != null) {
                        List<Date> dates = parsers.getDateParser().processing(resHeader.getPublicationDate());
                        // most basic heuristic, we take the first date - to be
                        // revised...
                        if (dates != null) {
                            if (dates.size() > 0) {
                                resHeader.setNormalizedPublicationDate(dates.get(0));
                            }
                        }
                    }

                    if (resHeader.getSubmissionDate() != null) {
                        List<Date> dates = parsers.getDateParser().processing(resHeader.getSubmissionDate());
                        if (dates != null) {
                            if (dates.size() > 0) {
                                resHeader.setNormalizedSubmissionDate(dates.get(0));
                            }
                        }
                    }
                }

                TEIFormatter teiFormatter = new TEIFormatter(doc);
                StringBuilder tei = teiFormatter.toTEIHeader(resHeader, null, GrobidAnalysisConfig.defaultInstance());
                tei.append("\t</text>\n");
                tei.append("</TEI>\n");
                //LOGGER.debug(tei);
                return tei.toString();
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return null;
    }


    /**
     * Return the header section with features to be processed by the CRF model
     */
    private String getSectionHeaderFeatured(Document doc,
                                            SortedSet<DocumentPiece> documentHeaderParts,
                                            boolean withRotation) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        StringBuilder header = new StringBuilder();
        String currentFont = null;
        int currentFontSize = -1;

        // vector for features
        FeaturesVectorHeader features;
        boolean endblock;
        //for (Integer blocknum : blockDocumentHeaders) {
        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        for (DocumentPiece docPiece : documentHeaderParts) {
            DocumentPointer dp1 = docPiece.a;
            DocumentPointer dp2 = docPiece.b;

            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
                Block block = blocks.get(blockIndex);
                boolean newline;
                boolean previousNewline = false;
                endblock = false;
                List<LayoutToken> tokens = block.getTokens();
                if (tokens == null)
                    continue;
                int n = 0;
                if (blockIndex == dp1.getBlockPtr()) {
                    //n = block.getStartToken();
                    n = dp1.getTokenDocPos() - block.getStartToken();
                }
                while (n < tokens.size()) {
                    if (blockIndex == dp2.getBlockPtr()) {
                        if (n > dp2.getTokenDocPos() - block.getStartToken()) {
                            break;
                        }
                    }

                    LayoutToken token = tokens.get(n);
                    features = new FeaturesVectorHeader();
                    features.token = token;
                    String text = token.getText();
                    if (text == null) {
                        n++;
                        continue;
                    }
                    //text = text.trim();
					text = text.replace(" ", "").replace("\t", "").replace("\u00A0","");
                    if (text.length() == 0) {
                        n++;
                        continue;
                    }

                    if (text.equals("\n") || text.equals("\r")) {
                        newline = true;
                        previousNewline = true;
                        n++;
                        continue;
                    } else
                        newline = false;

                    if (previousNewline) {
                        newline = true;
                        previousNewline = false;
                    }

					if (TextUtilities.filterLine(text)) {
	                    n++;
	                    continue;
	                }

                    features.string = text;

                    if (newline)
                        features.lineStatus = "LINESTART";
                    Matcher m0 = featureFactory.isPunct.matcher(text);
                    if (m0.find()) {
                        features.punctType = "PUNCT";
                    }
                    if (text.equals("(") || text.equals("[")) {
                        features.punctType = "OPENBRACKET";

                    } else if (text.equals(")") || text.equals("]")) {
                        features.punctType = "ENDBRACKET";

                    } else if (text.equals(".")) {
                        features.punctType = "DOT";

                    } else if (text.equals(",")) {
                        features.punctType = "COMMA";

                    } else if (text.equals("-")) {
                        features.punctType = "HYPHEN";

                    } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
                        features.punctType = "QUOTE";

                    }

                    if (n == 0) {
						// beginning of block
                        features.lineStatus = "LINESTART";
                        features.blockStatus = "BLOCKSTART";
                    } else if (n == tokens.size() - 1) {
						// end of block
                        features.lineStatus = "LINEEND";
                        previousNewline = true;
                        features.blockStatus = "BLOCKEND";
                        endblock = true;
                    } else {
                        // look ahead to see if we are at the end of a line within the block
                        boolean endline = false;

                        int ii = 1;
                        boolean endloop = false;
                        while ((n + ii < tokens.size()) && (!endloop)) {
                            LayoutToken tok = tokens.get(n + ii);
                            if (tok != null) {
                                String toto = tok.getText();
                                if (toto != null) {
                                    if (toto.equals("\n") || text.equals("\r")) {
                                        endline = true;
                                        endloop = true;
                                    } else {
                                        if ((toto.trim().length() != 0)
												&& (!text.equals("\u00A0"))
                                                && (!(toto.contains("@IMAGE")))
												&& (!(toto.contains("@PAGE")))
                                                && (!text.contains(".pbm"))
                                                && (!text.contains(".ppm"))
												&& (!text.contains(".png"))
                                                && (!text.contains(".vec"))
                                                && (!text.contains(".jpg"))) {
                                            endloop = true;
                                        }
                                    }
                                }
                            }

                            if (n + ii == tokens.size() - 1) {
                                endblock = true;
                                endline = true;
                            }

                            ii++;
                        }

                        if ((!endline) && !(newline)) {
                            features.lineStatus = "LINEIN";
                        } else if (!newline) {
                            features.lineStatus = "LINEEND";
                            previousNewline = true;
                        }

                        if ((!endblock) && (features.blockStatus == null))
                            features.blockStatus = "BLOCKIN";
                        else if (features.blockStatus == null)
                            features.blockStatus = "BLOCKEND";

                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    if (text.contains("-")) {
                        features.containDash = true;
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    Matcher m2 = featureFactory.YEAR.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    Matcher m3 = featureFactory.EMAIL.matcher(text);
                    if (m3.find()) {
                        features.email = true;
                    }

                    Matcher m4 = featureFactory.HTTP.matcher(text);
                    if (m4.find()) {
                        features.http = true;
                    }

                    if (currentFont == null) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else if (!currentFont.equals(token.getFont())) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else
                        features.fontStatus = "SAMEFONT";

                    int newFontSize = (int) token.getFontSize();
                    if (currentFontSize == -1) {
                        currentFontSize = newFontSize;
                        features.fontSize = "HIGHERFONT";
                    } else if (currentFontSize == newFontSize) {
                        features.fontSize = "SAMEFONTSIZE";
                    } else if (currentFontSize < newFontSize) {
                        features.fontSize = "HIGHERFONT";
                        currentFontSize = newFontSize;
                    } else if (currentFontSize > newFontSize) {
                        features.fontSize = "LOWERFONT";
                        currentFontSize = newFontSize;
                    }

                    if (token.getBold())
                        features.bold = true;

                    if (token.getItalic())
                        features.italic = true;

                    if (token.getRotation())
                        features.rotation = true;

                    // CENTERED
                    // LEFTAJUSTED

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    if (features.punctType == null)
                        features.punctType = "NOPUNCT";

                    header.append(features.printVector(withRotation));

                    n++;
                }
            }
        }

        return header.toString();
    }


    /**
     * Process the header of the specified pdf and format the result as training
     * data.
     *
     * @param inputFile  path to input file
     * @param pathHeader path to header
     * @param pathTEI    path to TEI
     */
    public Document createTrainingHeader(String inputFile, String pathHeader, String pathTEI) {
        DocumentSource documentSource = null;
        try {
            File file = new File(inputFile);
            String pdfFileName = file.getName();

            //Document doc = parsers.getSegmentationParser().processing(file, GrobidAnalysisConfig.defaultInstance());
            documentSource = DocumentSource.fromPdf(file);
            Document doc = parsers.getSegmentationParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            //documentSource = DocumentSource.fromPdf(file);
            //Document doc = new Document(documentSource);

            //doc.addTokenizedDocument();
            /*if (doc.getBlocks() == null) {
                throw new GrobidException("PDF parsing resulted in empty content");
            }*/

            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(SegmentationLabel.HEADER);
            List<LayoutToken> tokenizationsFull = doc.getTokenizations();

            if (documentHeaderParts != null) {
                List<LayoutToken> tokenizations = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.a;
                    DocumentPointer dp2 = docPiece.b;

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizations.add(tokenizationsFull.get(i));
                    }
                }
                String header = getSectionHeaderFeatured(doc, documentHeaderParts, true);
                String rese = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    rese = label(header);
                    //String header = doc.getHeaderFeatured(true, true);
                    //List<LayoutToken> tokenizations = doc.getTokenizationsHeader();

                    // we write the header untagged
                    String outPathHeader = pathHeader + File.separator + pdfFileName.replace(".pdf", ".header");
                    Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathHeader), false), "UTF-8");
                    writer.write(header + "\n");
                    writer.close();

                    // buffer for the header block
                    StringBuilder bufferHeader = trainingExtraction(rese, true, tokenizations);
                    Language lang = languageUtilities.runLanguageId(bufferHeader.toString());
                    if (lang != null) {
                        doc.setLanguage(lang.getLangId());
                    }

                    // buffer for the affiliation+address block
                    StringBuilder bufferAffiliation =
        				parsers.getAffiliationAddressParser().trainingExtraction(rese, tokenizations);
                    // buffer for the date block
                    StringBuilder bufferDate = null;
                    // we need to rebuild the found date string as it appears
                    String input = "";
                    int q = 0;
                    StringTokenizer st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<date>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.trim().length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferDate = parsers.getDateParser().trainingExtraction(inputs);
                    }

                    // buffer for the name block
                    StringBuilder bufferName = null;
                    // we need to rebuild the found author string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<author>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferName = parsers.getAuthorParser().trainingExtraction(inputs, true);
                    }

                    // buffer for the reference block
                    StringBuilder bufferReference = null;
                    // we need to rebuild the found citation string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<reference>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferReference = parsers.getCitationParser().trainingExtraction(inputs);
                    }

                    // write the TEI file to reflect the extract layout of the text as
                    // extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(new File(pathTEI + File.separator
                            + pdfFileName.replace(".pdf", GrobidProperties.FILE_ENDING_TEI_HEADER)), false), "UTF-8");
                    writer.write("<?xml version=\"1.0\" ?>\n<tei>\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
        					+ pdfFileName.replace(".pdf", "")
                            + "\"/>\n\t</teiHeader>\n\t<text");

                    if (lang != null) {
                        // TODO: why English (Slava)
                        writer.write(" xml:lang=\"en\"");
                    }
                    writer.write(">\n\t\t<front>\n");

                    writer.write(bufferHeader.toString());
                    writer.write("\n\t\t</front>\n\t</text>\n</tei>\n");
                    writer.close();

                    if (bufferAffiliation != null) {
                        if (bufferAffiliation.length() > 0) {
                            Writer writerAffiliation = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
        						File.separator
                                    + pdfFileName.replace(".pdf", ".affiliation.tei.xml")), false), "UTF-8");
                            writerAffiliation.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                            writerAffiliation.write("\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\""
                                    + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">");
                            writerAffiliation.write("\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>");
                            writerAffiliation.write("\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\t\t\t\t\t\t<author>\n\n");

                            writerAffiliation.write(bufferAffiliation.toString());

                            writerAffiliation.write("\n\t\t\t\t\t\t</author>\n\t\t\t\t\t</analytic>");
                            writerAffiliation.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                            writerAffiliation.write("\n\t</teiHeader>\n</tei>\n");
                            writerAffiliation.close();
                        }
                    }

                    if (bufferDate != null) {
                        if (bufferDate.length() > 0) {
                            Writer writerDate = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
        						File.separator
                                    + pdfFileName.replace(".pdf", ".date.xml")), false), "UTF-8");
                            writerDate.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                            writerDate.write("<dates>\n");

                            writerDate.write(bufferDate.toString());

                            writerDate.write("</dates>\n");
                            writerDate.close();
                        }
                    }

                    if (bufferName != null) {
                        if (bufferName.length() > 0) {
                            Writer writerName = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
        						File.separator
                                    + pdfFileName.replace(".pdf", ".authors.tei.xml")), false), "UTF-8");
                            writerName.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                            writerName.write("\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\"" + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                                    + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">");
                            writerName.write("\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>");
                            writerName.write("\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\n\t\t\t\t\t\t<author>");
                            writerName.write("\n\t\t\t\t\t\t\t<persName>\n");

                            writerName.write(bufferName.toString());

                            writerName.write("\t\t\t\t\t\t\t</persName>\n");
                            writerName.write("\t\t\t\t\t\t</author>\n\n\t\t\t\t\t</analytic>");
                            writerName.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                            writerName.write("\n\t</teiHeader>\n</tei>\n");
                            writerName.close();
                        }
                    }

                    if (bufferReference != null) {
                        if (bufferReference.length() > 0) {
                            Writer writerReference = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
        						File.separator
                                    + pdfFileName.replace(".pdf", ".header-reference.xml")), false), "UTF-8");
                            writerReference.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                            writerReference.write("<citations>\n");

                            writerReference.write(bufferReference.toString());

                            writerReference.write("</citations>\n");
                            writerReference.close();
                        }
                    }
                }
            }
            else {
                System.out.println("no header found");
            }
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            DocumentSource.close(documentSource, true);
        }
    }

    /**
     * Process the header (without application of the segmentation model) of the specified pdf and format the result as training
     * data.
     *
     * @param inputFile  path to input file
     * @param pathHeader path to header
     * @param pathTEI    path to TEI
     */
    public Document createTrainingHeader2(String inputFile, String pathHeader, String pathTEI) {
        DocumentSource documentSource = null;
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().startPage(0).endPage(2).build();
        try {
            File file = new File(inputFile);
            String pdfFileName = file.getName();
            documentSource = DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            Document doc = new Document(documentSource);
            doc.addTokenizedDocument(config);

            String header;
            header = doc.getHeaderFeatured(true, true);
            List<LayoutToken> tokenizations = doc.getTokenizationsHeader();

            String rese = null;
            if ((header != null) && (header.trim().length() > 0)) {
                rese = label(header);

                // we write the header untagged
                String outPathHeader = pathHeader + File.separator + pdfFileName.replace(".pdf", ".header");
                Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathHeader), false), "UTF-8");
                writer.write(header + "\n");
                writer.close();

                // buffer for the header block
                StringBuilder bufferHeader = trainingExtraction(rese, true, tokenizations);
                Language lang = languageUtilities.runLanguageId(bufferHeader.toString());
                if (lang != null) {
                    doc.setLanguage(lang.getLangId());
                }

                // buffer for the affiliation+address block
                StringBuilder bufferAffiliation =
                        parsers.getAffiliationAddressParser().trainingExtraction(rese, tokenizations);
                // buffer for the date block
                StringBuilder bufferDate = null;
                // we need to rebuild the found date string as it appears
                String input = "";
                int q = 0;
                StringTokenizer st = new StringTokenizer(rese, "\n");
                while (st.hasMoreTokens() && (q < tokenizations.size())) {
                    String line = st.nextToken();
                    String theTotalTok = tokenizations.get(q).getText();
                    String theTok = tokenizations.get(q).getText();
                    while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                        q++;
                        if ((q>0) && (q < tokenizations.size())) {
                            theTok = tokenizations.get(q).getText();
                            theTotalTok += theTok;
                        }
                    }
                    if (line.endsWith("<date>")) {
                        input += theTotalTok;
                    }
                    q++;
                }
                if (input.trim().length() > 1) {
                    List<String> inputs = new ArrayList<String>();
                    inputs.add(input.trim());
                    bufferDate = parsers.getDateParser().trainingExtraction(inputs);
                }

                // buffer for the name block
                StringBuilder bufferName = null;
                // we need to rebuild the found author string as it appears
                input = "";
                q = 0;
                st = new StringTokenizer(rese, "\n");
                while (st.hasMoreTokens() && (q < tokenizations.size())) {
                    String line = st.nextToken();
                    String theTotalTok = tokenizations.get(q).getText();
                    String theTok = tokenizations.get(q).getText();
                    while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                        q++;
                        if ((q>0) && (q < tokenizations.size())) {
                            theTok = tokenizations.get(q).getText();
                            theTotalTok += theTok;
                        }
                    }
                    if (line.endsWith("<author>")) {
                        input += theTotalTok;
                    }
                    q++;
                }
                if (input.length() > 1) {
                    List<String> inputs = new ArrayList<String>();
                    inputs.add(input.trim());
                    bufferName = parsers.getAuthorParser().trainingExtraction(inputs, true);
                }

                // buffer for the reference block
                StringBuilder bufferReference = null;
                // we need to rebuild the found citation string as it appears
                input = "";
                q = 0;
                st = new StringTokenizer(rese, "\n");
                while (st.hasMoreTokens() && (q < tokenizations.size())) {
                    String line = st.nextToken();
                    String theTotalTok = tokenizations.get(q).getText();
                    String theTok = tokenizations.get(q).getText();
                    while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                        q++;
                        if ((q>0) && (q < tokenizations.size())) {
                            theTok = tokenizations.get(q).getText();
                            theTotalTok += theTok;
                        }
                    }
                    if (line.endsWith("<reference>")) {
                        input += theTotalTok;
                    }
                    q++;
                }
                if (input.length() > 1) {
                    List<String> inputs = new ArrayList<String>();
                    inputs.add(input.trim());
                    bufferReference = parsers.getCitationParser().trainingExtraction(inputs);
                }

                // write the TEI file to reflect the extract layout of the text as
                // extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(new File(pathTEI + File.separator
                        + pdfFileName.replace(".pdf", GrobidProperties.FILE_ENDING_TEI_HEADER)), false), "UTF-8");
                writer.write("<?xml version=\"1.0\" ?>\n<tei>\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                        + pdfFileName.replace(".pdf", "")
                        + "\"/>\n\t</teiHeader>\n\t<text");

                if (lang != null) {
                    // TODO: why English (Slava)
                    writer.write(" xml:lang=\"en\"");
                }
                writer.write(">\n\t\t<front>\n");

                writer.write(bufferHeader.toString());
                writer.write("\n\t\t</front>\n\t</text>\n</tei>\n");
                writer.close();

                if (bufferAffiliation != null) {
                    if (bufferAffiliation.length() > 0) {
                        Writer writerAffiliation = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replace(".pdf", ".affiliation.tei.xml")), false), "UTF-8");
                        writerAffiliation.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        writerAffiliation.write("\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\""
                                + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">");
                        writerAffiliation.write("\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>");
                        writerAffiliation.write("\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\t\t\t\t\t\t<author>\n\n");

                        writerAffiliation.write(bufferAffiliation.toString());

                        writerAffiliation.write("\n\t\t\t\t\t\t</author>\n\t\t\t\t\t</analytic>");
                        writerAffiliation.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                        writerAffiliation.write("\n\t</teiHeader>\n</tei>\n");
                        writerAffiliation.close();
                    }
                }

                if (bufferDate != null) {
                    if (bufferDate.length() > 0) {
                        Writer writerDate = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replace(".pdf", ".date.xml")), false), "UTF-8");
                        writerDate.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                        writerDate.write("<dates>\n");

                        writerDate.write(bufferDate.toString());

                        writerDate.write("</dates>\n");
                        writerDate.close();
                    }
                }

                if (bufferName != null) {
                    if (bufferName.length() > 0) {
                        Writer writerName = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replace(".pdf", ".authors.tei.xml")), false), "UTF-8");
                        writerName.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        writerName.write("\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\"" + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                                + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">");
                        writerName.write("\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>");
                        writerName.write("\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\n\t\t\t\t\t\t<author>");
                        writerName.write("\n\t\t\t\t\t\t\t<persName>\n");

                        writerName.write(bufferName.toString());

                        writerName.write("\t\t\t\t\t\t\t</persName>\n");
                        writerName.write("\t\t\t\t\t\t</author>\n\n\t\t\t\t\t</analytic>");
                        writerName.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                        writerName.write("\n\t</teiHeader>\n</tei>\n");
                        writerName.close();
                    }
                }

                if (bufferReference != null) {
                    if (bufferReference.length() > 0) {
                        Writer writerReference = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replace(".pdf", ".header-reference.xml")), false), "UTF-8");
                        writerReference.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                        writerReference.write("<citations>\n");

                        writerReference.write(bufferReference.toString());

                        writerReference.write("</citations>\n");
                        writerReference.close();
                    }
                }
            }
            else {
                System.out.println("no header found");
            }
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            DocumentSource.close(documentSource, true);
        }
    }

    /**
     * OverLoad createTrainingHeader for restService
     **/
    public CreateTrainingHeader createTrainingHeader2(String inputFile) {
        DocumentSource documentSource = null;
        String headerText = null;
        String affiliationText =  null;
        String authorText = null;
        List<String> res = new ArrayList<String>();
        CreateTrainingHeader obj = new CreateTrainingHeader();
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().startPage(0).endPage(2).build();

        try {
            File file = new File(inputFile);
            String pdfFileName = file.getName();
            documentSource = DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            Document doc = new Document(documentSource);
            doc.addTokenizedDocument(config);

            String header;
            header = doc.getHeaderFeatured(true, true);
            List<LayoutToken> tokenizations = doc.getTokenizationsHeader();

                String rese = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    rese = label(header);

                    // buffer for the header block
                    StringBuilder bufferHeader = trainingExtraction(rese, true, tokenizations);
                    Language lang = languageUtilities.runLanguageId(bufferHeader.toString());
                    if (lang != null) {
                        doc.setLanguage(lang.getLangId());
                    }

                    // buffer for the affiliation+address block
                    StringBuilder bufferAffiliation =
                            parsers.getAffiliationAddressParser().trainingExtraction(rese, tokenizations);
                    // buffer for the date block
                    StringBuilder bufferDate = null;
                    // we need to rebuild the found date string as it appears
                    String input = "";
                    int q = 0;
                    StringTokenizer st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<date>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.trim().length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferDate = parsers.getDateParser().trainingExtraction(inputs);
                    }

                    // buffer for the name block
                    StringBuilder bufferName = null;
                    // we need to rebuild the found author string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<author>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferName = parsers.getAuthorParser().trainingExtraction(inputs, true);
                    }

                    // buffer for the reference block
                    StringBuilder bufferReference = null;
                    // we need to rebuild the found citation string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<reference>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferReference = parsers.getCitationParser().trainingExtraction(inputs);
                    }

                    // write the TEI file to reflect the extract layout of the text as
                    // extracted from the pdf

                    headerText = "<?xml version=\"1.0\" ?>\n<tei>\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                            + pdfFileName.replace(".pdf", "")
                            + "\"/>\n\t</teiHeader>\n\t<text";

                    if (lang != null) {
                        // TODO: why English (Slava)
                        headerText += " xml:lang=\"en\"";
                    }
                    headerText += ">\n\t\t<front>\n";

                    headerText += bufferHeader.toString();
                    headerText += "\n\t\t</front>\n\t</text>\n</tei>\n";
                    obj.setHeader(headerText);

                    if (bufferAffiliation != null) {
                        if (bufferAffiliation.length() > 0) {
                            affiliationText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                            affiliationText += "\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\""
                                    + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">";
                            affiliationText += "\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>";
                            affiliationText += "\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\t\t\t\t\t\t<author>\n\n";
                            affiliationText += bufferAffiliation.toString();
                            affiliationText += "\n\t\t\t\t\t\t</author>\n\t\t\t\t\t</analytic>";
                            affiliationText += "\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>";
                            affiliationText += "\n\t</teiHeader>\n</tei>\n";
                            obj.setAffiliation(affiliationText);
                        }
                    }


                    if (bufferName != null) {
                        if (bufferName.length() > 0) {
                            authorText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                            authorText += "\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\"" + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                                    + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">";
                            authorText += "\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>";
                            authorText += "\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\n\t\t\t\t\t\t<author>";
                            authorText += "\n\t\t\t\t\t\t\t<persName>\n";
                            authorText += bufferName.toString();
                            authorText += "\t\t\t\t\t\t\t</persName>\n";
                            authorText += "\t\t\t\t\t\t</author>\n\n\t\t\t\t\t</analytic>";
                            authorText += "\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>";
                            authorText += "\n\t</teiHeader>\n</tei>\n";
                            obj.setAuthor(authorText);
                        }
                    }
                }
            else {
                System.out.println("no header found");
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            DocumentSource.close(documentSource, true);
        }
    }

    /**
     * OverLoad createTrainingHeader for restService
     **/
    public CreateTrainingHeader createTrainingHeader(String inputFile) {
        DocumentSource documentSource = null;
        String headerText = null;
        String affiliationText =  null;
        String authorText = null;
        List<String> res = new ArrayList<String>();
        CreateTrainingHeader obj = new CreateTrainingHeader();

        try {
            File file = new File(inputFile);
            String pdfFileName = file.getName();

            documentSource = DocumentSource.fromPdf(file);
            Document doc = parsers.getSegmentationParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());


            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(SegmentationLabel.HEADER);
            List<LayoutToken> tokenizationsFull = doc.getTokenizations();

            if (documentHeaderParts != null) {
                List<LayoutToken> tokenizations = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.a;
                    DocumentPointer dp2 = docPiece.b;

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizations.add(tokenizationsFull.get(i));
                    }
                }
                String header = getSectionHeaderFeatured(doc, documentHeaderParts, true);
                String rese = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    rese = label(header);

                    // buffer for the header block
                    StringBuilder bufferHeader = trainingExtraction(rese, true, tokenizations);
                    Language lang = languageUtilities.runLanguageId(bufferHeader.toString());
                    if (lang != null) {
                        doc.setLanguage(lang.getLangId());
                    }

                    // buffer for the affiliation+address block
                    StringBuilder bufferAffiliation =
                            parsers.getAffiliationAddressParser().trainingExtraction(rese, tokenizations);
                    // buffer for the date block
                    StringBuilder bufferDate = null;
                    // we need to rebuild the found date string as it appears
                    String input = "";
                    int q = 0;
                    StringTokenizer st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<date>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.trim().length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferDate = parsers.getDateParser().trainingExtraction(inputs);
                    }

                    // buffer for the name block
                    StringBuilder bufferName = null;
                    // we need to rebuild the found author string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<author>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferName = parsers.getAuthorParser().trainingExtraction(inputs, true);
                    }

                    // buffer for the reference block
                    StringBuilder bufferReference = null;
                    // we need to rebuild the found citation string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < tokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = tokenizations.get(q).getText();
                        String theTok = tokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q>0) && (q < tokenizations.size())) {
                                theTok = tokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<reference>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferReference = parsers.getCitationParser().trainingExtraction(inputs);
                    }

                    // write the TEI file to reflect the extract layout of the text as
                    // extracted from the pdf

                    headerText = "<?xml version=\"1.0\" ?>\n<tei>\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                            + pdfFileName.replace(".pdf", "")
                            + "\"/>\n\t</teiHeader>\n\t<text";

                    if (lang != null) {
                        // TODO: why English (Slava)
                        headerText += " xml:lang=\"en\"";
                    }
                    headerText += ">\n\t\t<front>\n";

                    headerText += bufferHeader.toString();
                    headerText += "\n\t\t</front>\n\t</text>\n</tei>\n";
                    obj.setHeader(headerText);

                    if (bufferAffiliation != null) {
                        if (bufferAffiliation.length() > 0) {
                            affiliationText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                            affiliationText += "\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\""
                                    + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">";
                            affiliationText += "\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>";
                            affiliationText += "\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\t\t\t\t\t\t<author>\n\n";
                            affiliationText += bufferAffiliation.toString();
                            affiliationText += "\n\t\t\t\t\t\t</author>\n\t\t\t\t\t</analytic>";
                            affiliationText += "\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>";
                            affiliationText += "\n\t</teiHeader>\n</tei>\n";
                            obj.setAffiliation(affiliationText);
                        }
                    }


                    if (bufferName != null) {
                        if (bufferName.length() > 0) {
                            authorText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                            authorText += "\n<tei xmlns=\"http://www.tei-c.org/ns/1.0\"" + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                                    + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">";
                            authorText += "\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>";
                            authorText += "\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\n\t\t\t\t\t\t<author>";
                            authorText += "\n\t\t\t\t\t\t\t<persName>\n";
                            authorText += bufferName.toString();
                            authorText += "\t\t\t\t\t\t\t</persName>\n";
                            authorText += "\t\t\t\t\t\t</author>\n\n\t\t\t\t\t</analytic>";
                            authorText += "\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>";
                            authorText += "\n\t</teiHeader>\n</tei>\n";
                            obj.setAuthor(authorText);
                        }
                    }
                }
            }
            else {
                System.out.println("no header found");
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            DocumentSource.close(documentSource, true);
        }
    }

    /**
     * Extract results from a labelled header. If boolean intro is true, the
     * extraction is stopped at the first "intro" tag identified (this tag marks
     * the begining of the description).
     *
     * @param result        result
     * @param intro         if intro
     * @param tokenizations list of tokens
     * @param biblio        biblio item
     * @return a biblio item
     */
    public BiblioItem resultExtraction(String result, boolean intro, List<LayoutToken> tokenizations, BiblioItem biblio) {
        StringTokenizer st = new StringTokenizer(result, "\n");
        String tagName = null;
        String tagValue = null;
        String localFeature = null;
        String s6 = null;
        String lastTag = null;
        SizedStack<String> tokenStack = new SizedStack<String>(3) ;
        Utility utl = new Utility();
        boolean corresTextFlag = false;
        int p = 0;

        List<ITag> authorAffiliationTagList = new ArrayList<>();
        String authorAffiliationText = "";

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String token = st.nextToken().trim();

            if (token.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(token, "\t");
            List<String> localFeatures = new ArrayList<String>();
            int i = 0;

            // boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
                    tagValue = s;
                    tokenStack.push(tagValue) ;

                    int p0 = p;
                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).getText();
                        if (tokOriginal.equals(" ")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                    if (p == tokenizations.size()) {
                        // either we are at the end of the header, or we might have
                        // a problematic token in tokenization for some reasons
                        if ((p - p0) > 2) {
                            // we loose the synchronicity, so we reinit p for the next token
                            p = p0;
                        }
                    }
                } else if (i == ll - 1) {
                    tagName = s;
                } else if (i == 11){
                    localFeature = s; // if linestarts
                    if (localFeature.equals("LINESTART")) {
                        tokenStack.clear();
                        tokenStack.push(tagValue);
                    }
                } else if (i == 12) {
                    s6 = s; //if caps
                } else {
                    localFeatures.add(s);
                }
                i++;
            }
            if (tagValue.toLowerCase().contains("correspond")){

                if (localFeature.equals("LINESTART") || s6.equals("ALLCAPS")) {
                    corresTextFlag = true;
                    biblio.setContextualWord(true);
                } else if (utl.inCorrespList(tokenStack)) {
                    corresTextFlag = true;
                    biblio.setContextualWord(true);
                }
            }
            if ((tagName.equals("<title>")) || (tagName.equals("I-<title>"))) {
                if (biblio.getTitle() != null) {
                    if (localFeatures.contains("LINESTART")) {
                        biblio.setTitle(biblio.getTitle() + " " + tagValue);
                    } else if (addSpace) {
                        biblio.setTitle(biblio.getTitle() + " " + tagValue);
                    } else
                        biblio.setTitle(biblio.getTitle() + tagValue);
                } else
                    biblio.setTitle(tagValue);
            } else if ((tagName.equals("<author>")) || (tagName.equals("I-<author>"))) {
                if ((lastTag == null) || ((lastTag != null) && (lastTag.endsWith("<author>")))) {
                    if (biblio.getAuthors() != null) {
                        if (addSpace) {
                            biblio.setAuthors(biblio.getAuthors() + " " + tagValue);
                            authorAffiliationText = " " + tagValue;
                            if(corresTextFlag) {
                                biblio.setCorresAuthors(biblio.getCorresAuthors() + " " + tagValue);
                            }
                        } else {
                            biblio.setAuthors(biblio.getAuthors() + tagValue);
                            authorAffiliationText = tagValue;
                            if(corresTextFlag) {
                                biblio.setCorresAuthors(biblio.getCorresAuthors() + tagValue);
                            }
                        }
                    } else {
                        biblio.addCorresFlagtoList(corresTextFlag);
                        biblio.setAuthors(tagValue);
                        authorAffiliationText = tagValue;
                        if(corresTextFlag) {
                            biblio.setCorresAuthors(tagValue);
                        }
                    }

                } else {
                    if (biblio.getAuthors() != null) {
                        if (addSpace) {
                            biblio.addCorresFlagtoList(corresTextFlag);
                            biblio.setAuthors(biblio.getAuthors() + " \n" + tagValue);
                            authorAffiliationText = " \n" + tagValue;
                            if(corresTextFlag) {
                                biblio.setCorresAuthors(biblio.getCorresAuthors() + " \n" + tagValue);
                            }
                        } else {
                            biblio.addCorresFlagtoList(corresTextFlag);
                            biblio.setAuthors(biblio.getAuthors() + "\n" + tagValue);
                            authorAffiliationText = "\n" + tagValue;
                            if(corresTextFlag) {
                                biblio.setCorresAuthors(biblio.getCorresAuthors() + "\n" + tagValue);
                            }
                        }
                    } else {
                        biblio.addCorresFlagtoList(corresTextFlag);
                        biblio.setAuthors(tagValue);
                        authorAffiliationText = tagValue;
                        if(corresTextFlag) {
                            biblio.setCorresAuthors(tagValue);

                        }
                    }
                }
            } else if ((tagName.equals("<tech>")) || (tagName.equals("I-<tech>"))) {
                biblio.setItem(BiblioItem.TechReport);
                if (biblio.getBookType() != null) {
                    if (addSpace) {
                        biblio.setBookType(biblio.getBookType() + " " + tagValue);
                    } else
                        biblio.setBookType(biblio.getBookType() + tagValue);
                } else
                    biblio.setBookType(tagValue);
            } else if ((tagName.equals("<location>")) || (tagName.equals("I-<location>"))) {
                if (biblio.getLocation() != null) {
                    if (addSpace)
                        biblio.setLocation(biblio.getLocation() + " " + tagValue);
                    else
                        biblio.setLocation(biblio.getLocation() + tagValue);
                } else
                    biblio.setLocation(tagValue);
            } else if ((tagName.equals("<date>")) || (tagName.equals("I-<date>"))) {
                // it appears that the same date is quite often repeated,
                // we should check, before adding a new date segment, if it is
                // not already present

                if (biblio.getPublicationDate() != null) {
                    if (addSpace) {
                        biblio.setPublicationDate(biblio.getPublicationDate() + " " + tagValue);
                    } else
                        biblio.setPublicationDate(biblio.getPublicationDate() + tagValue);
                } else
                    biblio.setPublicationDate(tagValue);
            } else if ((tagName.equals("<date-submission>")) || (tagName.equals("I-<date-submission>"))) {
                // it appears that the same date is quite often repeated,
                // we should check, before adding a new date segment, if it is
                // not already present

                if (biblio.getSubmissionDate() != null) {
                    if (addSpace) {
                        biblio.setSubmissionDate(biblio.getSubmissionDate() + " " + tagValue);
                    } else
                        biblio.setSubmissionDate(biblio.getSubmissionDate() + tagValue);
                } else
                    biblio.setSubmissionDate(tagValue);
            } else if ((tagName.equals("<pages>")) || (tagName.equals("<page>")) | (tagName.equals("I-<pages>")) || (tagName.equals("I-<page>"))) {
                if (biblio.getPageRange() != null) {
                    if (addSpace) {
                        biblio.setPageRange(biblio.getPageRange() + " " + tagValue);
                    } else
                        biblio.setPageRange(biblio.getPageRange() + tagValue);
                } else
                    biblio.setPageRange(tagValue);
            } else if ((tagName.equals("<editor>")) || (tagName.equals("I-<editor>"))) {
                if (biblio.getEditors() != null) {
                    if (addSpace) {
                        biblio.setEditors(biblio.getEditors() + " " + tagValue);
                    } else {
                        biblio.setEditors(biblio.getEditors() + tagValue);
                    }
                } else
                    biblio.setEditors(tagValue);
            } else if ((tagName.equals("<institution>")) || (tagName.equals("I-<institution>"))) {
                if (biblio.getInstitution() != null) {
                    if (addSpace) {
                        biblio.setInstitution(biblio.getInstitution() + "; " + tagValue);
                    } else
                        biblio.setInstitution(biblio.getInstitution() + tagValue);
                } else
                    biblio.setInstitution(tagValue);
            } else if ((tagName.equals("<note>")) || (tagName.equals("I-<note>"))) {
                if (biblio.getNote() != null) {
                    if (addSpace) {
                        biblio.setNote(biblio.getNote() + " " + tagValue);
                    } else {
                        biblio.setNote(biblio.getNote() + tagValue);
                    }
                } else {
                    biblio.setNote(tagValue);
                }
            } else if ((tagName.equals("<abstract>")) || (tagName.equals("I-<abstract>"))) {
                if (biblio.getAbstract() != null) {
                    if (addSpace) {
                        biblio.setAbstract(biblio.getAbstract() + " " + tagValue);
                    } else
                        biblio.setAbstract(biblio.getAbstract() + tagValue);
                } else
                    biblio.setAbstract(tagValue);
            } else if ((tagName.equals("<reference>")) || (tagName.equals("I-<reference>"))) {
                if (biblio.getReference() != null) {
                    if (addSpace) {
                        biblio.setReference(biblio.getReference() + " " + tagValue);
                    } else
                        biblio.setReference(biblio.getReference() + tagValue);
                } else
                    biblio.setReference(tagValue);
            } else if ((tagName.equals("<grant>")) || (tagName.equals("I-<grant>"))) {
                if (biblio.getGrant() != null) {
                    if (addSpace) {
                        biblio.setGrant(biblio.getGrant() + " " + tagValue);
                    } else
                        biblio.setGrant(biblio.getGrant() + tagValue);
                } else
                    biblio.setGrant(tagValue);
            } else if ((tagName.equals("<copyright>")) || (tagName.equals("I-<copyright>"))) {
                if (biblio.getCopyright() != null) {
                    if (addSpace) {
                        biblio.setCopyright(biblio.getCopyright() + " " + tagValue);
                    } else
                        biblio.setCopyright(biblio.getCopyright() + tagValue);
                } else
                    biblio.setCopyright(tagValue);
            } else if ((tagName.equals("<affiliation>")) || (tagName.equals("I-<affiliation>"))) {
                // affiliation **makers** should be marked SINGLECHAR LINESTART
                if (biblio.getAffiliation() != null) {
                    if ((lastTag != null) && (tagName.equals(lastTag) || lastTag.equals("I-<affiliation>"))) {
                        if (tagName.equals("I-<affiliation>")) {
                            biblio.setAffiliation(biblio.getAffiliation() + " ; " + tagValue);
                            authorAffiliationText = " ; " + tagValue;
                        } else if (addSpace) {
                            biblio.setAffiliation(biblio.getAffiliation() + " " + tagValue);
                            authorAffiliationText = " " + tagValue;
                        } else {
                            biblio.setAffiliation(biblio.getAffiliation() + tagValue);
                            authorAffiliationText = tagValue;
                        }
                    } else {
                        biblio.setAffiliation(biblio.getAffiliation() + " ; " + tagValue);
                        authorAffiliationText = " ; " + tagValue;
                    }
                } else {
                    biblio.setAffiliation(tagValue);
                    authorAffiliationText = tagValue;
                }
            } else if ((tagName.equals("<address>")) || (tagName.equals("I-<address>"))) {
                if (biblio.getAddress() != null) {
                    if (addSpace) {
                        biblio.setAddress(biblio.getAddress() + " " + tagValue);
                        authorAffiliationText = " " + tagValue;
                    } else {
                        biblio.setAddress(biblio.getAddress() + tagValue);
                        authorAffiliationText = tagValue;
                    }
                } else {
                    biblio.setAddress(tagValue);
                    authorAffiliationText = tagValue;
                }
            } else if ((tagName.equals("<email>")) || (tagName.equals("I-<email>"))) {
                if (biblio.getEmail() != null) {
                    if (tagName.equals("I-<email>")) {
                        biblio.setEmail(biblio.getEmail() + " ; " + tagValue);
                        authorAffiliationText = " ; " + tagValue;
                    }
                    else if (addSpace) {
                        biblio.setEmail(biblio.getEmail() + " " + tagValue);
                        authorAffiliationText = " " + tagValue;
                    }
                    else {
                        biblio.setEmail(biblio.getEmail() + tagValue);
                        authorAffiliationText = tagValue;
                    }
                } else {
                    biblio.setEmail(tagValue);
                    authorAffiliationText = tagValue;
               }
                if(corresTextFlag && tagValue != null) {
                    biblio.setCorresEmails(tagValue);
                }
            } else if ((tagName.equals("<web>")) || (tagName.equals("I-<web>"))) {
                if (biblio.getWeb() != null) {
                    if (tagName.equals("I-<web>"))
                        biblio.setWeb(biblio.getWeb() + " ; " + tagValue);
                    else if (addSpace)
                        biblio.setWeb(biblio.getWeb() + " " + tagValue);
                    else
                        biblio.setWeb(biblio.getWeb() + tagValue);
                } else
                    biblio.setWeb(tagValue);
            } else if ((tagName.equals("<pubnum>")) || (tagName.equals("I-<pubnum>"))) {
                if (biblio.getPubnum() != null) {
                    if (addSpace)
                        biblio.setPubnum(biblio.getPubnum() + " " + tagValue);
                    else
                        biblio.setPubnum(biblio.getPubnum() + tagValue);
                } else
                    biblio.setPubnum(tagValue);
            } else if ((tagName.equals("<keyword>")) || (tagName.equals("I-<keyword>"))) {
                if (biblio.getKeyword() != null) {
                    if (localFeatures.contains("LINESTART")) {
                        biblio.setKeyword(biblio.getKeyword() + " \n " + tagValue);
                    } else if (addSpace)
                        biblio.setKeyword(biblio.getKeyword() + " " + tagValue);
                    else
                        biblio.setKeyword(biblio.getKeyword() + tagValue);
                } else
                    biblio.setKeyword(tagValue);
            } else if ((tagName.equals("<phone>")) || (tagName.equals("I-<phone>"))) {
                if (tagName.equals("I-<phone>")) {
                    biblio.setPhones(tagValue);
                }
                else {
                    if (biblio.getPhoneList().size() > 0) {
                        int phoneListLatestIndex = biblio.getPhoneList().size() - 1;
                        String latestPhoneNumber = biblio.getPhoneList().get(phoneListLatestIndex);
                        latestPhoneNumber = getUpdatedPhoneString(tagValue, latestPhoneNumber, localFeature);
                        biblio.getPhoneList().set(phoneListLatestIndex, latestPhoneNumber);
                    }
                }
              //            else if ((s1.equals("<phone>")) || (s1.equals("I-<phone>"))) {
//                if (biblio.getPhone() != null) {
//                    if (addSpace)
//                        biblio.setPhone(biblio.getPhone() + " " + s2);
//                    else
//                        biblio.setPhone(biblio.getPhone() + s2);
//                } else
//                    biblio.setPhone(s2);
//        }
//            }
            } else if ((tagName.equals("<degree>")) || (tagName.equals("I-<degree>"))) {
                if (biblio.getDegree() != null) {
                    if (addSpace)
                        biblio.setDegree(biblio.getDegree() + " " + tagValue);
                    else
                        biblio.setDegree(biblio.getDegree() + tagValue);
                } else
                    biblio.setDegree(tagValue);
            } else if ((tagName.equals("<web>")) || (tagName.equals("I-<web>"))) {
                if (biblio.getWeb() != null) {
                    if (addSpace)
                        biblio.setWeb(biblio.getWeb() + " " + tagValue);
                    else
                        biblio.setWeb(biblio.getWeb() + tagValue);
                } else
                    biblio.setWeb(tagValue);
            } else if ((tagName.equals("<dedication>")) || (tagName.equals("I-<dedication>"))) {
                if (biblio.getDedication() != null) {
                    if (addSpace)
                        biblio.setDedication(biblio.getDedication() + " " + tagValue);
                    else
                        biblio.setDedication(biblio.getDedication() + tagValue);
                } else
                    biblio.setDedication(tagValue);
            } else if ((tagName.equals("<submission>")) || (tagName.equals("I-<submission>"))) {
                if (biblio.getSubmission() != null) {
                    if (addSpace)
                        biblio.setSubmission(biblio.getSubmission() + " " + tagValue);
                    else
                        biblio.setSubmission(biblio.getSubmission() + tagValue);
                } else
                    biblio.setSubmission(tagValue);
            } else if ((tagName.equals("<entitle>")) || (tagName.equals("I-<entitle>"))) {
                if (biblio.getEnglishTitle() != null) {
                    if (tagName.equals(lastTag)) {
                        if (localFeatures.contains("LINESTART")) {
                            biblio.setEnglishTitle(biblio.getEnglishTitle() + " " + tagValue);
                        } else if (addSpace)
                            biblio.setEnglishTitle(biblio.getEnglishTitle() + " " + tagValue);
                        else
                            biblio.setEnglishTitle(biblio.getEnglishTitle() + tagValue);
                    } else
                        biblio.setEnglishTitle(biblio.getEnglishTitle() + " ; " + tagValue);
                } else
                    biblio.setEnglishTitle(tagValue);
            } else if ((tagName.equals("<articlenote>")) || (tagName.equals("I-<articlenote>"))) {
                if (biblio.getArticleNote() != null) {
                    if (addSpace)
                        biblio.setArticleNote(biblio.getArticleNote() + " " + tagValue);
                    else
                        biblio.setArticleNote(biblio.getArticleNote() + tagValue);
                } else
                    biblio.setArticleNote(tagValue);
            } else if (((tagName.equals("<intro>")) || (tagName.equals("I-<intro>"))) && intro) {
                break;
            }

            if(tagName.equals(TagName.I_AUTHOR) || tagName.equals(TagName.AUTHOR)||tagName.equals(TagName.I_AFFILIATION)||
                    tagName.equals(TagName.AFFILIATION) || tagName.equals(TagName.I_ADDRESS)||tagName.equals(TagName.ADDRESS)) {
                if (tagName.equals(TagName.I_AUTHOR) || tagName.equals(TagName.I_AFFILIATION)) {
                    addAuthorAffiliationNewTag(tagName, authorAffiliationTagList, authorAffiliationText, token);
                } else if (tagName.equals(TagName.AUTHOR) || tagName.equals(TagName.AFFILIATION)) {
                    if (isIStartTagIdentified(authorAffiliationTagList)) {
                        addValuesWhenStartTagAlreadyIdentified(authorAffiliationTagList, authorAffiliationText, token);
                    } else {
                        addAuthorAffiliationNewTag(getTagName(tagName), authorAffiliationTagList, authorAffiliationText, token);
                    }
                } else if (tagName.equals(TagName.I_ADDRESS) || tagName.equals(TagName.ADDRESS)) {
                    setAddressTagValues(tagName, authorAffiliationTagList, authorAffiliationText, token);
                }
                authorAffiliationText = "";
            }

            lastTag = tagName;
        }

        biblio.setSequenceTag(authorAffiliationTagList);
        return biblio;
    }

    private void addValuesWhenStartTagAlreadyIdentified(List<ITag> authorAffiliationTagList, String authorAffiliationText, String token) {
        int lastIndex = authorAffiliationTagList.size() - 1;
        ITag lastITag = authorAffiliationTagList.get(lastIndex);
        lastITag.setITagText(lastITag.getITagText() + authorAffiliationText);
        lastITag.setITagLableText(lastITag.getITagLableText() + '\n' + token);
        authorAffiliationTagList.set(lastIndex, lastITag);
    }

    private void setAddressTagValues(String tagName, List<ITag> authorAffiliationTagList, String authorAffiliationText, String token) {
        int lastIndex = authorAffiliationTagList.size() - 1;
        ITag lastITag = authorAffiliationTagList.get(lastIndex);
        if (lastITag.getITagName().equals(TagName.I_AFFILIATION) ||
                (lastITag.getITagName().equals(TagName.I_ADDRESS) && tagName.equals(TagName.ADDRESS))) {
            lastITag.setITagText(lastITag.getITagText() + authorAffiliationText);
            lastITag.setITagLableText(lastITag.getITagLableText() + '\n' + token);
            authorAffiliationTagList.set(lastIndex, lastITag);
        } else if (tagName.equals(TagName.I_ADDRESS)) {
            addAuthorAffiliationNewTag(tagName, authorAffiliationTagList, authorAffiliationText, token);
        }
    }

    private boolean isIStartTagIdentified(List<ITag> authorAffiliationTagList) {
        return authorAffiliationTagList.size() > 0 &&
                (authorAffiliationTagList.get(authorAffiliationTagList.size() - 1).getITagName().equals(TagName.I_AUTHOR) ||
                        authorAffiliationTagList.get(authorAffiliationTagList.size() - 1).getITagName().equals(TagName.I_AFFILIATION));
    }

    private void addAuthorAffiliationNewTag(String tagName, List<ITag> authorAffiliationTagList, String authorAffiliationText, String token) {
        ITag iTag = new ITag();
        iTag.setITagName(tagName);
        iTag.setITagText(authorAffiliationText);
        iTag.setITagLableText(token);
        authorAffiliationTagList.add(iTag);
    }

    private String getTagName(String tagName) {
        if (tagName.equals(TagName.AUTHOR))
            return TagName.I_AUTHOR;
        else
            return TagName.I_AFFILIATION;
    }
  
    private String getUpdatedPhoneString(String tagValue, String latestPhoneNumber, String localFeature) {
        String hash = "#";
        if (localFeature.equals("LINESTART")) {
            latestPhoneNumber = latestPhoneNumber + RegexPattern.SPACE + tagValue;
        } else if (latestPhoneNumber.endsWith("/")) {
            latestPhoneNumber = latestPhoneNumber + tagValue;
        } else if (Pattern.matches(RegexPattern.STARTS_WITH + "\\(", tagValue)) {
            latestPhoneNumber = latestPhoneNumber + tagValue;
        } else if (Pattern.matches(RegexPattern.STARTS_WITH + RegexPattern.OPENING_BRACKET + RegexPattern.NUMBER +
                RegexPattern.OR + RegexPattern.BACKWARD_SLASH + RegexPattern.PLUS + RegexPattern.CLOSING_BRACKET +
                RegexPattern.ANYTHING, tagValue)) {
            if (Pattern.matches(RegexPattern.ANYTHING + RegexPattern.ALPHABET + RegexPattern.OPENING_BRACKET +
                    RegexPattern.OPENING_SQUARE_BRACKET + RegexPattern.COLON + hash + RegexPattern.PERIOD +
                    RegexPattern.CLOSING_SQUARE_BRACKET + RegexPattern.PLUS + RegexPattern.CLOSING_BRACKET +
                    RegexPattern.QUESTION_MARK + RegexPattern.ENDS_WITH, latestPhoneNumber))
                latestPhoneNumber = latestPhoneNumber + RegexPattern.SPACE + tagValue;
            else
                latestPhoneNumber = latestPhoneNumber + tagValue;
        } else if (Pattern.matches(RegexPattern.STARTS_WITH + RegexPattern.ALPHABET + RegexPattern.ANYTHING, tagValue)) {
            if (Pattern.matches(RegexPattern.ANYTHING + RegexPattern.BACKWARD_SLASH + RegexPattern.OPENING_BRACKET
                    + RegexPattern.ENDS_WITH, latestPhoneNumber))
                latestPhoneNumber = latestPhoneNumber + tagValue;
            else
                latestPhoneNumber = latestPhoneNumber + RegexPattern.SPACE + tagValue;
        } else {
            latestPhoneNumber = latestPhoneNumber + tagValue;
        }
        return latestPhoneNumber;
    }
    /**
     * amn-mod
     */
    private String replaceStr(String str, ArrayList<ArrayList<Integer>> arrayList) {
        StringBuilder sb = new StringBuilder();
        char[] chars = str.toCharArray();
        for (ArrayList<Integer> idx : arrayList) {
            int beg = idx.get(0);
            int end = idx.get(1);
            if (beg != 0) {
                if (end != str.length()) {
                    for (int i = 0; i < str.length(); i++) {
                        if (i < beg || i > end) {
                            sb.append(chars[i]);
                        }

                    }
                } else {
                    for (int i = 0; i < beg; i++) {
                        sb.append(chars[i]);
                    }
                }
            } else {
                for (int i = end + 1; i < str.length(); i++) {
                    sb.append(chars[i]);
                }
            }
        }
        return sb.toString();
    }
    /**
     * amn-mod
     */
    /**
     * Extract results from a labelled header in the training format without any
     * string modification.
     *
     * @param result        result
     * @param intro         if intro
     * @param tokenizations list of tokens
     * @return a result
     */
    private StringBuilder trainingExtraction(String result, boolean intro, List<LayoutToken> tokenizations) {
        // this is the main buffer for the whole header
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null;
        String s2 = null;
        String lastTag = null;

        int p = 0;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, "\t");
            // List<String> localFeatures = new ArrayList<String>();
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
                    s2 = TextUtilities.HTMLEncode(s);
                    //s2 = s;

                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).t();
                        if (tokOriginal.equals(" ")
                                || tokOriginal.equals("\u00A0")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                } else if (i == ll - 1) {
                    s1 = s;
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                    // localFeatures.add(s);
                }
                i++;
            }

            if (newLine) {
                buffer.append("<lb/>");
            }

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            String currentTag0 = null;
            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            if (lastTag != null) {
                testClosingTag(buffer, currentTag0, lastTag0);
            }

            boolean output;

            output = writeField(buffer, s1, lastTag0, s2, "<title>", "<docTitle>\n\t<titlePart>", addSpace);
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<author>", "<byline>\n\t<docAuthor>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<location>", "<address>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<address>", "<address>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<date>", "<date>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<date-submission>", "<date type=\"submission\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<booktitle>", "<booktitle>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<pages>", "<pages>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<publisher>", "<publisher>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<journal>", "<journal>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<institution>", "<byline>\n\t<affiliation>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<affiliation>", "<byline>\n\t<affiliation>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<volume>", "<volume>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<editor>", "<editor>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<note>", "<note type=\"other\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<abstract>", "<div type=\"abstract\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<email>", "<email>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<pubnum>", "<idno>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<keyword>", "<keyword>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<phone>", "<phone>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<degree>", "<note type=\"degree\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<web>", "<ptr type=\"web\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<dedication>", "<dedication>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<submission>", "<note type=\"submission\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<entitle>", "<note type=\"title\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<reference>", "<reference>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<copyright>", "<note type=\"copyright\">", addSpace);
            }
            if (!output) {
                // noinspection UnusedAssignment
                output = writeField(buffer, s1, lastTag0, s2, "<grant>", "<note type=\"grant\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<intro>", "<p type=\"introduction\">", addSpace);
            }

            /*if (((s1.equals("<intro>")) || (s1.equals("I-<intro>"))) && intro) {
                break;
            }*/
            lastTag = s1;

            if (!st.hasMoreTokens()) {
                if (lastTag != null) {
                    testClosingTag(buffer, "", currentTag0);
                }
            }
        }

        return buffer;
    }

    private void testClosingTag(StringBuilder buffer, String currentTag0, String lastTag0) {
        if (!currentTag0.equals(lastTag0)) {
            // we close the current tag
            if (lastTag0.equals("<title>")) {
                buffer.append("</titlePart>\n\t</docTitle>\n");
            } else if (lastTag0.equals("<author>")) {
                buffer.append("</docAuthor>\n\t</byline>\n");
            } else if (lastTag0.equals("<location>")) {
                buffer.append("</address>\n");
            } else if (lastTag0.equals("<date>")) {
                buffer.append("</date>\n");
            } else if (lastTag0.equals("<abstract>")) {
                buffer.append("</div>\n");
            } else if (lastTag0.equals("<address>")) {
                buffer.append("</address>\n");
            } else if (lastTag0.equals("<date-submission>")) {
                buffer.append("</date>\n");
            } else if (lastTag0.equals("<booktitle>")) {
                buffer.append("</booktitle>\n");
            } else if (lastTag0.equals("<pages>")) {
                buffer.append("</pages>\n");
            } else if (lastTag0.equals("<email>")) {
                buffer.append("</email>\n");
            } else if (lastTag0.equals("<publisher>")) {
                buffer.append("</publisher>\n");
            } else if (lastTag0.equals("<institution>")) {
                buffer.append("</affiliation>\n\t</byline>\n");
            } else if (lastTag0.equals("<keyword>")) {
                buffer.append("</keyword>\n");
            } else if (lastTag0.equals("<affiliation>")) {
                buffer.append("</affiliation>\n\t</byline>\n");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<reference>")) {
                buffer.append("</reference>\n");
            } else if (lastTag0.equals("<copyright>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<grant>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<entitle>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<submission>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<dedication>")) {
                buffer.append("</dedication>\n");
            } else if (lastTag0.equals("<web>")) {
                buffer.append("</ptr>\n");
            } else if (lastTag0.equals("<phone>")) {
                buffer.append("</phone>\n");
            } else if (lastTag0.equals("<pubnum>")) {
                buffer.append("</idno>\n");
            } else if (lastTag0.equals("<degree>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<intro>")) {
                buffer.append("</p>\n");
            }
        }
    }

    private boolean writeField(StringBuilder buffer, String s1, String lastTag0, String s2, String field, String outField, boolean addSpace) {
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (s1.equals(lastTag0) || (s1).equals("I-" + lastTag0)) {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else
                buffer.append("\n\t").append(outField).append(s2);
        }
        return result;
    }

    /**
     * Consolidate an existing list of recognized citations based on access to
     * external internet bibliographic databases.
     *
     * @param resHeader original biblio item
     * @return consolidated biblio item
     */
    public BiblioItem consolidateHeader(BiblioItem resHeader) {
        try {
            if (consolidator == null) {
                consolidator = new Consolidation();
            }
            consolidator.openDb();
            List<BiblioItem> bibis = new ArrayList<BiblioItem>();
            boolean valid = consolidator.consolidate(resHeader, bibis);
            if ((valid) && (bibis.size() > 0)) {
                BiblioItem bibo = bibis.get(0);
                if (bibo != null) {
                    BiblioItem.correct(resHeader, bibo);
                }
            }
            consolidator.closeDb();
        } catch (Exception e) {
            // e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return resHeader;
    }

    public List<Person> getContextualAuthorsSequence(String auth) {
        String[] authorSegments;
        List<Person> perList = new ArrayList<>();

        if (auth != "") {
            ArrayList<String> auts;
            authorSegments = auth.split("\n");
            for (int k = 0; k < authorSegments.length; k++) {
                auts = new ArrayList<String>();
                auts.add(authorSegments[k]);
                List<Person> localAuthors = parsers.getAuthorParser().processingHeader(auts);
                if (localAuthors != null) {
                    for (Person pers : localAuthors) {
                        boolean doesPerExist = false;
                        for(Person p: perList) {
                            if(pers.equals(p)) {
                                doesPerExist = true;
                                break;
                            }
                        }
                        if(!doesPerExist) {
                            perList.add(pers);
                        }

                    }
                }
            }
        }

        return perList;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    public String processingHeaderBlockNew(boolean consolidate, Document doc, BiblioItem resHeader) {
        try {

        String header;
        //if (doc.getBlockDocumentHeaders() == null) {
        header = doc.getHeaderFeatured(true, true);
        /*} else {
            header = doc.getHeaderFeatured(false, true);
        }*/
        List<LayoutToken> tokenizations = doc.getTokenizationsHeader();
//System.out.println(tokenizations.toString());

        if ((header != null) && (header.trim().length() > 0)) {
            String res = label(header);
            resHeader = resultExtraction(res, true, tokenizations, resHeader);

            /**
             * amn-mod
             */
            Boolean flagAdd = false;
            Boolean flagAff = false;
            StringBuilder sb = new StringBuilder();
            String emailClassifiedWrongly = "";
            String regex="(?:[a-z.0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
            Pattern pat= Pattern.compile(regex);
            ArrayList<ArrayList<Integer>> emailIdxAdd =new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> emailIdxAff =new ArrayList<ArrayList<Integer>>();
            //for emails classified as address
            String textAdd=resHeader.getAddress();
            if (textAdd != null) {
                Matcher mAdd = pat.matcher(textAdd.toLowerCase());
                while (mAdd.find()) {
                    flagAdd = true;
                    ArrayList<Integer> arList = new ArrayList<Integer>();
                    arList.add(mAdd.start());
                    arList.add(mAdd.end());
                    emailIdxAdd.add(arList);
                    textAdd = replaceStr(textAdd, emailIdxAdd);
                    resHeader.setAddress(textAdd);
                    emailClassifiedWrongly = mAdd.group();
                    resHeader.setEmail(resHeader.getEmail() + ";" + emailClassifiedWrongly);
                }
            }
            //for emails classified as affliations
            String textAff = resHeader.getAffiliation();
            if (textAff != null) {
                Matcher mAff = pat.matcher(textAff.toLowerCase());
                while (mAff.find()) {
                    flagAff = true;
                    ArrayList<Integer> arList = new ArrayList<Integer>();
                    arList.add(mAff.start());
                    arList.add(mAff.end());
                    emailIdxAff.add(arList);
                    textAff = replaceStr(textAff, emailIdxAff);
                    resHeader.setAffiliation(textAff);
                    emailClassifiedWrongly = mAff.group();
                    resHeader.setEmail(resHeader.getEmail() + ";" + emailClassifiedWrongly);
                }
            }

            for (ArrayList<Integer> idx : emailIdxAdd){
                emailIdxAff.add(idx);
            }



            /**
             * amn-mod
             */

            // language identification
            String contentSample = "";
            if (resHeader.getTitle() != null)
                contentSample += resHeader.getTitle();
            if (resHeader.getAbstract() != null)
                contentSample += "\n" + resHeader.getAbstract();
            if (resHeader.getKeywords() != null)
                contentSample += "\n" + resHeader.getKeywords();
            if (contentSample.length() < 200) {
                // we need more textual content to ensure that the language identification will be
                // correct
                contentSample += doc.getBody();
            }
            Language langu = languageUtilities.runLanguageId(contentSample);
            if (langu != null) {
                String lang = langu.getLangId();
                doc.setLanguage(lang);
                resHeader.setLanguage(lang);
            }

            if (resHeader != null) {
                if (resHeader.getAbstract() != null) {
                    resHeader.setAbstract(TextUtilities.dehyphenizeHard(resHeader.getAbstract()));
                    //resHeader.setAbstract(TextUtilities.dehyphenize(resHeader.getAbstract()));
                }
                BiblioItem.cleanTitles(resHeader);
                if (resHeader.getTitle() != null) {
                    // String temp =
                    // utilities.dehyphenizeHard(resHeader.getTitle());
                    String temp = TextUtilities.dehyphenize(resHeader.getTitle());
                    temp = temp.trim();
                    if (temp.length() > 1) {
                        if (temp.startsWith("1"))
                            temp = temp.substring(1, temp.length());
                        temp = temp.trim();
                    }
                    resHeader.setTitle(temp);
                }
                if (resHeader.getBookTitle() != null) {
                    resHeader.setBookTitle(TextUtilities.dehyphenize(resHeader.getBookTitle()));
                }

                resHeader.setOriginalAuthors(resHeader.getAuthors());
                List<Person> contextualPersonSequence = getContextualAuthorsSequence(resHeader.getCorresAuthors());
                boolean fragmentedAuthors = false;
                boolean hasMarker = false;
                List<Integer> authorsBlocks = new ArrayList<Integer>();
                String[] authorSegments;
                List<Boolean> corresFlags = null;
                if (resHeader.getAuthors() != null) {

                    ArrayList<String> auts;
                    authorSegments = resHeader.getAuthors().split("\n");
                    corresFlags = resHeader.getCorresFlagsList();
                    if (authorSegments.length > 1) {
                        fragmentedAuthors = true;
                    }
                    for (int k = 0; k < authorSegments.length; k++) {
                        auts = new ArrayList<String>();
                        boolean corresFlag = false;
                        auts.add(authorSegments[k]);
                        List<Person> localAuthors = parsers.getAuthorParser().processingHeader(auts);
                        corresFlag = corresFlags.get(k);
                        if (localAuthors != null) {
                            for (Person pers : localAuthors) {
                                if (corresFlag == true) {
                                    pers.setCorresp(true);
                                    pers.setContectualAuthorSeq(contextualPersonSequence);
                                }
                                resHeader.addUniqueAuthor(pers);
                                if (pers.getMarkers() != null) {
                                    hasMarker = true;
                                }
                                authorsBlocks.add(k);
                            }
                        }
                    }

                    resHeader.setFullAffiliations(
                            parsers.getAffiliationAddressParser().processReflow(res, tokenizations));

//                    resHeader.setAffiliationForSequenceTag(resHeader.getFullAffiliations());

                    resHeader.attachEmails();
                    resHeader.attachPhones(res);
                    resHeader.attachUrls(res);
                    boolean attached = false;
//                    if (fragmentedAuthors && !hasMarker) {
//                        if (resHeader.getFullAffiliations() != null) {
//                            if (authorSegments != null) {
//                                if (resHeader.getFullAffiliations().size() == authorSegments.length) {
//                                    int k = 0;
//                                    for (Person pers : resHeader.getFullAuthors()) {
//                                        if (k < authorsBlocks.size()) {
//                                            int indd = authorsBlocks.get(k);
//                                            if (indd < resHeader.getFullAffiliations().size()) {
//                                                pers.addAffiliation(resHeader.getFullAffiliations().get(indd));
//                                            }
//                                        }
//                                        k++;
//                                    }
//                                    attached = true;
//                                    resHeader.setFullAffiliations(null);
//                                    resHeader.setAffiliation(null);
//                                }
//                            }
//                        }
//                    }


                    processingAuthorsITag(resHeader.getSequenceTags());
                    resHeader.attachAffiliations();

//                    initilizeAuthorToSeqAuthor(resHeader.getSequenceTags(),resHeader.getFullAuthors());

//                    if (!attached) {
//                        resHeader.attachAffiliations();
//                    }

                    if (resHeader.getEditors() != null) {
                        ArrayList<String> edits = new ArrayList<String>();
                        edits.add(resHeader.getEditors());

                        resHeader.setFullEditors(parsers.getAuthorParser().processingHeader(edits));
                        // resHeader.setFullEditors(authorParser.processingCitation(edits));
                    }

                    if (resHeader.getReference() != null) {
                        BiblioItem refer = parsers.getCitationParser().processing(resHeader.getReference(), false);
                        if (refer != null)
                            BiblioItem.correct(resHeader, refer);
                    }
                }

                // keyword post-processing
                if (resHeader.getKeyword() != null) {
                    String keywords = TextUtilities.dehyphenize(resHeader.getKeyword());
                    keywords = BiblioItem.cleanKeywords(keywords);
                    resHeader.setKeyword(keywords.replace("\n", " ").replace("  ", " "));
                    List<Keyword> keywordsSegmented = BiblioItem.segmentKeywords(keywords);
                    if ((keywordsSegmented != null) && (keywordsSegmented.size() > 0))
                        resHeader.setKeywords(keywordsSegmented);
                }

                // DOI pass
                List<String> dois = doc.getDOIMatches();
                if (dois != null) {
                    if ((dois.size() == 1) && (resHeader != null)) {
                        resHeader.setDOI(dois.get(0));
                    }
                }

                if (consolidate) {
                    resHeader = consolidateHeader(resHeader);
                }

                // normalization of dates
                if (resHeader != null) {
                    if (resHeader.getPublicationDate() != null) {
                        List<Date> dates = parsers.getDateParser().processing(resHeader.getPublicationDate());
                        // most basic heuristic, we take the first date - to be
                        // revised...
                        if (dates != null) {
                            if (dates.size() > 0) {
                                resHeader.setNormalizedPublicationDate(dates.get(0));
                            }
                        }
                    }

                    if (resHeader.getSubmissionDate() != null) {
                        List<Date> dates = parsers.getDateParser().processing(resHeader.getSubmissionDate());
                        if (dates != null) {
                            if (dates.size() > 0) {
                                resHeader.setNormalizedSubmissionDate(dates.get(0));
                            }
                        }
                    }
                }
                if (resHeader.getAuthors() != null) {
                    resHeader.checkAuthorMarkerHasAstrik();
                    resHeader.setCorrespondingAuthor();
                }
            }
        } else {
            LOGGER.debug("WARNING: header is empty.");
        }


        TEIFormatter teiFormatter = new TEIFormatter(doc);
        StringBuilder tei = teiFormatter.toTEIHeader(resHeader, null, GrobidAnalysisConfig.builder().consolidateHeader(consolidate).build());
        tei.append("\t</text>\n");
        tei.append("</TEI>\n");
        //LOGGER.debug(tei.toString());
        return tei.toString();
        } catch (Exception e) {
            throw new GrobidException(e.getMessage());
        }
    }

    private void initilizeAuthorToSeqAuthor(List<ITag> sequenceTags, List<Person> personList) {
        for(int i=0;i<sequenceTags.size();i++){
            if(sequenceTags.get(i).getITagName().equals("I-<author>")) {
                for(int j=0;j<sequenceTags.get(i).getPersons().size();j++){
                    for(int k=0; k<personList.size();k++){
                        if(personList.get(k).equals(sequenceTags.get(i).getPersons().get(j))){
//                            sequenceTags.get(i).getPersons().get(j).
                            Person original = personList.get(k);
                            sequenceTags.get(i).getPersons().get(j).copy(original);
                            break;
                        }
                    }
                }

            }

        }

    }


}
