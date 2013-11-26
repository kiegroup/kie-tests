package org.kie.tests.wb.eap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.kie.tests.wb.base.deploy.TestKjarDeploymentLoader;
import org.kie.tests.wb.base.test.objects.MyType;

public class TestList extends Assert {

    @Test
    public void wtfMarco() throws Exception {
        List<String> list = new ArrayList<String>();
        boolean foundFiles = false;

        CodeSource src = MyType.class.getProtectionDomain().getCodeSource();
        assertNotNull(src);
        URL jarUrl = src.getLocation();
        assertNotNull(jarUrl);
        ZipInputStream zip = new ZipInputStream(jarUrl.openStream());
        if (zip.getNextEntry() != null) {
            ZipFile jarFile = new ZipFile(new File(jarUrl.toURI()));
            while (true) {
                ZipEntry e = zip.getNextEntry();
                if (e == null)
                    break;
                String name = e.getName();
                if (name.startsWith("repo/test/") && name.length() > 10) {
                    foundFiles = true;
                    InputStream in = jarFile.getInputStream(e);
                    String bpmn = convertFileToString(in);
                    assertTrue(bpmn.length() > 100);
                    list.add(bpmn);
                }
            }
        }
        if (!foundFiles) {
            URL url = this.getClass().getResource("/repo/test");
            assertNotNull(url);
            File folder = new File(url.toURI());
            for (final File fileEntry : folder.listFiles()) {
                foundFiles = true;
                String path = fileEntry.getPath();
                InputStream in = new FileInputStream(fileEntry);
                assertNotNull(path, in);
                String bpmn = convertFileToString(in);
                assertTrue(bpmn.length() > 100);
                list.add(bpmn);
            }
        }
        assertEquals(6, list.size());
        System.out.println( list.get(0) );
        assertTrue(foundFiles);
    }

    private static String convertFileToString(InputStream in) {
        InputStreamReader input = new InputStreamReader(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter output = new OutputStreamWriter(baos);
        char[] buffer = new char[4096];
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toString();
    }
}
