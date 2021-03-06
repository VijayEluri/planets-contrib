package eu.planets_project.services.migration.imagemagick;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.DigitalObjectUtils;
import eu.planets_project.services.utils.test.ServiceCreator;


/**
 * local tests for image magick migrations
 *
 */
public class ImageMagickMigrateTests {
	  
	static Migrate imageMagick;
    /* The location of this service when deployed. */
	
	static String wsdlLocation = "/pserv-pa-imagemagick/ImageMagickMigrate?wsdl";
	
	/**
	 * test output
	 */
	public static String TEST_OUT = "ImageMagickTestOutput";
	
	static String[] compressionTypes = new String[11];
	
    /**
     * test setup
     */
    @BeforeClass
    public static void setup() {
    	
    	// Config the logger:
        Logger.getLogger("").setLevel( Level.FINE );
    	
//    	System.setProperty("pserv.test.context", "server");
//        System.setProperty("pserv.test.host", "localhost");
//        System.setProperty("pserv.test.port", "8080");
        
        // This method handles the local/standalone/server test context setup:
        imageMagick = ServiceCreator.createTestService(Migrate.QNAME, ImageMagickMigrate.class, wsdlLocation);
        
        compressionTypes[0] = "Undefined Compression";
        compressionTypes[1] = "No Compression";
        compressionTypes[2] = "BZip Compression";
        compressionTypes[3] = "Fax Compression";
        compressionTypes[4] = "Group4 Compression";
        compressionTypes[5] = "JPEG Compression";
        compressionTypes[6] = "JPEG2000 Compression";
        compressionTypes[7] = "LosslessJPEG Compression";
        compressionTypes[8] = "LZW Compression";
        compressionTypes[9] = "RLE Compression";
        compressionTypes[10] = "Zip Compression";
        
        
    }
    
    /**
     * Test the Description method.
     */
    @Test
    public void testDescribe() {
    	System.out.println("running Service at: " + Migrate.QNAME);
        ServiceDescription desc = imageMagick.describe();
        assertTrue("The ServiceDescription should not be NULL.", desc != null );
        System.out.println("Recieved service description: " + desc.toXmlFormatted());
    }
    
