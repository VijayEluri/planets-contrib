/**
 * 
 */
package eu.planets_project.services.migration.soxservices;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;

import com.sun.xml.ws.developer.StreamingAttachment;

import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.MigrationPath;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.datatypes.Tool;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.migration.soxservices.utils.SoXHelper;
import eu.planets_project.services.utils.ProcessRunner;
import eu.planets_project.services.utils.ServiceUtils;

/**
 * @author melmsp
 *
 */


@Stateless
@MTOM
@StreamingAttachment( parseEagerly=true, memoryThreshold=ServiceUtils.JAXWS_SIZE_THRESHOLD )
@WebService(
        name = SoX.NAME, 
        serviceName = Migrate.NAME,
        targetNamespace = PlanetsServices.NS,
        endpointInterface = "eu.planets_project.services.migrate.Migrate")
public class SoX implements Migrate, Serializable {
	
	public static final String NAME = "SoX";

	private static final long serialVersionUID = -402768544068369154L;
	
	private static final String SoX_HOMEPAGE_URI = "http://sox.sourceforge.net/Main/HomePage";
	
	private static final String SHOW_PROGRESS_PARAM = "showProgress";
	private static final String NO_SHOW_PROGRESS_PARAM = "noShowProgress";
	private static final String VERBOSITY_LEVEL_PARAM = "verbosityLevel";
	private static final String ADVANCED_CLI_PARAM = "advancedCmd";
	
	private static final String br = System.getProperty("line.separator");
	
	private static boolean USE_ADVANCED_CLI = false;
	private static String CLI_STRING = null;
	
	private static List<URI> inFormats = SoXHelper.getSupportedInputFormats();
	private static List<URI> outFormats = SoXHelper.getSupportedOutputFormats();
	private static String sox_version = SoXHelper.getVersion();
	private static String sox_help = SoXHelper.getHelpText();
	
	private static Logger log = Logger.getLogger(SoX.class.getName());
	
	
	/**
    * SOX tool name as a string
    */
	public final String SOX = "sox";
	
	/**
     * the SOX working directory
     */
    public static  String SoX_WORK_DIR = "SOX";
    
    /**
     * the SOX input dir
     */
    public static  String SoX_IN = "INPUT";
    
    /**
     * the SOX output dir
     */
    public static  String SoX_OUTPUT_DIR = "OUT";
    
    /**
     * SOX home dir
     */
    public String SOX_HOME = null;
    
    public static String processOutput = "";
    public static String processError = "";
	
	public SoX() {
		// Getting SoX installation location 
		SOX_HOME = System.getenv("SOX_HOME");
    	
    	if(SOX_HOME==null){
    		System.err.println("SOX_HOME is not set! Please create an system variable\n" +
    				"and point it to the SoX installation folder!");
    		log.severe("SOX_HOME is not set! Please create an system variable\n" +
    				"and point it to the SoX installation folder!");
    		SOX_HOME = "";
    	} else {
    	    SOX_HOME += File.separator;
    	}
		
        log.info("Found SoX installation in: " + SOX_HOME);
	}

