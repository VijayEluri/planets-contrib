/*
 *
 */
package eu.planets_project.services.migration.avidemux;

import eu.planets_project.ifr.core.techreg.api.formats.Format;
import eu.planets_project.ifr.core.techreg.impl.formats.FormatRegistryImpl;
import java.io.Serializable;
import java.net.URI;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebService;

import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.MigrationPath;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.Parameters;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.ByteArrayHelper;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.PlanetsLogger;
import eu.planets_project.services.utils.ProcessRunner;

import eu.planets_project.services.utils.ServiceUtils;
import java.io.File;
import java.io.InputStream;

import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



/**
 * The AvidemuxMigration migrates JPEG files to JP2 files and vice versa.
 *
 * @author Sven Schlarb <shsschlarb-planets@yahoo.de>
 */
@Local(Migrate.class)
@Remote(Migrate.class)
@Stateless
@WebService( name = AvidemuxMigration.NAME ,
        serviceName = Migrate.NAME,
        targetNamespace = PlanetsServices.NS,
        endpointInterface = "eu.planets_project.services.migrate.Migrate" )
public final class AvidemuxMigration implements Migrate, Serializable {

    PlanetsLogger log = PlanetsLogger.getLogger(AvidemuxMigration.class);


    /** The dvi ps installation dir */
    public String avidemux_install_dir;
    /** The avidemux application name */
    public String avidemux_app_name;
    /** The output file extension */
    //public String avidemux_outfile_ext;
    private File tmpInFile;
    private File tmpOutFile;

    String inputFmtExt = null;
    String outputFmtExt = null;

    /***/
    static final String NAME = "AvidemuxMigration";


    List<String> inputFormats = null;
    List<String> outputFormats = null;
    HashMap<String, String>  formatMapping = null;
    HashMap<String, Parameter> defaultParameters = null;

    List<Parameter> parameterList;

    /***/
    private static final long serialVersionUID = 2127494848765937613L;

    private List<String> getCommandsStringArray() {
        // Example command:
        List<String> commands = new ArrayList<String>();
        if( avidemux_app_name == null || tmpOutFile == null || tmpInFile == null )
            return null;
        // Simple example command:
        // avidemux --nogui --autoindex --load demonstration2.mpeg
        // --audio-codec NONE --video-codec XVID4 --output-format AVI
        // --save out2.avi --quit
        commands.add(this.avidemux_install_dir+"/"+this.avidemux_app_name);
        commands.add("--nogui");
        commands.add("--autoindex");
        commands.add("--load");
        commands.add(this.tmpInFile.getAbsolutePath());
        /* FIXME Audio bitrate has been deactivated - Correct input has to be checked, otherwise crash in libc */
//        commands.add("--audio-bitrate");
//        commands.add(this.defaultParameters.get("audio-bitrate").value);
        commands.add("--fps");
        commands.add(this.defaultParameters.get("fps").value);
        commands.add("--audio-codec");
        commands.add(this.defaultParameters.get("audio-codec").value);
        commands.add("--video-codec");
        String vidCod = this.defaultParameters.get("video-codec").value;
        commands.add(vidCod);
        commands.add("--output-format");
        commands.add("AVI");
        commands.add("--save");
        commands.add(this.tmpOutFile.getAbsolutePath());
        commands.add("--quit");
        System.out.println(commands.toString());
        return commands;
    }

    private void initFormats()
    {
        // input formats
        inputFormats = new ArrayList<String>();
        inputFormats.add("mpeg");

        // output formats and associated output parameters
        outputFormats = new ArrayList<String>();
        outputFormats.add("avi");


        // Disambiguation of extensions, e.g. {"JPG","JPEG"} to {"JPEG"}
        // FIXIT This should be supported by the FormatRegistryImpl class, but
        // it does not provide the complete set at the moment.
        formatMapping = new HashMap<String, String>();
        formatMapping.put("mpg","mpeg");
    }

