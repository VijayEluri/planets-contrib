/**
 * 
 */
package eu.planets_project.services.identification.imagemagick;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebService;

import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.datatypes.Tool;
import eu.planets_project.services.identification.imagemagick.utils.ImageMagickHelper;
import eu.planets_project.services.identify.Identify;
import eu.planets_project.services.identify.IdentifyResult;
import eu.planets_project.services.migration.imagemagick.CoreImageMagick;
import eu.planets_project.services.utils.DigitalObjectUtils;
import eu.planets_project.services.utils.ServiceUtils;

/**
 * @author melmsp
 *
 */

@Local(Identify.class)
@Remote(Identify.class)
@Stateless

@WebService(name = ImageMagickIdentify.NAME, 
        serviceName = Identify.NAME, 
        targetNamespace = PlanetsServices.NS,
        endpointInterface = "eu.planets_project.services.identify.Identify" )
        
public class ImageMagickIdentify implements Identify {
	
	public static final String NAME = "ImageMagickIdentify";
	
	public static final long serialVersionUID = -772290809743383420L;
	
	private static Logger log = Logger.getLogger(ImageMagickIdentify.class.getName()) ;
	
	private static final String DEFAULT_EXTENSION = "bin";
	private static final FormatRegistry format = FormatRegistryFactory.getFormatRegistry();
	
	private static final String IMAGE_MAGICK_URI = "http://www.imagemagick.org";
	
	private String version = "unknown";
	
	
	
	/**
	 * Default Constructor, setting the System.property to tell Jboss to use its own Classloader...
	 */
	public ImageMagickIdentify(){
//	    System.setProperty("jmagick.systemclassloader","no"); // Use the JBoss-Classloader, instead of the Systemclassloader.
	    log.info("Hello! Initializing and starting ImageMagickIdentify service!");
	    version = CoreImageMagick.checkImageMagickVersion();
	}

	/* (non-Javadoc)
	 * @see eu.planets_project.services.identify.Identify#describe()
	 */
	public ServiceDescription describe() {
		ServiceDescription.Builder sd = new ServiceDescription.Builder(NAME, Identify.class.getCanonicalName());
        sd.description("A DigitalObject Identification Service based on ImageMagick " + version + ". \n" +
        		"It returns a list of PRONOM IDs, matching for the identified file format!\n" +
        		"Please note: the first URI in the result list is a PLANETS format URI (e.g. \"planets:fmt/ext/tiff\")\n" +
        		"denoting the file format returned by ImageMagick for the file under consideration.\n" +
        		"The following URIs are the matching PRONOM IDs.");
        sd.author("Peter Melms, mailto:peter.melms@uni-koeln.de");
        sd.classname(this.getClass().getCanonicalName());
        sd.tool( Tool.create(null, "ImageMagick", version, null, IMAGE_MAGICK_URI) );
        sd.logo(URI.create("http://www.imagemagick.org/image/logo.jpg"));
        List<URI> formats = ImageMagickHelper.getSupportedInputFormats();
        if( formats != null )
            sd.inputFormats(formats.toArray(new URI[]{}));
        return sd.build();
	}
	
	

	/* (non-Javadoc)
	 * @see eu.planets_project.services.identify.Identify#identify(eu.planets_project.services.datatypes.DigitalObject)
	 */
	public IdentifyResult identify(DigitalObject digitalObject, List<Parameter> parameters ) {
		
		if(digitalObject.getContent()==null) {
			log.severe("The Content of the DigitalObject should NOT be NULL! Returning with ErrorReport");
			return this.returnWithErrorMessage("The Content of the DigitalObject should NOT be NULL! Returning with ErrorReport", null);
		}
		
		String fileName = digitalObject.getTitle();
		log.info("Input file to identify: " +fileName);
		URI inputFormat = digitalObject.getFormat();
        if(inputFormat!=null) {
            log.info("Assumed file format: " + inputFormat.toASCIIString());
        }
		String extension = null;
		
		if(inputFormat!=null) {
			extension = format.getExtensions(inputFormat).iterator().next();
			log.info("Found extension for input file: " + extension);
		}
		else {
			log.info("I am not able to find the file extension, using DEFAULT_EXTENSION instead: " + DEFAULT_EXTENSION);
			extension = DEFAULT_EXTENSION;
		}
		
		File inputFile = DigitalObjectUtils.toFile(digitalObject);
		log.info("Created temporary input file: " + inputFile.getAbsolutePath());
		
		ArrayList<URI> uriList = null;
		
		String srcImageFormat = null;
		
		srcImageFormat = CoreImageMagick.verifyInputFormat(inputFile);
		
		Set<URI> uris = format.getUrisForExtension(srcImageFormat);
	    
	    if(uris==null || uris.size() <= 0) {
	    	log.severe("No URI returned for this extension: " + srcImageFormat + ".\n" 
	    			+ "Input file: " + inputFile.getName() + " could not be identified!!!");
	    	return this.returnWithErrorMessage("No URI returned for this extension: " + srcImageFormat + ".\n" 
	    			+ "Input file: " + inputFile.getName() + " could not be identified!!!", null);
	    }
	    
	    uriList = new ArrayList <URI> (uris);
	    
	    URI formatURI = format.createExtensionUri(srcImageFormat);
	    uriList.add(0, formatURI);
	    String infoString = createFormatInfoString(uris);
	    log.info("Successfully identified Input file as: " + formatURI.toASCIIString() + "\n" + infoString);
	    ServiceReport sr = new ServiceReport(Type.INFO, Status.SUCCESS,
                "Successfully identified Input file as: "
                        + formatURI.toASCIIString() + "\n" + infoString);
		IdentifyResult identRes = new IdentifyResult(uriList, IdentifyResult.Method.PARTIAL_PARSE, sr);
		
		log.info("SUCCESS! Returning IdentifyResult. Goodbye!");
		return identRes;
	}
	
	
	
	private String createFormatInfoString(Set<URI> uris) {
		StringBuffer buf = new StringBuffer();
	    buf.append("Matching PRONOM IDs for this extension type: \n");
	    for (URI uri : uris) {
	    	buf.append(uri.toASCIIString() + " (\"" + format.getExtensions(uri) + "\")\n");
		}
		return buf.toString();
	}
	
	
	
	private IdentifyResult returnWithErrorMessage(String message, Exception e ) {
        if( e == null ) {
            return new IdentifyResult(null, null, ServiceUtils.createErrorReport(message));
        } else {
            return new IdentifyResult(null, null, ServiceUtils.createExceptionErrorReport(message, e));
        }
    }
	

}