	/* (non-Javadoc)
	 * @see eu.planets_project.services.migrate.Migrate#describe()
	 */
	public ServiceDescription describe() {
		ServiceDescription.Builder sd = new ServiceDescription.Builder(NAME,Migrate.class.getCanonicalName());
        sd.author("Peter Melms, mailto:peter.melms@uni-koeln.de");
        sd.description("A wrapper for the SoX Audio Converter. Using SoX " + sox_version + 
        		"\n" +
        		"This service accepts input and target formats of this shape: 'planets:fmt/ext/[extension]'\n" +
        		"e.g. 'planets:fmt/ext/wav' or 'planets:fmt/ext/au'\n" +
        		"\n" +
        		"IMPORTANT NOTE:\n" +
        		"---------------\n" +
        		"Decoding and/or Encoding from or to 'mp3', 'ogg' or 'flac' requires additional libraries to be installed\n" +
        		"(on the machine where the the SoX tool is installed, i.e. the machine this service runs on)!!!\n" +
        		"So it might be that some migrations from or to these formats will not deliver the expected results!");
        
        sd.classname(this.getClass().getCanonicalName());
        sd.version("0.1");
        sd.name("Sox Audio Converter Service");
        sd.type(Migrate.class.getCanonicalName());
        List<Parameter> parameterList = new ArrayList<Parameter>();
        
        Parameter showProgress = new Parameter.Builder("showProgress", "-S")
                .description(
                        "Display input file format/header information, and processing "
                                + "progress as input file(s) percentage complete, elapsed time, and remaining time "
                                + "(if known; shown in brackets), and the number of samples written to the output file. "
                                + "Also shown is a VU meter, and an indication if clipping has occurred.")
                .build();
        parameterList.add(showProgress);
        
//        Parameter noShowProgress = new Parameter("noShowProgress", "-q");
//        noShowProgress.setDescription("Run in quiet mode when SoX wouldn't otherwise do so; this is the opposite of the -S option.");
//        parameterList.add(noShowProgress);
        
        Parameter verbosityLevel = new Parameter.Builder("verbosityLevel",
                "1-6")
                .description(
                        "Increment or set verbosity level (default 2); levels:" + br + 
                        "1: failure messages" + br + 
                        "2: warnings" + br + 
                        "3: details of processing" + br + 
                        "4-6: increasing levels of debug messages")
                .build();
        parameterList.add(verbosityLevel);
        
        Parameter advancedCLI = new Parameter.Builder(ADVANCED_CLI_PARAM, "[gopts] [[fopts] #INFILE#]... [fopts] #OUTFILE# [effect [effopts]]...")
        .description(sox_help + br + "The #INFILE# and #OUTFILE# parts are placeholder, " +
        		"where the service fills in the actual files." + br + 
        		"You don't have to put the tools' name in the command line, this will be added by the service!" + br + 
        		"Please be aware of what you are doing, this command will be passed through to the Command line tool directly!")
        		.build();
        parameterList.add(advancedCLI);
        
        sd.parameters(parameterList);

        sd.tool(Tool.create(null, "SoX", sox_version, null, SoX_HOMEPAGE_URI));
        sd.logo(URI.create("http://sox.sourceforge.net/sox-logo.png"));
		
//		List<String> inputFormats = new ArrayList<String> ();
//		inputFormats.add("MP3");	// mp3 needs additional library, e.g. "lame"
//        inputFormats.add("WAV");
//		inputFormats.add("AIFF");
//		inputFormats.add("FLAC");
//		inputFormats.add("OGG");
//		inputFormats.add("RAW");	// raw does not work as input, but works as output
		
//		List<String> outputFormats = new ArrayList<String> ();
////		outputFormats.add("MP3");	// mp3 needs additional library, e.g. "lame"
//		outputFormats.add("WAV");
//		outputFormats.add("AIFF");
//		outputFormats.add("FLAC");
//		outputFormats.add("OGG");
//		outputFormats.add("RAW");
		
		// creating all possible/sensible combinations
		sd.paths(ServiceUtils.createMigrationPathways(inFormats, outFormats));
		return sd.build();
	}

	/* (non-Javadoc)
	 * @see eu.planets_project.services.migrate.Migrate#migrate(eu.planets_project.services.datatypes.DigitalObject, java.net.URI, java.net.URI, eu.planets_project.services.datatypes.Parameter)
	 */
	public MigrateResult migrate(DigitalObject digitalObject, URI inputFormat,
			URI outputFormat, List<Parameter> parameters) {
		
		String inputError = null;
		boolean inputErrDetected = false;
		String outputError = null;
		boolean outputErrDetected = false;
		
		/* Is the input format supported by this SoX service?
		 * if not, an error message is set up
		 */
		if(!isInputFormatSupported(inputFormat)) {
			inputError = "Unsupported InputFormat " + inputFormat.toASCIIString() + 
			". No result file has been created!";
			inputErrDetected = true;
		}
		
		/* Is the output format supported by this SoX service?
		 * if not, an error message is set up
		 */
		if(!isOutputFormatSupported(outputFormat)) {
			outputError = "Unsupported OutputFormat " + outputFormat.toASCIIString() + 
			". No result file has been created!";
			outputErrDetected = true;
		}

		/* 
		 * Is this an unsupported migration path way?
		 */
		if(inputErrDetected & outputErrDetected) {
			String combinedErrorMessage = "Unsupported migration path: [" + inputFormat.toASCIIString()
											+ " --> " 
											+ outputFormat.toASCIIString()
											+ "] is not supported! No result file has been created!";
			return this.returnWithErrorMessage(combinedErrorMessage, null);
		}
		
		// if we found an invalid input format, cancel execution and return with 
		// an error report
		if(inputErrDetected) {
			return this.returnWithErrorMessage(inputError, null);
		}
		
		// if we found an invalid output format, cancel execution and return with 
		// an error report
		if(outputErrDetected) {
			return this.returnWithErrorMessage(outputError, null);
		}
		
		// executing the migration by calling the convertAudio(...) method
		// and returning the MigrateResult
		return convertAudio(digitalObject, inputFormat, outputFormat, parameters);
		
	}
	
