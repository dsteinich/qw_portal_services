package gov.usgs.wma.qw.codes.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import gov.usgs.wma.qw.LastUpdateDao;
import gov.usgs.wma.qw.codes.Code;
import gov.usgs.wma.qw.codes.CodeList;
import gov.usgs.wma.qw.codes.CodeType;
import gov.usgs.wma.qw.codes.dao.CodeDao;
import gov.usgs.wma.qw.codes.webservices.CodesRestController;

@SpringBootTest
public class CodesRestControllerTest {

	private TestController testController;
	private MockHttpServletRequest mockRequest;
	private NativeWebRequest webRequest; 
	private MockHttpServletResponse servletResponse;
	private LocalDateTime localFromUTC; 

	@MockBean
	private LastUpdateDao lastUpdateDao;
	@MockBean
	private CodeDao codeDao;

	private class TestController extends CodesRestController {
		public TestController(final LastUpdateDao lastUpdateDao,
				final CodeDao codeDao) {
			this.lastUpdateDao = lastUpdateDao;
			this.codeDao = codeDao;
		}
	}

	@BeforeEach
	public void setup() {
		testController = new TestController(lastUpdateDao, codeDao);
		localFromUTC = LocalDateTime.of(2014, 1, 1, 1, 1, 1);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getListTest() {
		when(lastUpdateDao.getLastEtl()).thenReturn(localFromUTC);
		when(codeDao.getRecordCount(eq(CodeType.COUNTRYCODE), anyMap())).thenReturn(12);
		when(codeDao.getCodes(eq(CodeType.COUNTRYCODE), anyMap())).thenAnswer(new Answer<List<Code>>() {
			@Override
			public List<Code> answer(InvocationOnMock invocation) throws Throwable {
				//This will place the values sent to the dao in the properties of the code...
				Code code = new Code();
				Object[] arguments = invocation.getArguments();
				if (arguments != null && arguments.length > 1 && arguments[1] != null) {
					Map<String, Object> parms = (Map<String, Object>) arguments[1];
					code.setValue((String) parms.get("text"));
					code.setDesc(String.valueOf((Integer) parms.get("offset")));
					code.setProviders(String.valueOf((Integer) parms.get("fetchSize")));
					return Arrays.asList(code);
				}
				return null;
			}
		});

		//Make sure "not modified" wired in
		mockRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(mockRequest, servletResponse);
		mockRequest.addHeader("If-Modified-Since", new Date());
		assertNull(testController.getList(CodeType.COUNTRYCODE, "US", "0", "5", null, webRequest),
				"Header set for after last modified, so is not modified.");

		//Otherwise get the codes
		//Happy path!
		mockRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(mockRequest, servletResponse);
		CodeList codeList = testController.getList(CodeType.COUNTRYCODE, "US", "1", "5", null, webRequest);
		assertEquals(1, codeList.getCodes().size());
		assertEquals(12, codeList.getRecordCount());
		Code code = (Code) codeList.getCodes().toArray()[0];
		assertEquals("US", code.getValue());
		assertEquals("0", code.getDesc());
		assertEquals("5", code.getProviders());

		//Lotsa nulls
		codeList = testController.getList(CodeType.COUNTRYCODE, null, null, null, null, webRequest);
		assertEquals(1, codeList.getCodes().size());
		code = (Code) codeList.getCodes().toArray()[0];
		assertNull(code.getValue());
		assertEquals("null", code.getDesc());
		assertEquals("null", code.getProviders());

		//Lotsa empties
		codeList = testController.getList(CodeType.COUNTRYCODE, "", "", "", new HashMap<String, Object>(), webRequest);
		assertEquals(1, codeList.getCodes().size());
		code = (Code) codeList.getCodes().toArray()[0];
		assertNull(code.getValue());
		assertEquals("null", code.getDesc());
		assertEquals("null", code.getProviders());

		//funky values
		codeList = testController.getList(CodeType.COUNTRYCODE,  "xx x", "y4", "z2", null, webRequest);
		assertEquals(1, codeList.getCodes().size());
		code = (Code) codeList.getCodes().toArray()[0];
		assertEquals("xx x", code.getValue());
		assertEquals("null", code.getDesc());
		assertEquals("null", code.getProviders());

		//more funky values
		codeList = testController.getList(CodeType.COUNTRYCODE,  "xx x", "-4", "-2", null, webRequest);
		assertEquals(1, codeList.getCodes().size());
		code = (Code) codeList.getCodes().toArray()[0];
		assertEquals("xx x", code.getValue());
		assertEquals("null", code.getDesc());
		assertEquals("null", code.getProviders());

		//even more funky values
		codeList = testController.getList(CodeType.COUNTRYCODE,  "xx x", "X", "2", null, webRequest);
		assertEquals(1, codeList.getCodes().size());
		code = (Code) codeList.getCodes().toArray()[0];
		assertEquals("xx x", code.getValue());
		assertEquals("null", code.getDesc());
		assertEquals("2", code.getProviders());

		//later pages
		codeList = testController.getList(CodeType.COUNTRYCODE,  "xx x", "4", "15", null, webRequest);
		assertEquals(1, codeList.getCodes().size());
		code = (Code) codeList.getCodes().toArray()[0];
		assertEquals("xx x", code.getValue());
		assertEquals("45", code.getDesc());
		assertEquals("15", code.getProviders());

	}

	@Test
	public void getCodeTest() {
		when(lastUpdateDao.getLastEtl()).thenReturn(localFromUTC);
		Code us = new Code();
		us.setValue("US");
		us.setDesc("UNITED STATES OF AMERICA");
		us.setProviders("NWIS STEWARDS STORET");
		when(codeDao.getCode(CodeType.COUNTRYCODE, "US")).thenReturn(us);

		//Make sure "not modified" wired in
		mockRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(mockRequest, servletResponse);
		mockRequest.addHeader("If-Modified-Since", new Date());
		assertNull(testController.getCode(CodeType.COUNTRYCODE, "US", webRequest, servletResponse),
				"Header set for after last modified, so is not modified.");

		//Otherwise get the code
		mockRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(mockRequest, servletResponse);
		assertEquals(us, testController.getCode(CodeType.COUNTRYCODE, "US", webRequest, servletResponse));

		//or return a 404 if not found
		mockRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(mockRequest, servletResponse);
		assertNull(testController.getCode(CodeType.COUNTRYCODE, "XX", webRequest, servletResponse));
		assertEquals(404, servletResponse.getStatus());
	}

	@Test
	public void isIntegerTest() {
		//NPE avoidance
		assertFalse(CodesRestController.isInteger(null));

		//Handles Strings
		assertFalse(CodesRestController.isInteger("a b c2"));

		//Handles decimals
		assertFalse(CodesRestController.isInteger("12.34"));

		//Gets it
		assertTrue(CodesRestController.isInteger("1234"));
	}

}