    /**
     * test jpg compression
     * @throws IOException
     */
    @Test
    public void testJpgCompression () throws IOException {
    	String inputFormatExt = "TIFF";
        String outputFormatExt = "JPEG";
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_NO, ImageMagickMigrateTestHelper.COMP_QUAL_100));
    }
    
    /**
     * test png compression
     * @throws IOException
     */
    @Test
    public void testPngCompression () throws IOException {
    	
    	String inputFormatExt = "TIFF";
        String outputFormatExt = "PNG";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_100));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_NO, ImageMagickMigrateTestHelper.COMP_QUAL_100));
    }
    
    /**
     * test jpg to tiff migration
     * @throws IOException
     */
    @Test
    public void testTiffToJP2 () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "jp2";
        
        String javaVersion = System.getProperty("java.version");
        
        if(javaVersion.contains("1.6")) {
        	testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG2000, ImageMagickMigrateTestHelper.COMP_QUAL_25));
            
            testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG2000, ImageMagickMigrateTestHelper.COMP_QUAL_50));
            
            testMigrate(inputFormatExt, outputFormatExt, null);
        }
        if(javaVersion.contains("1.5")) {
        	System.err.println("Skipping JP2 tests due to a known issue with ImageMagick, the Java Hotspot VM 1.5 and the rest of the World.");
        }
    }
    
    /**
     * Test JPEG2 to PNG migration
     * @throws IOException
     */
    @Test
    public void testJP2ToPng () throws IOException {
    	String inputFormatExt = "jp2";
        String outputFormatExt = "png";
        
        String javaVersion = System.getProperty("java.version");
        
        if(javaVersion.contains("1.6")) {
        	testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        	testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_50));
        
        	testMigrate(inputFormatExt, outputFormatExt, null);
        }
        if(javaVersion.contains("1.5")) {
        	System.err.println("Skipping JP2 tests due to a known issue with ImageMagick, the Java HotSpot VM 1.5 and the rest of the World.");
        }
        
    }
    
    /**
     * Test JPEG2 to PNG migration
     * @throws IOException
     */
    @Test
    public void testJpcToPng () throws IOException {
    	String inputFormatExt = "jpc";
        String outputFormatExt = "tiff";
        
        String javaVersion = System.getProperty("java.version");
        
        if(javaVersion.contains("1.6")) {
	        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
	        
	        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_50));
	        
	        testMigrate(inputFormatExt, outputFormatExt, null);
        }
        if(javaVersion.contains("1.5")) {
        	System.err.println("Skipping JP2 tests due to a known issue with ImageMagick, the Java HotSpot VM 1.5 and the rest of the World.");
        }
    }
    
    /**
     * Test for JPEG to TIFF migration
     * @throws IOException
     */
    @Test
    public void testJpgToTiff () throws IOException {
    	String inputFormatExt = "jpeg";
        String outputFormatExt = "tif";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));

        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_50));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test jpg to png migration
     * @throws IOException
     */
   @Test
    public void testJpgToPng () throws IOException {
    	String inputFormatExt = "jpeg";
        String outputFormatExt = "png";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
   /**
    * test jpg to gif migration
    * @throws IOException
    */
    @Test
    public void testJpgToGif () throws IOException {
    	String inputFormatExt = "jpeg";
        String outputFormatExt = "gif";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test png to tiff migration
     * @throws IOException
     */
    @Test
    public void testPngToTiff () throws IOException {
    	String inputFormatExt = "png";
        String outputFormatExt = "tiff";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test pnf to jpg migration
     * @throws IOException
     */
    @Test
    public void testPngToJpg () throws IOException {
    	String inputFormatExt = "png";
        String outputFormatExt = "jpeg";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_50));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test png to gif migration
     * @throws IOException
     */
    @Test
    public void testPngToGif () throws IOException {
    	String inputFormatExt = "png";
        String outputFormatExt = "gif";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test tiff to jpg migration
     * @throws IOException
     */
    @Test
    public void testTiffToJpg () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "jpeg";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }

    /**
     * test tiff to png migration
     * @throws IOException
     */
    @Test
    public void testTiffToPng () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "png";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test tiff to gif migration
     * @throws IOException
     */
    @Test
    public void testTiffToGif () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "gif";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test tiff to tga migration
     * @throws IOException
     */
    @Test
    public void testTiffToTga () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "tga";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_RLE, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_RLE, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test tga to tiff migration
     * @throws IOException
     */
    @Test
    public void testTgaToTiff () throws IOException {
    	String inputFormatExt = "tga";
        String outputFormatExt = "tiff";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_RLE, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_RLE, ImageMagickMigrateTestHelper.COMP_QUAL_75));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
/* **************************************************************************************************    
 * RAW to TIFF and TIFF to RAW doesn't work properly at the moment, so tests are disabled for now... *
 * **************************************************************************************************/
    
//    @Test
//    public void testTiffToRaw () throws IOException {
//    	String inputFormatExt = "tiff";
//        String outputFormatExt = "raw";
//        
//        List<Parameter> parameterList = new ArrayList<Parameter>();
//        parameterList.add(new Parameter("compressionType", Integer.toString(COMPRESSION_TYPE_NO)));
////        parameterList.add(new Parameter("compressionQuality", Integer.toString(COMPRESSION_QUALITY_100)));
//        Parameters parameters = new Parameters();
//        parameters.setParameters(parameterList);
//        testMigrate(inputFormatExt, outputFormatExt, parameters);
//    }
    
//    @Test
//    public void testRawToTiff () throws IOException {
//    	String inputFormatExt = "raw";
//        String outputFormatExt = "tiff";
//        
//        List<Parameter> parameterList = new ArrayList<Parameter>();
//        parameterList.add(new Parameter("compressionType", Integer.toString(COMPRESSION_TYPE_LZW)));
//        parameterList.add(new Parameter("compressionQuality", Integer.toString(COMPRESSION_QUALITY_25)));
//        Parameters parameters = new Parameters();
//        parameters.setParameters(parameterList);
//        testMigrate(inputFormatExt, outputFormatExt, parameters);
//        
//        parameterList = new ArrayList<Parameter>();
//        parameterList.add(new Parameter("compressionType", Integer.toString(COMPRESSION_TYPE_LZW)));
//        parameterList.add(new Parameter("compressionQuality", Integer.toString(COMPRESSION_QUALITY_100)));
//        parameters = new Parameters();
//        parameters.setParameters(parameterList);
//        testMigrate(inputFormatExt, outputFormatExt, parameters);
//    }
    
    /**
     * test tiff to pcx migration
     * @throws IOException
     */
    @Test
    public void testTiffToPcx () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "pcx";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_RLE, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_RLE, ImageMagickMigrateTestHelper.COMP_QUAL_50));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    /**
     * test pcx to tiff migration
     * @throws IOException
     */
    @Test
    public void testPcxToTiff () throws IOException {
    	String inputFormatExt = "pcx";
        String outputFormatExt = "tiff";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_LZW, ImageMagickMigrateTestHelper.COMP_QUAL_50));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    