	/* 
	 * Method to test, whether the inputFormat is supported by this service, i.e. by testing 
	 * whether the input format is found in the possible migration paths 
	 */
	private boolean isInputFormatSupported(URI inputFormat) {
		MigrationPath[] migrationPaths = ServiceUtils.createMigrationPathways(inFormats, outFormats);
		boolean isSupported = false;
		
		for (int i = 0; i < migrationPaths.length; i++) {
			URI currentInputFormat = migrationPaths[i].getInputFormat();
			if(currentInputFormat.toASCIIString().equalsIgnoreCase(inputFormat.toASCIIString())) {
				isSupported = true;
				return isSupported;
			}
		}
		return isSupported;
	}
	
	/* 
	 * Method to test, whether the output Format is supported by this service, i.e. by testing 
	 * whether the output format is found in the possible migration paths 
	 */
	private boolean isOutputFormatSupported(URI outputFormat) {
		MigrationPath[] migrationPaths = ServiceUtils.createMigrationPathways(inFormats, outFormats);
		boolean isSupported = false;
		
		for (int i = 0; i < migrationPaths.length; i++) {
			URI currentOutputFormat = migrationPaths[i].getOutputFormat();
			if(currentOutputFormat.toASCIIString().equalsIgnoreCase(outputFormat.toASCIIString())) {
				isSupported = true;
				return isSupported;
			}
		}
		return isSupported;
	}
	
	
	
	/**
	 * 
     * @param input input data as DigitalObject
     * @param inputFormat a format URI specifying the input format (e.g. planets:format/ext/wav) 
     * @param outputFormat a format URI specifying the destination format 
     * @param parameters some additional parameters you wish to pass to the underlying tool?
     * @return the migrated DigitalObject along with a ServiceReport
     */
    private MigrateResult convertAudio(DigitalObject input,
            URI inputFormat, URI outputFormat, List<Parameter> parameters) {
    	
    	FormatRegistry format = FormatRegistryFactory.getFormatRegistry();
        String srcExt = format.getFirstExtension(inputFormat);
		String destExt = format.getFirstExtension(outputFormat);
    	
        if (!srcExt.startsWith("."))
        	srcExt = "." + srcExt;
        
        if (!destExt.startsWith("."))
        	destExt = "." + destExt;
        
        log.info("Starting migration. Converting from "
                + inputFormat.toASCIIString() + " to " + outputFormat.toASCIIString());
        
     // getting the input data from the DigitalObject and writing it to a File...
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("planets", srcExt);
            outputFile = File.createTempFile("planets", destExt);
        } catch (IOException x) {
            throw new IllegalStateException("Could not create temp files.", x);
        }
        
        // setting up the command line 
        List<String> soxCommands = new ArrayList<String>();
        if(SOX_HOME!=null && !SOX_HOME.equalsIgnoreCase("")) {
        	soxCommands.add(SOX_HOME + SOX);	// the path and name of the tool itself
        }
        else {
        	soxCommands.add(SOX);
        }
        if(srcExt.equalsIgnoreCase(".raw")) {
        	log.warning("RAW format detected! Using defaults. Please be aware that this might produce unpredictable results!"
        			+ br +"To make sure you are using the correct settings, please use the advancedCmd parameter and pass the command line directly!");
        	soxCommands.add("-r");
        	soxCommands.add("44100");
        	soxCommands.add("-g");
        }
        soxCommands.add(inputFile.getAbsolutePath());	// the input file
        
