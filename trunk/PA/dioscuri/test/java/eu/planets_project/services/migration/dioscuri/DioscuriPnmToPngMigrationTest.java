/**
 * 
 */
package eu.planets_project.services.migration.dioscuri;


import java.io.File;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.planets_project.ifr.core.techreg.api.formats.Format;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.ImmutableContent;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.test.ServiceCreator;
import static org.junit.Assert.assertTrue;

/**
 * @author melmsp
 *
 */
public class DioscuriPnmToPngMigrationTest {
	
	public static String wsdlLoc = "/pserv-pa-dioscuri-pictview-migrate/DioscuriPnmToPngMigration?wsdl"; 
	
	public static Migrate DIOSCURI_MIGRATE = null;
	
	public static File DIOSCURI_TEST_OUT = FileUtils.createWorkFolderInSysTemp("DIOSCURI_TEST_OUT");
	
	public static File TEST_FILE = new File("tests/test-files/images/bitmap/test_pnm/BASI0G02.PNM"); 

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
		DigitalObject inputDigOb = new DigitalObject.Builder(ImmutableContent.asStream(TEST_FILE)).title(TEST_FILE.getName()).format(Format.extensionToURI(FileUtils.getExtensionFromFile(TEST_FILE))).build();
		MigrateResult result = DIOSCURI_MIGRATE.migrate(inputDigOb, Format.extensionToURI(FileUtils.getExtensionFromFile(TEST_FILE)), Format.extensionToURI("PNG"), null);
		
		assertTrue("MigrateResult should not be NULL", result!=null);
		assertTrue("ServiceReport should be SUCCESS", result.getReport().getErrorState()==ServiceReport.SUCCESS);
		
		System.out.println(result.getReport());
		
		File resultFile = new File(DIOSCURI_TEST_OUT, result.getDigitalObject().getTitle());
		FileUtils.writeInputStreamToFile(result.getDigitalObject().getContent().read(), resultFile);
		
		System.out.println("Please find the converted file here: " + resultFile.getAbsolutePath());
	}

}