    /**
     * Initialize the parameters list for all migration file formats. Every
     * parameter has a default value which is overridden where requested
     * from the user (parameters contains the parameters passed to the
     * service by the user).
     */
    private void initParameters()
    {
        // main parameters hashmap
        // ...
        defaultParameters = new HashMap<String, Parameter>();

        // Define parameters and default values
        // AVI
        String audioCodecParamName = "audio-codec";
        Parameter audioCodecParam = new Parameter(audioCodecParamName, "NONE");
        audioCodecParam.setDescription("Choose one of MP2, MP3, AC3, NONE, TWOLAME, COPY. The audio track will be encoded using the selected encoder (COPY leaves the audio-track unchanged).");
        this.defaultParameters.put(audioCodecParamName, audioCodecParam);

        String videoCodecParamName = "video-codec";
        Parameter videoCodecParam = new Parameter(videoCodecParamName, "XVID4");
        videoCodecParam.setDescription("Choose one of XVID4, X264, FFMPEG4 (LavCodec), COPY (one argument). The video track will be encoded using the selected encoder (COPY leaves the video-track unchanged)");
        this.defaultParameters.put(videoCodecParamName, videoCodecParam);

        /* FIXME Audio bitrate has been deactivated - Correct input has to be checked, otherwise crash in libc! */
//        String audioBitrateParamName = "audio-bitrate";
//        Parameter audioBitrateParam = new Parameter(audioBitrateParamName, "128");
//        audioBitrateParam.setDescription("Choose one of the integers 56, 64, 80, 96, 112, 128, 160, 192, 224 for the audio bitrate.");
//        this.defaultParameters.put(audioBitrateParamName, audioBitrateParam);

        String fpsParamName = "fps";
        Parameter fpsParam = new Parameter(fpsParamName, "30");
        fpsParam.setDescription("Positive integer for the frames per second of the video.");
        this.defaultParameters.put(fpsParamName, fpsParam);

        // Get list for the migration path parameter
        parameterList = this.getParameters().getParameters();
    }

    /**
     * {@inheritDoc}
     *
     * @see eu.planets_project.services.migrate.Migrate#migrate(eu.planets_project.services.datatypes.DigitalObject, java.net.URI, java.net.URI, eu.planets_project.services.datatypes.Parameters)
     */
    public MigrateResult migrate( final DigitalObject digitalObject, URI inputFormat,
            URI outputFormat, Parameters parameters) {

        Properties props = new Properties();
        try {

            String strRsc = "/eu/planets_project/services/migration/avidemux/avidemux.properties";
            props.load( this.getClass().getResourceAsStream(strRsc));
            // config vars
            this.avidemux_install_dir = props.getProperty("avidemux.install.dir");
            this.avidemux_app_name = props.getProperty("avidemux.app.name");

        } catch( Exception e ) {
            // // config vars
            this.avidemux_install_dir  = "/usr/bin";
            this.avidemux_app_name = "avidemux2_cli";
        }
        log.info("Using avidemux install directory: "+this.avidemux_install_dir);
        log.info("Using avidemux application name: "+this.avidemux_app_name);

        // Init formats, extensions, and parameters
        initFormats();
        getExtensions(inputFormat,outputFormat);
        initParameters();

        // override the default parameters initialised above  by the parameters
        // passed by the user
        overrideDefaultParamets(parameters);

        /*
         * We just return a new digital object with the same required arguments
         * as the given:
         */
        byte[] binary = null;
        InputStream inputStream = digitalObject.getContent().read();

        // write input stream to temporary file
        tmpInFile = FileUtils.writeInputStreamToTmpFile(inputStream, "planets", inputFmtExt);
        if( !(tmpInFile.exists() && tmpInFile.isFile() && tmpInFile.canRead() ))
        {
            String errorMsg = "[AvidemuxMigration] Unable to create temporary input file!";
            log.error(errorMsg);
            return new MigrateResult( null, ServiceUtils.createErrorReport(errorMsg) );
        }
        log.info("[AvidemuxMigration] Temporary input file created: "+tmpInFile.getAbsolutePath());

        // outfile name
        String outFileStr = tmpInFile.getAbsolutePath()+"."+outputFmtExt;
        log.info("[AvidemuxMigration] Output file name: "+outFileStr);
        tmpOutFile = new File(outFileStr);
        
        // run command
        ProcessRunner runner = new ProcessRunner();
//        List<String> command = new ArrayList<String>();

        // commands string array
        List<String> commands = getCommandsStringArray();
        log.info("[AvidemuxMigration] Executing command: "+commands.toString() +" ...");
        runner.setCommand(commands);
        runner.setInputStream(inputStream);
        runner.run();
        int return_code = runner.getReturnCode();
        if (return_code != 0){
            String errorMsg = "[AvidemuxMigration] Avidemux conversion error. Error code: " +
                    Integer.toString(return_code) + ". Error message: " + runner.getProcessErrorAsString();
            log.error(errorMsg);
            return new MigrateResult( null, ServiceUtils.createErrorReport(errorMsg) );
        }

        
        // read byte array from temporary file
        if( tmpOutFile.isFile() && tmpOutFile.canRead() )
            binary = ByteArrayHelper.read(tmpOutFile);
        else {
            String errorMsg =  "[AvidemuxMigration] Error: Unable to read temporary file "+tmpOutFile.getAbsolutePath();
            log.error(errorMsg);
            return new MigrateResult( null, ServiceUtils.createErrorReport(errorMsg) );
        }
                
            
        DigitalObject newDO = null;
        ServiceReport report = new ServiceReport();
        newDO = new DigitalObject.Builder(Content.byValue(binary)).build();
        return new MigrateResult(newDO, report);
    }

