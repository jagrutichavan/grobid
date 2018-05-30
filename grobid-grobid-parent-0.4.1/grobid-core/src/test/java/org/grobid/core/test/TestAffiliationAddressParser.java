package org.grobid.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.grobid.core.data.Affiliation;
import org.grobid.core.engines.AffiliationAddressParser;
import org.grobid.core.engines.Engine;
import org.junit.Before;
import org.junit.Test;

/**
 *  @author Patrice Lopez
 */
public class TestAffiliationAddressParser extends EngineTest{
	AffiliationAddressParser affiliationAddressParser;

	@Before
	public void init(){
		engine = new Engine();
		affiliationAddressParser = new AffiliationAddressParser();
	}
	
	@Test
	public void testParser() throws Exception {


		String affiliationSequence1 = "Atomic Physics Division, Department of Atomic Physics and Luminescence, " + 
									  "Faculty of Applied Physics and Mathematics, Gdansk University of " + 
									  "Technology, Narutowicza 11/12, 80-233 Gdansk, Poland";			
		List<Affiliation> res = engine.processAffiliation(affiliationSequence1);	
		assertEquals(1, res.size());
		if (res.size() > 0) {
			assertNotNull(res.get(0).getInstitutions());
			assertEquals(1, res.get(0).getInstitutions().size());
			assertEquals(res.get(0).getInstitutions().get(0), "Gdansk University of Technology");
			assertEquals(res.get(0).getCountry(), "Poland");
			assertEquals(res.get(0).getAddrLine(), "Narutowicza 11/12");
		}
	}
	
	@Test
	public void testParser2() throws Exception {
		String affiliationSequence2 = "Faculty of Health, School of Biomedical Sciences, " + 
				"University of Newcastle, New South Wales, Australia.";
			List<Affiliation> res = engine.processAffiliation(affiliationSequence2);	
			if (res.size() > 0) {
				assertNotNull(res.get(0).getInstitutions());
			}
	}

	private void After(List<Affiliation> expectedAffiliations, List<Affiliation> resultAffiliations) {
		assertEquals(expectedAffiliations.size(), resultAffiliations.size());

		for (int i = 0; i < resultAffiliations.size(); i++) {
			Affiliation resultAffiliation = resultAffiliations.get(i);
			Affiliation expectedAffiliation = expectedAffiliations.get(i);
			assertEquals(resultAffiliation.getText().toLowerCase(), expectedAffiliation.getText().toLowerCase());
		}
	}

