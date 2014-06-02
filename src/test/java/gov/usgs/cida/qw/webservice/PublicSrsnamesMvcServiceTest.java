package gov.usgs.cida.qw.webservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import gov.usgs.cida.qw.BaseSpringTest;
import gov.usgs.cida.qw.QWConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
public class PublicSrsnamesMvcServiceTest extends BaseSpringTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void getAsJson() throws Exception {
        mockMvc.perform(get("/publicsrsnames").accept(MediaType.parseMediaType(QWConstants.MIME_TYPE_APPLICATION_JSON)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(QWConstants.MIME_TYPE_APPLICATION_JSON));

        MvcResult rtn = mockMvc.perform(get("/publicsrsnames?mimetype=json").accept(MediaType.parseMediaType(QWConstants.MIME_TYPE_APPLICATION_JSON)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(QWConstants.MIME_TYPE_APPLICATION_JSON))
            .andExpect(content().encoding(QWConstants.DEFAULT_ENCODING))
            .andReturn();
        assertTrue(rtn.getResponse().getContentAsString().contains("maxLastRevDate"));
        assertTrue(rtn.getResponse().getContentAsString().contains("pcodes"));
    }

    @Test
    public void doCsvTest() {
        OutputStream stream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(stream);
        PublicSrsnamesMvcService service = new PublicSrsnamesMvcService();
        LinkedHashMap<String, Object> item1 = new LinkedHashMap<String, Object>();
        item1.put("bb", "222");
        item1.put("aa", "111");
        LinkedHashMap<String, Object> item2 = new LinkedHashMap<String, Object>();
        item2.put("xx", "bbb");
        item2.put("zz", "ccc");
        List<LinkedHashMap<String, Object>> data = new ArrayList<LinkedHashMap<String, Object>>();
        data.add(item1);
        data.add(item2);
        service.doCsv(writer, data);
        writer.close();
        assertEquals("\"bb\",\"aa\"\n\"222\",\"111\"\n\"bbb\",\"ccc\"\n", stream.toString());
    }

    @Test
    public void getAsCsv() throws Exception {
        mockMvc.perform(get("/publicsrsnames").accept(MediaType.parseMediaType(QWConstants.MIME_TYPE_TEXT_CSV)))
            .andExpect(status().isBadRequest());

        MvcResult rtn = mockMvc.perform(get("/publicsrsnames?mimetype=csv").accept(MediaType.parseMediaType(QWConstants.MIME_TYPE_TEXT_CSV)))
            .andExpect(status().isOk())
            .andExpect(content().encoding(QWConstants.DEFAULT_ENCODING))
            .andReturn();
        assertTrue(rtn.getResponse().getHeader(QWConstants.HEADER_CONTENT_DISPOSITION).contains("attachment;filename=\"public_srsnames_"));
        assertTrue(rtn.getResponse().getHeader(QWConstants.HEADER_CONTENT_DISPOSITION).contains(".zip\""));
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(rtn.getResponse().getContentAsByteArray()));
        ZipEntry entry = zip.getNextEntry();
        assertTrue(entry.getName().contains("public_srsnames_"));
        assertTrue(entry.getName().contains(".csv"));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int len;
        byte[] buffer = new byte[1024];
        while ((len = zip.read(buffer)) > 0) {
        os.write(buffer, 0, len);
        }
        assertTrue(os.toString(QWConstants.DEFAULT_ENCODING).contains("\"parm_cd\",\"description\",\"characteristicname\",\"measureunitcode\"," +
                "\"resultsamplefraction\",\"resulttemperaturebasis\",\"resultstatisticalbasis\",\"resulttimebasis\",\"resultweightbasis\"," +
                "\"resultparticlesizebasis\",\"last_rev_dt\""));
    }

}