    private void overrideDefaultParamets(Parameters userParams)
    {
        // Change default parameters according to the parameters defined by the
        // user
        if( userParams != null )
        {
            List<Parameter> userParamList = userParams.getParameters(); // User parameters
            Iterator<Parameter> userParmsItr = userParamList.iterator();
            while(userParmsItr.hasNext()) {
                Parameter userParam = (Parameter) userParmsItr.next();
                log.info("Set parameter: " + userParam.name + " with value: " + userParam.value);
                if( defaultParameters.containsKey(userParam.name))
                    this.defaultParameters.put(userParam.name, userParam);
                else
                    log.info("[AvidemuxMigration] Warning: Parameter ignored: " + userParam.name + " with value: " + userParam.value);
            }
        }
    }

    private void getExtensions(URI inputFormat, URI outputFormat)
    {
        if( inputFormat != null && outputFormat != null )
        {
            inputFmtExt = getFormatExt( inputFormat, false );
            outputFmtExt = getFormatExt( outputFormat, true );
        }
    }

    /**
     * Gets one extension from a set of possible extensions for the incoming
     * request planets URI (e.g. planets:fmt/ext/jpeg) which matches with
     * one format of the set of avidemux's supported input/output formats. If
     * isOutput is false, it checks against the gimp input formats ArrayList,
     * otherwise it checks against the gimp output formats HashMap.
     *
     * @param formatUri Planets URI (e.g. planets:fmt/ext/jpeg)
     * @param isOutput Is the format an input or an output format
     * @return Format extension (e.g. "JPEG")
     */
    private String getFormatExt( URI formatUri, boolean isOutput  )
    {
        String fmtStr = null;
        // status variable which indicates if an input/out format has been found
        // while iterating over possible matches
        boolean fmtFound = false;
        // Extensions which correspond to the format
        // planets:fmt/ext/jpg -> { "JPEG", "JPG" }
        // or can be found in the list of supported formats
        FormatRegistryImpl fmtRegImpl = new FormatRegistryImpl();
        Format uriFormatObj = fmtRegImpl.getFormatForURI(formatUri);
        Set<String> reqInputFormatExts = uriFormatObj.getExtensions();
        Iterator<String> itrReq = reqInputFormatExts.iterator();
        // Iterate either over input formats ArrayList or over output formats
        // HasMap
        Iterator<String> itrAvidemux = (isOutput)?outputFormats.iterator():inputFormats.iterator();
        // Iterate over possible extensions that correspond to the request
        // planets uri.
        while(itrReq.hasNext()) {
            // Iterate over the different extensions of the planets:fmt/ext/jpg
            // format URI, note that the relation of Planets-format-URI to
            // extensions is 1 : n.
            String reqFmtExt = normalizeExt((String) itrReq.next());
            while(itrAvidemux.hasNext()) {
                // Iterate over the formats that avidemux offers either as input or
                // as output format.
                // See input formats in the this.init() method to see the
                // avidemux input/output formats offered by this service.
                String gimpFmtStr = (String) itrAvidemux.next();
                if( reqFmtExt.equalsIgnoreCase(gimpFmtStr) )
                {
                    // select the gimp supported format
                    fmtStr = gimpFmtStr;
                    fmtFound = true;
                    break;
                }
                if( fmtFound )
                    break;
            }
        }
        return fmtStr;
    }

