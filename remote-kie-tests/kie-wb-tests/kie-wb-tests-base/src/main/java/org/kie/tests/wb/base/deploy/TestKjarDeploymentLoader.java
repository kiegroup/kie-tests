package org.kie.tests.wb.base.deploy;

import static org.junit.Assert.*;
import static org.kie.scanner.MavenRepository.getMavenRepository;
import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieRepositoryImpl;
import org.drools.compiler.kie.builder.impl.KieServicesImpl;
import org.jbpm.kie.services.api.DeployedUnit;
import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit.RuntimeStrategy;
import org.jbpm.kie.services.api.Kjar;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.scanner.MavenRepository;
import org.kie.tests.wb.base.test.MyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class TestKjarDeploymentLoader {

    private static final Logger logger = LoggerFactory.getLogger(TestKjarDeploymentLoader.class);

    @Inject
    @Kjar
    private DeploymentService deploymentService;

    @PostConstruct
    public void init() throws Exception {
        KModuleDeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION, KBASE_NAME, KSESSION_NAME);
        deploymentUnit.setStrategy(RuntimeStrategy.SINGLETON);

        DeployedUnit alreadyDeployedUnit = deploymentService.getDeployedUnit(deploymentUnit.getIdentifier());
        if (alreadyDeployedUnit == null) {
            deploymentService.deploy(deploymentUnit);
        }
        logger.info("Deployed [" + deploymentUnit.getIdentifier() + "]");
    }

    private static class BpmnResource {
        public String name;
        public String content;

        public BpmnResource(String name, String content) {
            this.content = content;
            this.name = name;
        }
    }

    public static void deployKjarToMaven() {
        try {
            deployKjarToMaven(GROUP_ID, ARTIFACT_ID, VERSION, KBASE_NAME, KSESSION_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to deploy kjar to maven: " + e.getMessage());
        }
    }

    private static void deployKjarToMaven(String group, String artifact, String version, String kbaseName, String ksessionName) {
        List<BpmnResource> bpmnResources;

        try {
            bpmnResources = loadBpmnResources();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load BPMN resources: " + e.getMessage(), e);
        }

        final KieServices ks = new KieServicesImpl() {
            public KieRepository getRepository() {
                // override repository to not store the artifact on deploy to trigger load from maven repo
                return new KieRepositoryImpl();
            }
        };
        ReleaseId releaseId = ks.newReleaseId(group, artifact, version);
        InternalKieModule kjar = createKieJar(ks, releaseId, bpmnResources, kbaseName, ksessionName);

        String pomText = getPom(releaseId);
        String pomFileName = MavenRepository.toFileName(releaseId, null) + ".pom";
        File pomFile = new File(System.getProperty("java.io.tmpdir"), pomFileName);
        try {
            FileOutputStream fos = new FileOutputStream(pomFile);
            fos.write(pomText.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MavenRepository repository = getMavenRepository();
        repository.deployArtifact(releaseId, kjar, pomFile);
    }

    /**
     * Create a list of {@link BpmnResource} instances with the process files to be included in the KJar.
     * 
     * @return The list of {@link BpmnResource} instances.
     * @throws Exception
     */
    private static List<BpmnResource> loadBpmnResources() throws Exception {
        List<BpmnResource> list = new ArrayList<BpmnResource>();
        boolean foundFiles = false;

        CodeSource src = MyType.class.getProtectionDomain().getCodeSource();
        URL jarUrl = src.getLocation();
        ZipInputStream zip = new ZipInputStream(jarUrl.openStream());
        if (zip.getNextEntry() != null) {
            ZipFile jarFile = new ZipFile(new File(jarUrl.toURI()));
            while (true) {
                ZipEntry e = zip.getNextEntry();
                if (e == null)
                    break;
                String name = e.getName();
                if (name.startsWith("repo/test/") && name.length() > 10) {
                    String shortName = name.replace("repo/test/", "");
                    foundFiles = true;
                    InputStream in = jarFile.getInputStream(e);
                    String content = convertFileToString(in);
                    assertTrue(content.length() > 100);
                    list.add(new BpmnResource(shortName, content));
                }
            }
        }
        if (!foundFiles) {
            URL url = TestKjarDeploymentLoader.class.getResource("/repo/test");
            File folder = new File(url.toURI());
            for (final File fileEntry : folder.listFiles()) {
                foundFiles = true;
                InputStream in = new FileInputStream(fileEntry);
                String content = convertFileToString(in);
                assertTrue(content.length() > 100);
                list.add(new BpmnResource(fileEntry.getName(), content));
            }
        }
        return list;
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

    /**
     * Create a KJar for deployment in the arquillian tests.
     * 
     * @param ks {@link KieServices} instance in order to build the {@link KieModule}
     * @param releaseId Release (deployment) id.
     * @param bpmns List of process (.bpmn) files and info
     * @param kbaseName The name of the {@link KieBase}
     * @param ksessionName The name of the {@link KieSession}.
     * 
     * @return The {@link InternalKieModule} which represents the KJar.
     */
    private static InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, List<BpmnResource> bpmns, String kbaseName,
            String ksessionName) {
        KieFileSystem kfs = createKieFileSystemWithKProject(ks, kbaseName, ksessionName);
        kfs.writePomXML(getPom(releaseId));

        for (BpmnResource bpmn : bpmns) {
            kfs.write("src/main/resources/" + kbaseName + "/" + bpmn.name, bpmn.content);
        }

        addClass(MyType.class, kfs);

        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        assertTrue(kieBuilder.buildAll().getResults().getMessages().isEmpty());
        return (InternalKieModule) kieBuilder.getKieModule();
    }

    /**
     * Add class to the {@link KieFileSystem}.
     * 
     * @param userClass The class to be added.
     * @param kfs The {@link KieFileSystem}
     */
    private static void addClass(Class<?> userClass, KieFileSystem kfs) {
        String pkgFolder = userClass.getPackage().toString();
        pkgFolder = pkgFolder.replace("package ", "");
        pkgFolder = pkgFolder.replaceAll("\\.", File.separator);
        String classSimpleName = userClass.getSimpleName();
        String classFilePath = pkgFolder + "/" + classSimpleName + ".class";

        URL classFileUrl = userClass.getResource(classSimpleName + ".class");
        assertNotNull("Unable to get path for class " + classSimpleName, classFileUrl);

        System.out.println("PROT: " + classFileUrl.getProtocol());
        System.out.println("PATH: " + classFileUrl.getPath());

        byte[] classByteCode = null;
        if ("file".equalsIgnoreCase(classFileUrl.getProtocol())) {
            File classFile = new File(classFileUrl.getPath());
            assertTrue(classFile != null);
            logger.info(classFile.getAbsolutePath());

            try {
                classByteCode = readStream(new FileInputStream(classFile));
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unable to read in " + classFile.getAbsolutePath());
            }
            assertTrue("no bytes retrieved", classByteCode.length > 0);
        } else if ("jar".equalsIgnoreCase(classFileUrl.getProtocol())) {
            // file:/opt/mavenRepository/org/kie/tests/kie-wb-tests-base/6.0.0-SNAPSHOT/kie-wb-tests-base-6.0.0-SNAPSHOT.jar!/org/kie/tests/wb/base/test/MyType.class
            String path = classFileUrl.getPath();
            int bangIndex = path.indexOf('!');
            String jarPath = path.substring("file:".length(), bangIndex);
            String classPath = path.substring(bangIndex+2); // no base /
            
            try { 
                ZipFile zip = new ZipFile(new File(jarPath));
                ZipEntry entry = zip.getEntry(classPath);
                InputStream zipStream = zip.getInputStream(entry);
                classByteCode = readStream(zipStream);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unable to read in " + jarPath);
            }
        }

        kfs.write(classFilePath, classByteCode);
    }

    private static byte[] readStream(InputStream ios) throws Exception {
        ByteArrayOutputStream baos = null;
        try {
            byte[] buffer = new byte[4096];
            baos = new ByteArrayOutputStream();
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (ios != null) {
                    ios.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        return baos.toByteArray();
    }

    private static KieFileSystem createKieFileSystemWithKProject(KieServices ks, String kbaseName, String ksessionName) {
        KieModuleModel kproj = ks.newKieModuleModel();

        KieBaseModel kieBaseModel = kproj.newKieBaseModel(kbaseName).setDefault(true)
                .setEqualsBehavior(EqualityBehaviorOption.EQUALITY).setEventProcessingMode(EventProcessingOption.STREAM);

        KieSessionModel ksession = kieBaseModel.newKieSessionModel(ksessionName).setDefault(true)
                .setType(KieSessionModel.KieSessionType.STATEFUL).setClockType(ClockTypeOption.get("realtime"));

        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(kproj.toXML());
        return kfs;
    }

    /**
     * Create the pom that will be placed in the KJar.
     * 
     * @param releaseId The release (deployment) id.
     * @param dependencies The list of dependendencies to be added to the pom
     * @return A string representation of the pom.
     */
    private static String getPom(ReleaseId releaseId, ReleaseId... dependencies) {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n" + "\n" + "  <groupId>" + releaseId.getGroupId() + "</groupId>\n"
                + "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" + "  <version>" + releaseId.getVersion()
                + "</version>\n" + "\n";
        if (dependencies != null && dependencies.length > 0) {
            pom += "<dependencies>\n";
            for (ReleaseId dep : dependencies) {
                pom += "<dependency>\n";
                pom += "  <groupId>" + dep.getGroupId() + "</groupId>\n";
                pom += "  <artifactId>" + dep.getArtifactId() + "</artifactId>\n";
                pom += "  <version>" + dep.getVersion() + "</version>\n";
                pom += "</dependency>\n";
            }
            pom += "</dependencies>\n";
        }
        pom += "</project>";
        return pom;
    }
}
