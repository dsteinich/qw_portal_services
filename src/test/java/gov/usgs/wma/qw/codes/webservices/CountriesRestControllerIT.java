package gov.usgs.wma.qw.codes.webservices;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.github.springtestdbunit.annotation.DatabaseSetup;

@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
@DatabaseSetup("classpath:/testData/countryCode.xml")
public class CountriesRestControllerIT extends BaseCodesRestControllerTest {

	public static String TEST_ENDPOINT = "/countrycode";
	public static String CODE_VALUE = "US";
	public static String CODE_JSON = "{\"value\":\"US\",\"desc\":\"UNITED STATES OF AMERICA\",\"providers\":\"NWIS STEWARDS STORET\"}";
	public static String CODE_XML = XML_HEADER +"<Code value=\"US\" desc=\"UNITED STATES OF AMERICA\" providers=\"NWIS STEWARDS STORET\"/>";
	public static String SEARCH_TEXT = "ca";
	public static String SEARCH_JSON = "{\"codes\":[{\"value\":\"CV\",\"desc\":\"CAPE VERDE\",\"providers\":\"NWIS\"}],\"recordCount\":3}";
	public static String SEARCH_XML = XML_HEADER + "<Codes><Code value=\"CV\" desc=\"CAPE VERDE\" providers=\"NWIS\"/><recordCount>3</recordCount></Codes>"; 
	public static String COMPARE_FILE_JSON = "countryCode.json";
	public static String COMPARE_FILE_XML = "countryCode.xml";

	@Test
	public void getListAsJsonTest() throws Exception {
		runGetListAsJsonTest(TEST_ENDPOINT, SEARCH_TEXT, COMPARE_FILE_JSON, SEARCH_JSON);
	}

	@Test
	public void getListAsXmlTest() throws Exception {
		runGetListAsXmlTest(TEST_ENDPOINT, SEARCH_TEXT, COMPARE_FILE_XML, SEARCH_XML);
	}

	@Test
	public void getCodeAsJsonTest() throws Exception {
		runGetCodeAsJson(TEST_ENDPOINT, CODE_VALUE, CODE_JSON);
	}

	@Test
	public void getCodeAsXmlTest() throws Exception {
		runGetCodeAsXml(TEST_ENDPOINT, CODE_VALUE, CODE_XML);
	}

}