//    @Test
//    public void testPdfToTiff () throws IOException {
//    	String inputFormatExt = "pdf";
//        String outputFormatExt = "tiff";
//        
//        List<Parameter> parameterList = new ArrayList<Parameter>();
//        parameterList.add(new Parameter("compressionType", Integer.toString(COMPRESSION_TYPE_LZW)));
//        parameterList.add(new Parameter("compressionQuality", Integer.toString(COMPRESSION_QUALITY_25)));
//        Parameters parameters = new Parameters();
//        parameters.setParameters(parameterList);
//        testMigrate(inputFormatExt, outputFormatExt, parameters);
//        
//        parameterList = new ArrayList<Parameter>();
//        parameterList.add(new Parameter("compressionType", Integer.toString(COMPRESSION_TYPE_LZW)));
//        parameterList.add(new Parameter("compressionQuality", Integer.toString(COMPRESSION_QUALITY_100)));
//        parameters = new Parameters();
//        parameters.setParameters(parameterList);
//        testMigrate(inputFormatExt, outputFormatExt, parameters);
//    }
    
    /**
     * test tiff to pdf migration
     * @throws IOException
     */
    @Test
    public void testTiffToPdf () throws IOException {
    	String inputFormatExt = "tiff";
        String outputFormatExt = "pdf";
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_25));
        
        testMigrate(inputFormatExt, outputFormatExt, createParameters(ImageMagickMigrateTestHelper.COMP_TYPE_JPEG, ImageMagickMigrateTestHelper.COMP_QUAL_50));
        
        testMigrate(inputFormatExt, outputFormatExt, null);
    }
    
    private List<Parameter> createParameters(String compressionType, String compressionQuality) {
    	if(compressionType==null || compressionQuality==null) {
    		return null;
    	}
    	
    	List<Parameter> parameterList = new ArrayList<Parameter>();
    	
    	if((compressionType!=null) && (compressionQuality!=null)) {
            parameterList.add(new Parameter("compressionType", compressionType));
            parameterList.add(new Parameter("compressionQuality", compressionQuality));
          
    	}
    	
		return parameterList;
    }

    private File getTestFile(String srcExtension) {
    	File testFile = null;
    	
    	if (srcExtension.equalsIgnoreCase("BMP")) {
    		testFile = new File(ImageMagickMigrateTestHelper.BMP_TEST_FILE);
    		return testFile;
		}
    	
    	if (srcExtension.equalsIgnoreCase("GIF")) {
    		testFile = new File(ImageMagickMigrateTestHelper.GIF_TEST_FILE);
    		return testFile;
		}
    	
    	if(srcExtension.equalsIgnoreCase("JPG") || srcExtension.equalsIgnoreCase("JPEG")) {
    		testFile = new File(ImageMagickMigrateTestHelper.JPG_TEST_FILE);
    		return testFile;
    	}
    	
    	if(srcExtension.equalsIgnoreCase("JP2") || srcExtension.equalsIgnoreCase("J2K")) {
    		testFile = new File(ImageMagickMigrateTestHelper.JP2_TEST_FILE);
    		return testFile;
    	}
    	
    	if(srcExtension.equalsIgnoreCase("JPC")) {
    		testFile = new File(ImageMagickMigrateTestHelper.JPC_TEST_FILE);
    		return testFile;
    	}
    	
    	if (srcExtension.equalsIgnoreCase("PCX")) {
    		testFile = new File(ImageMagickMigrateTestHelper.PCX_TEST_FILE);
    		return testFile;
		}
    	
    	if (srcExtension.equalsIgnoreCase("PDF")) {
    		testFile = new File(ImageMagickMigrateTestHelper.PDF_TEST_FILE);
    		return testFile;
		}
    	
    	if (srcExtension.equalsIgnoreCase("PNG")) {
    		testFile = new File(ImageMagickMigrateTestHelper.PNG_TEST_FILE);
    		return testFile;
		}
    	
    	if (srcExtension.equalsIgnoreCase("RAW")) {
    		testFile = new File(ImageMagickMigrateTestHelper.RAW_TEST_FILE);
    		return testFile;
		}
    	
    	if (srcExtension.equalsIgnoreCase("TGA")) {
    		testFile = new File(ImageMagickMigrateTestHelper.TGA_TEST_FILE);
    		return testFile;
		}
    	
    	if (srcExtension.equalsIgnoreCase("TIF") || srcExtension.equalsIgnoreCase("TIFF")) {
    		testFile = new File(ImageMagickMigrateTestHelper.TIFF_TEST_FILE);
    		return testFile;
		}
    	return null;
    }

    /**
     * Test the pass-thru migration.
     * @param srcExtension 
     * @param targetExtension 
     * @param parameters 
     * @throws IOException 
     */
    public void testMigrate(String srcExtension, String targetExtension, List<Parameter> parameters) throws IOException {
        /*
         * To test usability of the digital object instance in web services,
         * we simply pass one into the service and expect one back:
         */

        File inputFile = getTestFile(srcExtension);

        FormatRegistry format = FormatRegistryFactory.getFormatRegistry();
        DigitalObject input = new  DigitalObject.Builder(Content.byReference(inputFile))
        	.format(format.createExtensionUri(srcExtension))
        	.title("test title with lots of spaces in it")
        	.build();
        System.out.println("Input: " + input);

        MigrateResult mr = imageMagick.migrate(input, format
                .createExtensionUri(srcExtension), format
                .createExtensionUri(targetExtension), parameters);

        ServiceReport sr = mr.getReport();
        System.out.println("Got Report: "+sr);

        DigitalObject doOut = mr.getDigitalObject();

        assertTrue("Resulting digital object is null.", doOut != null);

        System.out.println("DigitalObject.getTitle(): " + doOut.getTitle());
        System.out.println("DigitalObject.getFormat(): " + doOut.getFormat().toASCIIString());
        System.out.println("Events: ");

        File outFile = DigitalObjectUtils.toFile(doOut);

        System.out.println("Please find the result file here: " + outFile.getAbsolutePath() + "\n\n");
        assertTrue("Result file created?", outFile.canRead());
            
    }

}