	@Test
	public void testWhenTwoConsecutiveIAddressTagPresentAfterIAffilaitionTag() throws Exception {
		String result = "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tLINESTART\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Taiwan\ttaiwan\tT\tTa\tTai\tTaiw\tn\tan\twan\tiwan\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"No\tno\tN\tNo\tNo\tNo\to\tNo\tNo\tNo\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXx\tI-<address>\tI-<addrLine>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<addrLine>\n" +
				"168\t168\t1\t16\t168\t168\t8\t68\t168\t168\tLINEEND\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tddd\t<address>\t<addrLine>";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "1", ".", " ", "Department", " ", "of"," ", "Surgery",
				","," ", "Taiwan", ".", " ", "\n","\n", "No", ".", " ", "168");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Q1departmentofsurgery,taiwan.\\E");

		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\Qno.168\\E");

		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}

	@Test
	public void testWhenTwoIAddressTagPresentButNotConcecutive() throws Exception {
		String result = "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tLINESTART\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Taiwan\ttaiwan\tT\tTa\tTai\tTaiw\tn\tan\twan\tiwan\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"2\t2\t2\t2\t2\t2\t2\t2\t2\t2\tLINEIN\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<affiliation>\tI-<other>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Al\tal\tA\tAl\tAl\tAl\tl\tAl\tAl\tAl\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXx\t<affiliation>\tI-<institution>\n" +
				"-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<institution>\n" +
				"Adan\tadan\tA\tAd\tAda\tAdan\tn\tan\tdan\tAdan\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"General\tgeneral\tG\tGe\tGen\tGene\tl\tal\tral\teral\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"Hospital\thospital\tH\tHo\tHos\tHosp\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Kuwait\tkuwait\tK\tKu\tKuw\tKuwa\tt\tit\tait\twait\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"No\tno\tN\tNo\tNo\tNo\to\tNo\tNo\tNo\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXx\t<address>\tI-<addrLine>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<addrLine>\n" +
				"168\t168\t1\t16\t168\t168\t8\t68\t168\t168\tLINEEND\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tddd\t<address>";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "1", ".", " ", "Department", " ", "of"," ", "Surgery",
				","," ", "Taiwan", ".", " ", "\n","\n","2", ".", " ", "Department", " ", "of", " ", "Surgery", ",",
				"Al", "-", "Adan", " ", "General", " ", "Hospital", ",", "Kuwait",".", " ", "\n","\n", "No", ".", " ", "168");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Q1departmentofsurgery,taiwan.\\E");

		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\Q2.DepartmentofSurgery,Al-AdanGeneralHospital,Kuwait.no.168\\E");

		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}

	@Test
	public void testWhenOneIAffiliationTagandIAddressTagPresent() throws Exception {
		String result = "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tLINESTART\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Taiwan\ttaiwan\tT\tTa\tTai\tTaiw\tn\tan\twan\tiwan\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "1", ".", " ", "Department", " ", "of"," ", "Surgery",
				","," ", "Taiwan", ".");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Q1departmentofsurgery,taiwan.\\E");

		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}

	@Test
	public void testWhenThreeIAddressTagPresentAndNoIAffiliationTag() throws Exception {
		String result = "950\t950\t9\t95\t950\t950\t0\t50\t950\t950\tLINEIN\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tddd\tI-<address>\tI-<addrLine>\n" +
				"New\tnew\tN\tNe\tNew\tNew\tw\tew\tNew\tNew\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxx\t<address>\t<addrLine>\n" +
				"Hampshire\thampshire\tH\tHa\tHam\tHamp\te\tre\tire\thire\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<address>\t<addrLine>\n" +
				"Ave\tave\tA\tAv\tAve\tAve\te\tve\tAve\tAve\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXxx\t<address>\t<addrLine>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<address>\tI-<other>\n" +
				"Chapel\tchapel\tC\tCh\tCha\tChap\tl\tel\tpel\tapel\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\tXxxx\tI-<address>\t<other>\n" +
				"Hill\thill\tH\tHi\tHil\tHill\tl\tll\till\tHill\tLINEEND\tINITCAP\tNODIGIT\t0\t1\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<address>\t<other>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<address>\t<other>\n" +
				"N\tn\tN\tN\tN\tN\tN\tN\tN\tN\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\tX\t<address>\tI-<region>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<region>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<address>\tI-<other>\n" +
				"Abc\tchapel\tC\tCh\tCha\tChap\tl\tel\tpel\tapel\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t1\t0\tNOPUNCT\tXxxx\tI-<address>\t<other>\n" +
				"def\thill\tH\tHi\tHil\tHill\tl\tll\till\tHill\tLINEEND\tINITCAP\tNODIGIT\t0\t1\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<address>\t<other>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<address>\t<other>\n" +
				"ghi\tn\tN\tN\tN\tN\tN\tN\tN\tN\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\tX\t<address>\tI-<region>";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "950", " ", "New", " ", "Hampshire", " ", "Ave",
				",", " ", "\n", "\n", "Chapel", " ", "Hill", ", ", " ", "N", ".", "\n", "\n", "Abc", " ", "def", ", ", "ghi",".");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Q950newHampshireAve,\\E");

		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\QChapelHill,N.,\\E");

		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\QAbcdef,ghi\\E");

		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}

	@Test
	public void testWhenTwoConsecutiveIAffiliationTagPresent() throws Exception {
		String result = "1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tLINESTART\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"2\t2\t2\t2\t2\t2\t2\t2\t2\t2\tLINEIN\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Al\tal\tA\tAl\tAl\tAl\tl\tAl\tAl\tAl\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXx\t<affiliation>\tI-<institution>\n" +
				"-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<institution>\n" +
				"Adan\tadan\tA\tAd\tAda\tAdan\tn\tan\tdan\tAdan\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"General\tgeneral\tG\tGe\tGen\tGene\tl\tal\tral\teral\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"Hospital\thospital\tH\tHo\tHos\tHosp\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "1", " ", "Department", " ", "of"," ", "Surgery",
				","," ", "\n","\n","2", " ", "Department", " ", "of", " ", "Surgery", ",",
				"Al", "-", "Adan", " ", "General", " ", "Hospital");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Q1departmentofsurgery,\\E");

		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\Q2DepartmentofSurgery,Al-AdanGeneralHospital\\E");

		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}

	@Test
	public void testWhenIAddressTagIAffiliationTagThenIAddressTagIAffiliationTagPresent() throws Exception {
		String result = "Taiwan\ttaiwan\tT\tTa\tTai\tTaiw\tn\tan\twan\tiwan\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"1\t1\t1\t1\t1\t1\t1\t1\t1\t1\tLINESTART\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Kuwait\tkuwait\tK\tKu\tKuw\tKuwa\tt\tit\tait\twait\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"No\tno\tN\tNo\tNo\tNo\to\tNo\tNo\tNo\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXx\t<address>\tI-<addrLine>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<addrLine>\n" +
				"168\t168\t1\t16\t168\t168\t8\t68\t168\t168\tLINEEND\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tddd\t<address>\n" +
				"2\t2\t2\t2\t2\t2\t2\t2\t2\t2\tLINEIN\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<affiliation>\tI-<other>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Al\tal\tA\tAl\tAl\tAl\tl\tAl\tAl\tAl\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXx\t<affiliation>\tI-<institution>\n" +
				"-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<institution>\n" +
				"Adan\tadan\tA\tAd\tAda\tAdan\tn\tan\tdan\tAdan\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"General\tgeneral\tG\tGe\tGen\tGene\tl\tal\tral\teral\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"Hospital\thospital\tH\tHo\tHos\tHosp\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "Taiwan", ".", " ", "1", " ", "Department", " ", "of", " ", "Surgery",
				",", " ", "\n", "\n", "Kuwait", ".", " ", "No", ".", " ", "168", " ", "2", ".", " ", "Department", " ", "of", " ", "Surgery", ",",
				"Al", "-", "Adan", " ", "General", " ", "Hospital", ",", "\n", "\n");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Qtaiwan.\\E");
		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\Q1departmentofsurgery,Kuwait.no.168\\E");
		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\Q2.DepartmentofSurgery,Al-AdanGeneralHospital,\\E");
		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}

	@Test
	public void testWhenIAddressTagIAddressTagThenIAffiliationTagPresent() throws Exception {
		String result = "Taiwan\ttaiwan\tT\tTa\tTai\tTaiw\tn\tan\twan\tiwan\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t0\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"Kuwait\tkuwait\tK\tKu\tKuw\tKuwa\tt\tit\tait\twait\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t0\t0\t1\t1\tNOPUNCT\tXxxx\tI-<address>\tI-<country>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<country>\n" +
				"No\tno\tN\tNo\tNo\tNo\to\tNo\tNo\tNo\tLINEEND\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXx\t<address>\tI-<addrLine>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEEND\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<address>\t<addrLine>\n" +
				"168\t168\t1\t16\t168\t168\t8\t68\t168\t168\tLINEEND\tNOCAPS\tALLDIGIT\t0\t0\t0\t0\t0\t0\tNOPUNCT\tddd\t<address>\n" +
				"2\t2\t2\t2\t2\t2\t2\t2\t2\t2\tLINEIN\tNOCAPS\tALLDIGIT\t1\t0\t0\t0\t0\t0\tNOPUNCT\td\tI-<affiliation>\tI-<marker>\n" +
				".\t.\t.\t.\t.\t.\t.\t.\t.\t.\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tDOT\t.\t<affiliation>\tI-<other>\n" +
				"Department\tdepartment\tD\tDe\tDep\tDepa\tt\tnt\tent\tment\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\tI-<department>\n" +
				"of\tof\to\tof\tof\tof\tf\tof\tof\tof\tLINEIN\tNOCAPS\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\txx\t<affiliation>\t<department>\n" +
				"Surgery\tsurgery\tS\tSu\tSur\tSurg\ty\try\tery\tgery\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<department>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n" +
				"Al\tal\tA\tAl\tAl\tAl\tl\tAl\tAl\tAl\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXx\t<affiliation>\tI-<institution>\n" +
				"-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tHYPHEN\t-\t<affiliation>\t<institution>\n" +
				"Adan\tadan\tA\tAd\tAda\tAdan\tn\tan\tdan\tAdan\tLINEIN\tINITCAP\tNODIGIT\t0\t1\t0\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"General\tgeneral\tG\tGe\tGen\tGene\tl\tal\tral\teral\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				"Hospital\thospital\tH\tHo\tHos\tHosp\tl\tal\ttal\tital\tLINEIN\tINITCAP\tNODIGIT\t0\t0\t1\t0\t0\t0\tNOPUNCT\tXxxx\t<affiliation>\t<institution>\n" +
				",\t,\t,\t,\t,\t,\t,\t,\t,\t,\tLINEIN\tALLCAPS\tNODIGIT\t1\t0\t0\t0\t0\t0\tCOMMA\t,\t<affiliation>\tI-<other>\n";

		List<String> tokenizations = Arrays.asList(" ", "\n", "\n", "Taiwan", ".", " ", "Kuwait", ".", " ", "No", ".", " ", "168", " ",
				"\n", "\n", "2", ".", " ", "Department", " ", "of", " ", "Surgery", ",", "Al", "-", "Adan", " ", "General", " ", "Hospital", ",", "\n", "\n");

		List<Affiliation> expectedAffiliations = new ArrayList<>();

		Affiliation affiliation = new Affiliation();
		affiliation.setText("\\Qtaiwan.\\E");
		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\QKuwait.no.168\\E");
		expectedAffiliations.add(affiliation);

		affiliation = new Affiliation();
		affiliation.setText("\\Q2.DepartmentofSurgery,Al-AdanGeneralHospital,\\E");
		expectedAffiliations.add(affiliation);

		List<Affiliation> resultAffiliations = affiliationAddressParser.resultBuilder(result, tokenizations, false);
		After(expectedAffiliations, resultAffiliations);
	}
}