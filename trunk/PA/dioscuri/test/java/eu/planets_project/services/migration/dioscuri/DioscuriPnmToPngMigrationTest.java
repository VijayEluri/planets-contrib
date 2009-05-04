/**
 * 
 */
package eu.planets_project.services.migration.dioscuri;


import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.ImmutableContent;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.test.ServiceCreator;

/**
 * @author melmsp
 *
 */
public class DioscuriPnmToPngMigrationTest {
	
	public static String wsdlLoc = "/pserv-pa-dioscuri-pictview-migrate/DioscuriPnmToPngMigration?wsdl"; 
	
	public static Migrate DIOSCURI_MIGRATE = null;
	
	public static File DIOSCURI_TEST_OUT = FileUtils.createWorkFolderInSysTemp("DIOSCURI_TEST_OUT");
	
	public static File PNM_TEST_FILE = new File("tests/test-files/images/bitmap/test_pnm/BASI0G02.PNM"); 
	public static File PNG_TEST_FILE = null;
	
	public static FormatRegistry format = FormatRegistryFactory.getFormatRegistry();

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		DIOSCURI_MIGRATE = ServiceCreator.createTestService(Migrate.QNAME, DioscuriPnmToPngMigration.class, wsdlLoc);
	}
	
	@Test
	public void testDescribe() {
		ServiceDescription sd = DIOSCURI_MIGRATE.describe();
		assertTrue("The ServiceDescription should not be NULL.", sd != null );
    	System.out.println("test: describe()");
    	System.out.println("--------------------------------------------------------------------");
    	System.out.println();
    	System.out.println("Received ServiceDescription from: " + DIOSCURI_MIGRATE.getClass().getName());
    	System.out.println(sd.toXmlFormatted());
    	System.out.println("--------------------------------------------------------------------");
	}
	
	@Test
	public void testMigrate() {
		DigitalObject inputDigOb = new DigitalObject.Builder(ImmutableContent.asStream(PNM_TEST_FILE)).title(PNM_TEST_FILE.getName()).format(format.createExtensionUri(FileUtils.getExtensionFromFile(PNM_TEST_FILE))).build();
		MigrateResult result = DIOSCURI_MIGRATE.migrate(inputDigOb, format.createExtensionUri(FileUtils.getExtensionFromFile(PNM_TEST_FILE)), format.createExtensionUri("PNG"), null);
		
		assertTrue("MigrateResult should not be NULL", result!=null);
		assertTrue("ServiceReport should be SUCCESS", result.getReport().getErrorState()==ServiceReport.SUCCESS);
		
		System.out.println(result.getReport());
		
		File resultFile = new File(DIOSCURI_TEST_OUT, result.getDigitalObject().getTitle());
		FileUtils.writeInputStreamToFile(result.getDigitalObject().getContent().read(), resultFile);
		
		System.out.println("Please find the converted file here: " + resultFile.getAbsolutePath());
		
		PNG_TEST_FILE = resultFile;
		
		inputDigOb = new DigitalObject.Builder(ImmutableContent.asStream(PNG_TEST_FILE)).title(PNG_TEST_FILE.getName()).format(format.createExtensionUri(FileUtils.getExtensionFromFile(PNG_TEST_FILE))).build();
		result = DIOSCURI_MIGRATE.migrate(inputDigOb, format.createExtensionUri(FileUtils.getExtensionFromFile(PNG_TEST_FILE)), format.createExtensionUri("PNM"), null);
		
		assertTrue("MigrateResult should not be NULL", result!=null);
		assertTrue("ServiceReport should be SUCCESS", result.getReport().getErrorState()==ServiceReport.SUCCESS);
		
		System.out.println(result.getReport());
		
		resultFile = new File(DIOSCURI_TEST_OUT, result.getDigitalObject().getTitle());
		FileUtils.writeInputStreamToFile(result.getDigitalObject().getContent().read(), resultFile);
		
		System.out.println("Please find the converted file here: " + resultFile.getAbsolutePath());
	}

}