    /**
     * Disambiguation (e.g. JPG -> JPEG) according to the formatMapping
     * datas structure defined in this class.
     *
     * @param ext
     * @return Uppercase disambiguized extension string
     */
    private String normalizeExt(String ext)
    {
        String normExt = ext.toUpperCase();
        return ((formatMapping.containsKey(normExt))?
            (String)formatMapping.get(normExt):normExt);
    }

    private Parameters getParameters()
    {
        Parameters parameters = new Parameters();
        List<Parameter> paramList = new ArrayList<Parameter>();
        Iterator<String> itr = defaultParameters.keySet().iterator();
        while( itr.hasNext() )
        {
            String key = (String)itr.next();

            Parameter param = (Parameter) defaultParameters.get(key);
            paramList.add(param);
        }
        parameters.setParameters(paramList);
        return parameters;
    }

    /**
     * @see eu.planets_project.services.migrate.Migrate#describe()
     */
    public ServiceDescription describe() {

        this.initFormats();
        initParameters();
        Parameters parameters = getParameters();
        ServiceDescription.Builder builder = new ServiceDescription.Builder(NAME, Migrate.class.getName());
        builder.author("Sven Schlarb <shsschlarb-planets@yahoo.de>");
        builder.classname(this.getClass().getCanonicalName());
        builder.description(
            "This service offers MPEG to AVI migration using a small subset of " +
            "the configuration parameters that the program Avidemux offers. "+
            "Avidemux offers a GUI (Grafical user interface) "+
            "and a CLI (Command line interface) for video editing and " +
            "conversion with numerous configuration parameters and filters for " +
            "compressing the video file. A huge set of parameters allows reducing " +
            "the file size while keeping - if the parameters are combined thoroughly -  " +
            "video and audio quality close to the original video file.\n\n" +
            "The service offers some of the configuration options, like codecs " +
            "and filters for the audio and/or the video track of " +
            "the video file, or options like frames per second " +
            "of the video track, etc. Furthermore, it offers filters, like e.g.  " +
            "deinterlacing, noise reduction, or sharpen filters.\n" +
            "Please contact the service developer if specific parameters " +
            "should be considered.\n\n" +
            "The service requires the package avidemux-cli (openSUSE/debian based Linux " +
            "distribution). Only if this package is installed, the command line " +
            "program /usr/bin/avidemux2_cli is available. Often there is a symbol " +
            "/usr/bin/avidemux2_qt4 to the Qt-GUI version."+
            "Further information on Avidemux (source: Wikipedia):\n"+
            "Avidemux is a free open-source program designed for "+
            "multi-purpose video editing and processing. It is written in C/C++, "+
            "using either the GTK+ or Qt graphics toolkit or a command line interface, "+
            "and is a platform independent, universal video processing program. "+
            "It is available for almost all distributions of Linux that are "+
            "capable of compiling C/C++, GTK+ and the SpiderMonkey ECMAScript "+
            "scripting engine. A Win32 version of this program is also available "+
            "for Windows users, as well as Mac OS X, FreeBSD, NetBSD and OpenBSD "+
            "ports and packages. The program has also been successfully run "+
            "under Solaris, though no official packages or binaries exist for it. "+
            "The program can be run in 64-bit operating systems that are "+
            "non-Windows and non-Macintosh based.\n");
        MigrationPath[] mPaths = new MigrationPath []{
            new MigrationPath(Format.extensionToURI("mpeg"), Format.extensionToURI("avi"),this.parameterList)
        };

        builder.paths(mPaths);
        builder.parameters(parameters);
        builder.classname(this.getClass().getCanonicalName());
        builder.version("0.1");

        ServiceDescription mds =builder.build();

        return mds;
    }
}
