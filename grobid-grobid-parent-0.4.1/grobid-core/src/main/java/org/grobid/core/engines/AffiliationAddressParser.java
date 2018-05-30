package org.grobid.core.engines;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.Affiliation;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorAffiliationAddress;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.RegexPattern;
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Patrice Lopez
 */
public class AffiliationAddressParser extends AbstractParser {
    public Lexicon lexicon = Lexicon.getInstance();

    public AffiliationAddressParser() {
        super(GrobidModels.AFFIILIATON_ADDRESS);
    }

    public ArrayList<Affiliation> processing(String input) {
        try {
            if ((input == null) || (input.length() == 0)) {
                return null;
            }

            ArrayList<String> affiliationBlocks = new ArrayList<String>();
            input = input.trim();

            input = TextUtilities.dehyphenize(input);
            //StringTokenizer st = new StringTokenizer(input, " \n\t" + TextUtilities.fullPunctuations, true);
            //List<String> tokenizations = new ArrayList<String>();
			// TBD: pass the language object to the tokenizer 
			List<String> tokenizations = analyzer.tokenize(input);
            //while (st.hasMoreTokens()) {
            //    String tok = st.nextToken();
			int p = 0;
			for(String tok : tokenizations) {
                if (tok.length() == 0) continue;
                if (tok.equals("\n")) {
                    //tokenizations.add(" ");
					tokenizations.set(p, " ");
                } /*else {
                    tokenizations.add(tok);
                }*/
                if (!tok.equals(" ")) {
                    if (tok.equals("\n")) {
                        affiliationBlocks.add("@newline");
                    } else
                        affiliationBlocks.add(tok + " <affiliation>");
                }
				p++;
            }

            List<List<OffsetPosition>> placesPositions = new ArrayList<List<OffsetPosition>>();
            placesPositions.add(lexicon.inCityNames(input));

            String header = FeaturesVectorAffiliationAddress.addFeaturesAffiliationAddress(affiliationBlocks, placesPositions);

            // add context
//            st = new StringTokenizer(header, "\n");

            //TODO:
            String res = label(header);
            return resultBuilder(res, tokenizations, false); // don't use pre-labels
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * Post processing of extracted field affiliation and address.
     * Here the input string to be processed comes from a previous parser: the segmentation
     * can be kept and we filter in all tokens labelled <address> or <affiliation>.
     * We also need to keep the original tokenization information to recreate the exact
     * initial string.
     */
    public List<Affiliation> processReflow(String result, List<LayoutToken> tokenizations) {
        if ((result == null) || (result.length() == 0)) {
            return null;
        }
        List<String> affiliationBlocks = new ArrayList<String>();
        List<String> subTokenizations = new ArrayList<String>();

        filterAffiliationAddress(result, tokenizations, affiliationBlocks, subTokenizations);

        return processingReflow(affiliationBlocks, subTokenizations);
    }

    public List<Affiliation> processReflow(String result, String input) throws Exception {
        if ((result == null) || (result.length() == 0)) {
            return null;
        }
        List<String> affiliationBlocks = new ArrayList<String>();
        List<String> subTokenizations = new ArrayList<String>();

        filterAffiliationAddress(result, input, affiliationBlocks, subTokenizations);

        return processingReflow(affiliationBlocks, subTokenizations);
    }



    private void filterAffiliationAddress(String result,
                                          List<LayoutToken> tokenizations,
                                          List<String> affiliationBlocks,
                                          List<String> subTokenizations) {
        StringTokenizer st = new StringTokenizer(result, "\n");
//System.out.println(result);
        String lastLabel = null;
        int p = 0;
        List<LayoutToken> tokenizationsBuffer = null;
        boolean open = false;
        while (st.hasMoreTokens() && (p < tokenizations.size())) {
            String line = st.nextToken();
//System.out.println(line);
            if (line.trim().length() == 0) {
                affiliationBlocks.add("\n");
                lastLabel = null;
            }
            else {
                String[] s = line.split("\t");
                String s0 = s[0].trim();
                int p0 = p;
                boolean strop = false;
                tokenizationsBuffer = new ArrayList<LayoutToken>();
                String tokOriginal = null;
//System.out.println("s0 = " + s0);
                while ((!strop) && (p < tokenizations.size())) {
                    tokOriginal = tokenizations.get(p).getText().trim();
                    if (open) {
                        if (tokOriginal.equals("\n")) {
                            affiliationBlocks.add("@newline");
                        }
                    }
                    tokenizationsBuffer.add(tokenizations.get(p));
                    if (tokOriginal.equals(s0)) {
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
                        continue;
                    }
                }
//System.out.println("tokOriginal = " + tokOriginal);
                int ll = s.length;
                String label = s[ll-1];
                //String plainLabel = GenericTaggerUtils.getPlainLabel(label);
//System.out.println(label);
                if ((tokOriginal != null) && ( ((label.indexOf("affiliation") != -1) || (label.indexOf("address") != -1)) )) {
                    affiliationBlocks.add(tokOriginal + " " + label);
                    // add the content of tokenizationsBuffer
                    for(LayoutToken tokk : tokenizationsBuffer) {
                        subTokenizations.add(tokk.getText());
                    }
                    open = true;
                }
                else if (lastLabel != null) {
                    affiliationBlocks.add("\n");
                }

                if ((label.indexOf("affiliation") != -1) || (label.indexOf("address") != -1)) {
                    lastLabel = label;
                } else {
                    lastLabel = null;
                }
            }
        }

//System.out.println(subTokenizations.toString());
//System.out.println(affiliationBlocks.toString());
    }


    private void filterAffiliationAddress(String result,
                                          String input,
                                          List<String> affiliationBlocks,
                                          List<String> subTokenizations) throws Exception {
        List<String> tokenizations = analyzer.tokenize(input);
        StringTokenizer st = new StringTokenizer(result, "\n");
//System.out.println(result);
        String lastLabel = null;
        int p = 0;
        List<String> tokenizationsBuffer = null;
        boolean open = false;
        while (st.hasMoreTokens() && (p < tokenizations.size())) {
            String line = st.nextToken();
//System.out.println(line);
            if (line.trim().length() == 0) {
                affiliationBlocks.add("\n");
                lastLabel = null;
            }
            else {
                String[] s = line.split("\t");
                String s0 = s[0].trim();
                int p0 = p;
                boolean strop = false;
                tokenizationsBuffer = new ArrayList<String>();
                String tokOriginal = null;
//System.out.println("s0 = " + s0);
                while ((!strop) && (p < tokenizations.size())) {
                    tokOriginal = tokenizations.get(p).trim();
                    if (open) {
                        if (tokOriginal.equals("\n")) {
                            affiliationBlocks.add("@newline");
                        }
                    }
                    tokenizationsBuffer.add(tokenizations.get(p));
                    if (tokOriginal.equals(s0)) {
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
                        continue;
                    }
                }
//System.out.println("tokOriginal = " + tokOriginal);
                int ll = s.length;
                String label = s[ll-1];
                //String plainLabel = GenericTaggerUtils.getPlainLabel(label);
//System.out.println(label);
                if ((tokOriginal != null) && ( ((label.indexOf("affiliation") != -1) || (label.indexOf("address") != -1)) )) {
                    affiliationBlocks.add(tokOriginal + " " + label);
                    // add the content of tokenizationsBuffer
                    for(String  tokk : tokenizationsBuffer) {
                        subTokenizations.add(tokk);
                    }
                    open = true;
                }
                else if (lastLabel != null) {
                    affiliationBlocks.add("\n");
                }

                if ((label.indexOf("affiliation") != -1) || (label.indexOf("address") != -1)) {
                    lastLabel = label;
                } else {
                    lastLabel = null;
                }
            }
        }

//System.out.println(subTokenizations.toString());
//System.out.println(affiliationBlocks.toString());
    }

    private ArrayList<Affiliation> processingReflow(List<String> affiliationBlocks, List<String> tokenizations) {
        String res = runReflow(affiliationBlocks, tokenizations);
        return resultBuilder(res, tokenizations, false); // normally use pre-label because it is a reflow
    }


    static class DebugTahher {
        private String str = "";

        public void add(String s) {
            str += s;
        }

        public void clear() {
            str = "";
        }

        String[] split;


        public boolean parse() {
            System.out.println("Parsing:\n" + str + "\n------------------");
            split = str.split("\n");

            return true;
        }

        public int size() {
            return split.length;
        }


    }

    private String runReflow(List<String> affiliationBlocks,
                             List<String> tokenizations) {
//        StringBuilder res = new StringBuilder();
//        DebugTahher tagger = new DebugTahher();
        try {
            List<List<OffsetPosition>> placesPositions = new ArrayList<List<OffsetPosition>>();
            placesPositions.add(lexicon.inCityNames(tokenizations));
            String header =
                    FeaturesVectorAffiliationAddress.addFeaturesAffiliationAddress(affiliationBlocks, placesPositions);

            if ((header == null) || (header.trim().length() == 0)) {
                return null;
            }

            String res = label(header);
//            res = label(res);

            return res;
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }


    public ArrayList<Affiliation> resultBuilder(String result,
                                                 List<String> tokenizations,
                                                 boolean usePreLabel) {
        ArrayList<Affiliation> fullAffiliations = null;
         BiblioItem biblioItem = new BiblioItem();

        if (result == null) {
            return fullAffiliations;
        }

        try {
            //System.out.println(tokenizations.toString());
            // extract results from the processed file
            if ((result == null) || (result.length() == 0)) {
                return null;
            }

            StringTokenizer st2 = new StringTokenizer(result, "\n");
            String lastTag = null;
            org.grobid.core.data.Affiliation aff = new Affiliation();
            int lineCount = 0;
            boolean hasInstitution;
            boolean hasDepartment = false;
            boolean hasAddress = false;
            boolean hasLaboratory;
            boolean newMarker = false;
            boolean useMarker = false;
            String currentMarker = null;
            boolean isIAddressTagFound = false;
            int p = 0;

            while (st2.hasMoreTokens()) {
                boolean addSpace = false;
                String line = st2.nextToken();
                Integer lineCountInt = lineCount;

                if (line.trim().length() == 0) {
                    if (aff.notNull()) {
                        if (fullAffiliations == null) {
                            fullAffiliations = new ArrayList<Affiliation>();
                        }
                        if (biblioItem.isAffiliationUnique(aff, fullAffiliations))
                            fullAffiliations.add(aff);
                            aff = new Affiliation();
                            currentMarker = null;
                    }
                    hasInstitution = false;
                    hasDepartment = false;
                    hasLaboratory = false;
                    hasAddress = false;
                    continue;
                }
                StringTokenizer st3 = new StringTokenizer(line, "\t");
                int ll = st3.countTokens();
                int i = 0;
                String label = null; // predicted label
                String lexicalToken = null; // lexical token
                String preLabel = null; // pre-label
                ArrayList<String> localFeatures = new ArrayList<String>();

                while (st3.hasMoreTokens()) {
                    String s = st3.nextToken().trim();
                    if (i == 0) {
                        lexicalToken = s; // lexical token

                        boolean strop = false;
                        while ((!strop) && (p < tokenizations.size())) {
                            String tokOriginal = tokenizations.get(p);
                            if (tokOriginal.equals(" ")) {
                                addSpace = true;
                            } else if (tokOriginal.equals(s)) {
                                strop = true;
                            }
                            p++;
                        }
                    } else if (i == ll - 2) {
                        preLabel = s; // pre-label
                    } else if (i == ll - 1) {
                        label = s; // label
                    } else {
                        localFeatures.add(s);
                    }
                    i++;
                }

                if (preLabel.equals("I-<affiliation>")) {
                    if (aff.notNull()) {
                        if (fullAffiliations == null)
                            fullAffiliations = new ArrayList<Affiliation>();
                        if (biblioItem.isAffiliationUnique(aff, fullAffiliations))
                            fullAffiliations.add(aff);
                    }

                    aff = new Affiliation();
                    hasInstitution = false;
                    hasLaboratory = false;
                    hasDepartment = false;
                    hasAddress = false;
                }

                if(preLabel.contains("I-"))
                {
                    if(isIAddressTagFound && preLabel.equals("I-<address>")) {
                        if (aff.notNull()) {
                            if (fullAffiliations == null)
                                fullAffiliations = new ArrayList<>();
                            if (biblioItem.isAffiliationUnique(aff, fullAffiliations))
                                fullAffiliations.add(aff);
                        }
                        aff = new Affiliation();
                    }

                    isIAddressTagFound = preLabel.equals("I-<address>");
                }

                if (lexicalToken != null) {
                    aff.appendText(lexicalToken);
                }

                if (label.equals("<marker>")) {
                    if (currentMarker == null)
                        currentMarker = lexicalToken;
                    else {
                        if (addSpace) {
                            currentMarker += " " + lexicalToken;
                        } else
                            currentMarker += lexicalToken;
                    }
                    aff.setMarker(currentMarker);
                    newMarker = false;

                } else if (label.equals("I-<marker>")) {
                    currentMarker = lexicalToken;
                    //newMarker = true;
                    if (currentMarker != null) {
                        aff.setMarker(currentMarker);
                    }
                    useMarker = true;
                }

                if (newMarker) {
                    if (aff.notNull()) {
                        if (fullAffiliations == null)
                            fullAffiliations = new ArrayList<Affiliation>();
                        if (biblioItem.isAffiliationUnique(aff, fullAffiliations))
                            fullAffiliations.add(aff);
                    }

                    aff = new Affiliation();
                    hasInstitution = false;
                    hasLaboratory = false;
                    hasDepartment = false;
                    hasAddress = false;

                    if (currentMarker != null) {
                        aff.setMarker(currentMarker);
                    }
                    newMarker = false;
                }

                else if (label.equals("<institution>") || label.equals("I-<institution>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && (preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))
                            ) {
                        hasInstitution = true;
                        if (aff.getInstitutions() != null) {
                            if (label.equals("I-<institution>") &&
                                    (localFeatures.contains("LINESTART"))) {
                                // new affiliation
                               /* if (aff.notNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);

                                }
                                hasInstitution = true;
                                hasDepartment = false;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();*/

                                hasInstitution = true;
                                hasDepartment = false;
                                hasLaboratory = false;
                                hasAddress = false;



                                aff.addInstitution(lexicalToken);
                                if (currentMarker != null)
                                    aff.setMarker(currentMarker.trim());
                            } else if (label.equals("I-<institution>") && hasInstitution && hasAddress &&
                                    (!lastTag.equals("<institution>"))) {
                                // new affiliation
                               /* if (aff.notNull()) {
                                    if (fullAffiliations == null) {
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    }
                                    fullAffiliations.add(aff);

                                }
                                hasInstitution = true;
                                hasDepartment = false;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();*/

                                hasInstitution = true;
                                hasDepartment = false;
                                hasLaboratory = false;
                                hasAddress = false;



                                aff.addInstitution(lexicalToken);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (label.equals("I-<institution>")) {
                                // we have multiple institutions for this affiliation
                                //aff.addInstitution(aff.institution);
                                aff.addInstitution(lexicalToken);
                            } else if (addSpace) {
                                aff.extendLastInstitution(" " + lexicalToken);
                            } else {
                                aff.extendLastInstitution(lexicalToken);
                            }
                        } else {
                            aff.addInstitution(lexicalToken);
                        }
                    } else if ((usePreLabel) && (preLabel.equals("<address>") || preLabel.equals("I-<address>"))) {
                        // that's a piece of the address badly labelled according to the model
                        if (aff.getAddressString() != null) {
                            if (addSpace) {
                                aff.setAddressString(aff.getAddressString() + " " + lexicalToken);
                            } else {
                                aff.setAddressString(aff.getAddressString() + lexicalToken);
                            }
                        } else {
                            aff.setAddressString(lexicalToken);
                        }
                    }
                } else if (label.equals("<addrLine>") || label.equals("I-<addrLine>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((preLabel.equals("<address>") || preLabel.equals("I-<address>"))))) {
                        if (aff.getAddrLine() != null) {
                            if (label.equals(lastTag) || lastTag.equals("I-<addrLine>")) {
                                if (label.equals("I-<addrLine>")) {
                                    aff.setAddrLine(aff.getAddrLine() + " ; " + lexicalToken);
                                } else if (addSpace) {
                                    aff.setAddrLine(aff.getAddrLine() + " " + lexicalToken);
                                } else {
                                    aff.setAddrLine(aff.getAddrLine() + lexicalToken);
                                }
                            } else {
                                aff.setAddrLine(aff.getAddrLine() + ", " + lexicalToken);
                            }
                        } else {
                            aff.setAddrLine(lexicalToken);
                        }
                        hasAddress = true;
                    } else if ((usePreLabel) && ((preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (label.equals(lastTag)) {
                                if (addSpace) {
                                    aff.setAffiliationString(aff.getAffiliationString() + " " + lexicalToken);
                                } else {
                                    aff.setAffiliationString(aff.getAffiliationString() + lexicalToken);
                                }
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + " ; " + lexicalToken);
                            }
                        } else {
                            aff.setAffiliationString(lexicalToken);
                        }
                    }
                } else if (label.equals("<department>") || label.equals("I-<department>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && (preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))
                            ) {
                        if (aff.getDepartments() != null) {
                            /*if (localFeatures.contains("LINESTART"))
                                       aff.department += " " + lexicalToken;*/

                            if ((label.equals("I-<department>")) &&
                                    (localFeatures.contains("LINESTART"))
                                    ) {
                               /* if (aff.notNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasDepartment = true;
                                hasLaboratory = false;
                                hasAddress = false;

                               aff = new Affiliation();*/

                                hasInstitution = false;
                                hasDepartment = true;
                                hasLaboratory = false;
                                hasAddress = false;



                                aff.addDepartment(lexicalToken);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if ((label.equals("I-<department>")) && hasDepartment && hasAddress &&
                                    !lastTag.equals("<department>")) {
                               /* if (aff.notNull()) {
                                    if (fullAffiliations == null) {
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    }
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasDepartment = true;
                                hasAddress = false;
                                hasLaboratory = false;

                                aff = new Affiliation();*/

                                hasInstitution = false;
                                hasDepartment = true;
                                hasAddress = false;
                                hasLaboratory = false;


                                aff.addDepartment(lexicalToken);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (label.equals("I-<department>")) {
                                // we have multiple departments for this affiliation
                                aff.addDepartment(lexicalToken);
                                //aff.department = lexicalToken;
                            } else if (addSpace) {
                                //aff.extendFirstDepartment(" " + lexicalToken);
                                aff.extendLastDepartment(" " + lexicalToken);
                            } else {
                                //aff.extendFirstDepartment(lexicalToken);
                                aff.extendLastDepartment(lexicalToken);
                            }
                        } else if (aff.getInstitutions() != null) {
                            /*if (localFeatures.contains("LINESTART"))
                                       aff.department += " " + lexicalToken;*/

                            if ((label.equals("I-<department>")) && hasAddress &&
                                    (localFeatures.contains("LINESTART"))
                                    ) {
                               /* if (aff.notNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasDepartment = true;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();*/
                                hasInstitution = false;
                                hasDepartment = true;
                                hasLaboratory = false;
                                hasAddress = false;


                                aff.addDepartment(lexicalToken);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else {
                                aff.addDepartment(lexicalToken);
                            }
                        } else {
                            aff.addDepartment(lexicalToken);
                        }
                    } else if ((usePreLabel) && (preLabel.equals("<address>") || preLabel.equals("I-<address>"))) {
                        if (aff.getAddressString() != null) {
                            if (addSpace) {
                                aff.setAddressString(aff.getAddressString() + " " + lexicalToken);
                            } else {
                                aff.setAddressString(aff.getAddressString() + lexicalToken);
                            }
                        } else {
                            aff.setAddressString(lexicalToken);
                        }
                    }
                } else if (label.equals("<laboratory>") || label.equals("I-<laboratory>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && (preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))
                            ) {
                        hasLaboratory = true;
                        if (aff.getLaboratories() != null) {
                            if (label.equals("I-<laboratory>") &&
                                    (localFeatures.contains("LINESTART"))) {
                                // new affiliation
                                /*if (aff.notNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasLaboratory = true;
                                hasDepartment = false;
                                hasAddress = false;
                                aff = new Affiliation();*/
                                hasInstitution = false;
                                hasLaboratory = true;
                                hasDepartment = false;
                                hasAddress = false;


                                aff.addLaboratory(lexicalToken);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (label.equals("I-<laboratory>")
                                    && hasLaboratory
                                    && hasAddress
                                    && (!lastTag.equals("<laboratory>"))) {
                                // new affiliation
                                /*if (aff.notNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasLaboratory = true;
                                hasDepartment = false;
                                hasAddress = false;
                                aff = new Affiliation();*/

                                hasInstitution = false;
                                hasLaboratory = true;
                                hasDepartment = false;
                                hasAddress = false;


                                aff.addLaboratory(lexicalToken);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (label.equals("I-<laboratory>")) {
                                // we have multiple laboratories for this affiliation
                                aff.addLaboratory(lexicalToken);
                            } else if (addSpace) {
                                aff.extendLastLaboratory(" " + lexicalToken);
                            } else {
                                aff.extendLastLaboratory(lexicalToken);
                            }
                        } else {
                            aff.addLaboratory(lexicalToken);
                        }
                    } else if ((usePreLabel) && (preLabel.equals("<address>") || preLabel.equals("I-<address>"))) {
                        // that's a piece of the address badly labelled
                        if (aff.getAddressString() != null) {
                            if (addSpace) {
                                aff.setAddressString(aff.getAddressString() + " " + lexicalToken);
                            } else {
                                aff.setAddressString(aff.getAddressString() + lexicalToken);
                            }
                        } else {
                            aff.setAddressString(lexicalToken);
                        }
                    }
                } else if (label.equals("<country>") || label.equals("I-<country>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((preLabel.equals("<address>") || preLabel.equals("I-<address>"))))) {
                        if (aff.getCountry() != null) {
                            if (label.equals("I-<country>")) {
                                aff.setCountry(aff.getCountry() + ", " + lexicalToken);
                            } else if (addSpace) {
                                aff.setCountry(aff.getCountry() + " " + lexicalToken);
                            } else {
                                aff.setCountry(aff.getCountry() + lexicalToken);
                            }
                        } else {
                            aff.setCountry(lexicalToken);
                        }
                        hasAddress = true;
                    } else if ((usePreLabel) && ((preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + lexicalToken);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + lexicalToken);
                            }
                        } else {
                            aff.setAffiliationString(lexicalToken);
                        }
                    }
                } else if (label.equals("<postCode>") || label.equals("I-<postCode>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((preLabel.equals("<address>") || preLabel.equals("I-<address>"))))) {
                        if (aff.getPostCode() != null) {
                            if (label.equals("I-<postCode>")) {
                                aff.setPostCode(aff.getPostCode() + ", " + lexicalToken);
                            } else if (addSpace) {
                                aff.setPostCode(aff.getPostCode() + " " + lexicalToken);
                            } else {
                                aff.setPostCode(aff.getPostCode() + lexicalToken);
                            }
                        } else {
                            aff.setPostCode(lexicalToken);
                        }
                    } else if ((usePreLabel) && ((preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + lexicalToken);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + lexicalToken);
                            }
                        } else {
                            aff.setAffiliationString(lexicalToken);
                        }
                    }
                } else if (label.equals("<postBox>") || label.equals("I-<postBox>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((preLabel.equals("<address>") || preLabel.equals("I-<address>"))))) {
                        if (aff.getPostBox() != null) {
                            if (label.equals("I-<postBox>")) {
                                aff.setPostBox(aff.getPostBox() + ", " + lexicalToken);
                            } else if (addSpace) {
                                aff.setPostBox(aff.getPostBox() + " " + lexicalToken);
                            } else {
                                aff.setPostBox(aff.getPostBox() + lexicalToken);
                            }
                        } else {
                            aff.setPostBox(lexicalToken);
                        }
                    } else if ((usePreLabel) && ((preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + lexicalToken);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + lexicalToken);
                            }
                        } else {
                            aff.setAffiliationString(lexicalToken);
                        }
                    }
                } else if (label.equals("<region>") || label.equals("I-<region>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((preLabel.equals("<address>") || preLabel.equals("I-<address>"))))) {
                        if (aff.getRegion() != null) {
                            if (label.equals("I-<region>")) {
                                aff.setRegion(aff.getRegion() + ", " + lexicalToken);
                            } else if (addSpace) {
                                aff.setRegion(aff.getRegion() + " " + lexicalToken);
                            } else {
                                aff.setRegion(aff.getRegion() + lexicalToken);
                            }
                        } else {
                            aff.setRegion(lexicalToken);
                        }
                    } else if ((usePreLabel) && ((preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + lexicalToken);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + lexicalToken);
                            }
                        } else {
                            aff.setAffiliationString(lexicalToken);
                        }
                    }
                } else if (label.equals("<settlement>") || label.equals("I-<settlement>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((preLabel.equals("<address>") || preLabel.equals("I-<address>"))))) {
                        if (aff.getSettlement() != null) {
                            if (label.equals("I-<settlement>")) {
                                aff.setSettlement(aff.getSettlement() + ", " + lexicalToken);
                            } else if (addSpace) {
                                aff.setSettlement(aff.getSettlement() + " " + lexicalToken);
                            } else {
                                aff.setSettlement(aff.getSettlement() + lexicalToken);
                            }
                        } else {
                            aff.setSettlement(lexicalToken);
                        }
                        hasAddress = true;
                    } else if ((usePreLabel) && ((preLabel.equals("<affiliation>") || preLabel.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + lexicalToken);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + lexicalToken);
                            }
                        } else {
                            aff.setAffiliationString(lexicalToken);
                        }
                    }
                }

                lastTag = label;
                lineCount++;
                newMarker = false;
            }
            if (aff.notNull()) {
                if (fullAffiliations == null)
                    fullAffiliations = new ArrayList<Affiliation>();
                if (biblioItem.isAffiliationUnique(aff, fullAffiliations))
                    fullAffiliations.add(aff);
                hasInstitution = false;
                hasDepartment = false;
                hasAddress = false;
            }

            // we clean a little bit
            if (fullAffiliations != null) {
                for (Affiliation affi : fullAffiliations) {
                    affi.clean();
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return fullAffiliations;
    }

    /**
     * Extract results from a labelled header in the training format without any string modification.
     */
    public StringBuilder trainingExtraction(String result,
                                           List<LayoutToken> tokenizations) {
        if ((result == null) || (result.length() == 0)) {
            return null;
        }

        List<String> affiliationBlocks = new ArrayList<String>();
        List<String> tokenizationsAffiliation = new ArrayList<String>();

        filterAffiliationAddress(result, tokenizations, affiliationBlocks, tokenizationsAffiliation);
        String resultAffiliation = runReflow(affiliationBlocks, tokenizationsAffiliation);

        StringBuilder bufferAffiliation = new StringBuilder();

        if (resultAffiliation == null) {
            return bufferAffiliation;
        }

        StringTokenizer st = new StringTokenizer(resultAffiliation, "\n");
        String tagName = null;
        String tagValue = null;
        String preLabel = null;
        String lastTag = null;

        int p = 0;

        String currentTag0 = null;
        String lastTag0 = null;
        boolean hasAddressTag = false;
        boolean start = true;
        boolean tagClosed = false;
        boolean isIAddressTagFound = false;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer tokenizedString = new StringTokenizer(tok, "\t");
            ArrayList<String> localFeatures = new ArrayList<String>();
            int i = 0;

            boolean newLine = false;
            int ll = tokenizedString.countTokens();
            while (tokenizedString.hasMoreTokens()) {
                String localFeature = tokenizedString.nextToken().trim();
                if (i == 0) {
                    tagValue = TextUtilities.HTMLEncode(localFeature);

                    boolean strop = false;
                    while ((!strop) && (p < tokenizationsAffiliation.size())) {
                        String tokOriginal = tokenizationsAffiliation.get(p);
                        if (tokOriginal.equals(" ")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(localFeature)) {
                            strop = true;
                        }
                        p++;
                    }
                } else if (i == ll - 2) {
                    preLabel = localFeature;
                } else if (i == ll - 1) {
                    tagName = localFeature;
                } else {
                    localFeatures.add(localFeature);
                    if (localFeature.equals("LINESTART") && !start) {
                        newLine = true;
                        start = false;
                    } else if (localFeature.equals("LINESTART")) {
                        start = false;
                    }
                }
                i++;
            }


            lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            currentTag0 = null;
            if (tagName != null) {
                if (tagName.startsWith("I-")) {
                    currentTag0 = tagName.substring(2, tagName.length());
                } else {
                    currentTag0 = tagName;
                }
            }

            if (lastTag != null) {
                tagClosed = testClosingTag(bufferAffiliation, currentTag0, lastTag0);
            } else
                tagClosed = false;

            if (newLine) {
                if (tagClosed) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<lb/>\n");
                } else {
                    bufferAffiliation.append("<lb/>");
                }
            }

            if (preLabel.equals("I-<affiliation>")) {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }
                if (bufferAffiliation.toString().contains("<affiliation>"))
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");

                bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
            }

            if(preLabel.contains("I-")) {
                if (isIAddressTagFound && preLabel.equals("I-<address>")) {
                    if (hasAddressTag) {
                        bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    }
                    if (bufferAffiliation.toString().contains("<affiliation>"))
                        bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");

                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    bufferAffiliation.append("\t\t\t\t\t\t<address>\n");
                    lastTag = tagName;

                }

                isIAddressTagFound = preLabel.equals("I-<address>");
            }

            String output = writeField(tagName, lastTag0, tagValue, "<marker>", "<marker>", addSpace, 7);
            if (output != null) {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                }
                bufferAffiliation.append(output);
                hasAddressTag = false;
                lastTag = tagName;
                continue;
            } else {
                output = writeField(tagName, lastTag0, tagValue, "<institution>", "<orgName type=\"institution\">", addSpace, 7);
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<department>", "<orgName type=\"department\">", addSpace, 7);
            } else {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }

                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<laboratory>", "<orgName type=\"laboratory\">", addSpace, 7);
            } else {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }

                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<addrLine>", "<addrLine>", addSpace, 8);
            } else {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }

                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<postCode>", "<postCode>", addSpace, 8);
            } else {
                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<postBox>", "<postBox>", addSpace, 8);
            } else {
                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<region>", "<region>", addSpace, 8);
            } else {
                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<settlement>", "<settlement>", addSpace, 8);
            } else {
                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<country>", "<country>", addSpace, 8);
            } else {
                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output == null) {
                output = writeField(tagName, lastTag0, tagValue, "<other>", "<other>", addSpace, 8);
            } else {
                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }

                bufferAffiliation.append(output);
                lastTag = tagName;
                continue;
            }
            if (output != null) {
                if (bufferAffiliation.length() > 0) {
                    if (bufferAffiliation.charAt(bufferAffiliation.length() - 1) == '\n') {
                        bufferAffiliation.deleteCharAt(bufferAffiliation.length() - 1);
                    }
                }
                bufferAffiliation.append(output);
            }
            lastTag = tagName;
        }

        if (lastTag != null) {
            if (lastTag.startsWith("I-")) {
                lastTag0 = lastTag.substring(2, lastTag.length());
            } else {
                lastTag0 = lastTag;
            }
            currentTag0 = "";
            testClosingTag(bufferAffiliation, currentTag0, lastTag0);
            if (hasAddressTag) {
                bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
            }
            bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
        }

        return bufferAffiliation;
    }

    private String writeField(String s1,
                              String lastTag0,
                              String s2,
                              String field,
                              String outField,
                              boolean addSpace,
                              int nbIndent) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if ((s1.equals("<other>") || s1.equals("I-<other>"))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                for (int i = 0; i < nbIndent; i++) {
                    result += "\t";
                }
                result += outField + s2;
            }
        }
        return result;
    }

    private boolean testClosingTag(StringBuilder buffer,
                                   String currentTag0,
                                   String lastTag0) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<institution>")) {
                buffer.append("</orgName>\n");
            } else if (lastTag0.equals("<department>")) {
                buffer.append("</orgName>\n");
            } else if (lastTag0.equals("<laboratory>")) {
                buffer.append("</orgName>\n");
            } else if (lastTag0.equals("<addrLine>")) {
                buffer.append("</addrLine>\n");
            } else if (lastTag0.equals("<postCode>")) {
                buffer.append("</postCode>\n");
            } else if (lastTag0.equals("<postBox>")) {
                buffer.append("</postBox>\n");
            } else if (lastTag0.equals("<region>")) {
                buffer.append("</region>\n");
            } else if (lastTag0.equals("<settlement>")) {
                buffer.append("</settlement>\n");
            } else if (lastTag0.equals("<country>")) {
                buffer.append("</country>\n");
            } else if (lastTag0.equals("<marker>")) {
                buffer.append("</marker>\n");
            } else if (lastTag0.equals("<other>")) {
                buffer.append("\n");
            } else {
                res = false;
            }
        }
        return res;
    }
}