        if(destExt.equalsIgnoreCase(".raw")) {
        	log.warning("RAW format detected! Using defaults. Please be aware that this might produce unpredictable results!"
        			+ br +"To make sure you are using the correct settings, please use the advancedCmd parameter and pass the command line directly!");
        	soxCommands.add("-r");
        	soxCommands.add("44100");
        	soxCommands.add("-g");
        }
        soxCommands.add(outputFile.getAbsolutePath());	// the output file path
        
        
     // Are there any additional parameters for us?
		if(parameters != null) {
			log.info("Got additional parameters:");
			String showProgress;
			String noShowProgress;
			String verbosityLevel;
			
			for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext();) {
				Parameter parameter = (Parameter) iterator.next();
				String name = parameter.getName();
				String value = parameter.getValue();
				
				log.info("Got parameter: " + name + " with value: " + value);
				if(!name.equalsIgnoreCase(SHOW_PROGRESS_PARAM) 
						&& !name.equalsIgnoreCase(NO_SHOW_PROGRESS_PARAM) 
						&& !name.equalsIgnoreCase(VERBOSITY_LEVEL_PARAM) 
						&& !name.equalsIgnoreCase(ADVANCED_CLI_PARAM)) {
					log.info("Invalid parameter with name: " + parameter.getName()+"\n using DEFAULT values.");
				}
				
				if(name.equalsIgnoreCase(ADVANCED_CLI_PARAM)) {
					USE_ADVANCED_CLI = true;
					CLI_STRING = value;
					break;
				}
				
				if(name.equalsIgnoreCase(SHOW_PROGRESS_PARAM)) {
					showProgress = value;
					soxCommands.add(showProgress);
					log.info("Enabling 'showProgress' feature '-S'.");
					continue;
				}
				
				if(name.equalsIgnoreCase(NO_SHOW_PROGRESS_PARAM)) {
					noShowProgress = value;
					soxCommands.add(noShowProgress);
					log.info("Enabling 'noShowProgress' feature '-q' or 'quiet mode'.");
				}
				
				if(name.equalsIgnoreCase(VERBOSITY_LEVEL_PARAM)){
					verbosityLevel = value;
					String parameterString = "-V" + verbosityLevel;
					soxCommands.add(parameterString);
					log.info("Setting verbosity level to: " + verbosityLevel + "\n" +
							"(Please see ServiceDescription for further information)");
				}
			}
		}
        
		//**********************************************************************
        // Configuring and running the ProcessRunner
		ProcessRunner pr = new ProcessRunner();
		if(USE_ADVANCED_CLI) {
			List<String> cmd = getAdvancedCmd(CLI_STRING, inputFile, outputFile.getAbsolutePath());
			pr.setCommand(cmd);
			log.info("Executing: " + cmd);
			soxCommands = cmd;
			USE_ADVANCED_CLI = false;
		}
		else {
			pr.setCommand(soxCommands);
			log.info("Executing: " + soxCommands);
		}
		if( SOX_HOME != null && ! "".equals(SOX_HOME)) {
		    pr.setStartingDir(new File(SOX_HOME));
		}
        
        pr.run();

        processOutput = pr.getProcessOutputAsString();
        processError = pr.getProcessErrorAsString();
        
        log.info("SOX call output: " + processOutput);
        log.severe("SOX call error: " + processError);
        
        log.info("Executing " + soxCommands + " finished.");

        log.info("Migration finished.");
		//**********************************************************************
        
        
        
        // if an output file has been created by the service,
        // read it into a byte[]
        if(outputFile.canRead()) {
        	DigitalObject resultDigObj = null;
			resultDigObj = createDigitalObjectByReference(outputFile);
			
			log.info("Created new DigitalObject for result file...");
            log.info("Output file lenght: " + outputFile.length());
            
         // create a ServiceReport...
            ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS,
                    "Output and error logs:\n" + "--------------------------\n"
                            + processOutput + "\n" + processError);
			
			log.info("Created Service report...");
			
			log.info("Success!! Returning results!");
			// and create a MigrateResult
			MigrateResult migrateResult = new MigrateResult(resultDigObj, report);

			// and return the result
			return migrateResult;
        }
        
        // if no output file has been created, log out an error and return with an Error report!
        else {
        	log.severe("No result file created. Maybe SoX is missing a required library for this migration?");
        	return this.returnWithErrorMessage("No result file created. Maybe SoX is missing a required library for this migration?", null);
        }
    }
    
    private List<String> getAdvancedCmd(String cmdLine, File srcFile, String outFileName) {
    	String[] parts = cmdLine.split(" ");
    	ArrayList<String> cmd = new ArrayList<String>();
    	if(!SOX_HOME.equalsIgnoreCase("")) {
    		cmd.add(SOX_HOME + SOX);
    	}
    	else {
    		cmd.add(SOX);
    	}
    	for (String currentPart : parts) {
			if(currentPart.equalsIgnoreCase("#INFILE#") || currentPart.equalsIgnoreCase("infile")) {
				cmd.add(srcFile.getAbsolutePath());
				continue;
			}
			if(currentPart.equalsIgnoreCase("#OUTFILE#")|| currentPart.equalsIgnoreCase("outfile")) {
				cmd.add(outFileName);
				continue;
			}
			cmd.add(currentPart);
		}
    	return cmd;
    }
    
    // Creates a MigrateResult, containing a ServiceReport with the passed
    // error message, and optional a thrown exception e
    private MigrateResult returnWithErrorMessage(String message, Exception e ) {
        if( e == null ) {
        	ServiceReport errorReport = ServiceUtils.createErrorReport(message
        			+ "\n" 
					+ "Process error log:\n" 
					+ "------------------\n"
					+ processError);
            return new MigrateResult(null, errorReport);
        } else {
        	ServiceReport exceptionErrorReport = ServiceUtils.createExceptionErrorReport(message
        			+ "\n" 
					+ "Process error log:\n" 
					+ "------------------\n"
					+ processError + "\n", e);
            return new MigrateResult(null, exceptionErrorReport);
        }
    }
	
	
	// Convenience method to create a DigitalObject byValue (File)
	private DigitalObject createDigitalObjectByReference(File resultFile) {
		DigitalObject digObj =  new DigitalObject.Builder(Content.byReference(resultFile)).build();
		return digObj;
	}
	
	
}
