/*
 *
 */
package eu.planets_project.services.migration.kakadu;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebService;

import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.MigrationPath;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.PlanetsLogger;
import eu.planets_project.services.utils.ProcessRunner;

/**
 * The KakaduCompressMigration migrates TIF files to JP2 files.
 * @author Sven Schlarb <shsschlarb-planets@yahoo.de>
 */
@Local(Migrate.class)
@Remote(Migrate.class)
@Stateless
@WebService(name = KakaduCompressMigration.NAME, serviceName = Migrate.NAME, targetNamespace = PlanetsServices.NS, endpointInterface = "eu.planets_project.services.migrate.Migrate")
public final class KakaduCompressMigration implements Migrate {

    PlanetsLogger log = PlanetsLogger.getLogger(KakaduCompressMigration.class);
    /** The dvi ps installation dir */
    public String kakadu_install_dir;
    /** The kakadu application name */
    public String kakadu_app_name;
    /** The output file extension */
    // public String kakadu_outfile_ext;
    private File tmpInFile;
    private File tmpOutFile;
    String inputFmtExt = null;
    String outputFmtExt = null;
    /***/
    static final String NAME = "KakaduCompressMigration";
    List<String> inputFormats = null;
    List<String> outputFormats = null;
    HashMap<String, String> formatMapping = null;
    List<Parameter> serviceParametersList;
    List<Parameter> requestParametersList;
    StringBuffer serviceMessage = null;
    KakaduCompressServiceParameters kduServiceParameters = null;
    
    /***/
    private static final long serialVersionUID = 2127494848765937613L;

    private void init() {
        // input formats
        inputFormats = new ArrayList<String>();
        // TIF, RAW, BMP, PBM, PGM and PPM
        inputFormats.add("tif");
        inputFormats.add("raw");
        inputFormats.add("bmp");
        inputFormats.add("pbm");
        inputFormats.add("pgm");
        inputFormats.add("ppm");
        

        // output formats and associated output parameters
        outputFormats = new ArrayList<String>();
        outputFormats.add("jp2");

        // Disambiguation of extensions, e.g. {"JPG","JPEG"} to {"JPEG"}
        // FIXIT This should be supported by the FormatRegistryImpl class, but
        // it does not provide the complete set at the moment.
        formatMapping = new HashMap<String, String>();
        formatMapping.put("tiff", "tif");
        serviceMessage = new StringBuffer();

        kduServiceParameters = new KakaduCompressServiceParameters();
        
    }

    /**
     * Initialize the parameters list for all migration file formats. Every
     * parameter has a default value which is overridden where requested
     * from the user (parameters contains the parameters passed to the
     * service by the user).
     */
    private void initParameters() {
        serviceParametersList = KakaduCompressServiceParameters.getParameterList();
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.services.migrate.Migrate#migrate(eu.planets_project.services.datatypes.DigitalObject,
     *      java.net.URI, java.net.URI,
     *      eu.planets_project.services.datatypes.Parameter)
     */
    public MigrateResult migrate(final DigitalObject digitalObject,
            URI inputFormat, URI outputFormat, List<Parameter> parameters) {
        requestParametersList = parameters;
        Properties props = new Properties();
        try {

            String strRsc = "/eu/planets_project/services/migration/kakadu/kakadu.properties";
            props.load(this.getClass().getResourceAsStream(strRsc));
            // config vars
            this.kakadu_install_dir = props.getProperty("kakadu.install.dir");
            this.kakadu_app_name = props.getProperty("kakadu.app.name");

        } catch (Exception e) {
            // // config vars
            this.kakadu_install_dir = "C:/Programme/Kakadu";
            this.kakadu_app_name = "kdu_compress";
        }
        log.info("Using kakadu install directory: " + this.kakadu_install_dir);
        log.info("Using kakadu application name: " + this.kakadu_app_name);

        init();
        // Initialise parameters with default values
        initParameters();
        getExtensions(inputFormat, outputFormat);

        /*
         * We just return a new digital object with the same required arguments
         * as the given:
         */
        byte[] binary = null;
        InputStream inputStream = digitalObject.getContent().read();

        // write input stream to temporary file
        tmpInFile = FileUtils.writeInputStreamToTmpFile(inputStream, "planets",
                inputFmtExt);
        if (!(tmpInFile.exists() && tmpInFile.isFile() && tmpInFile.canRead())) {
            log.error("Unable to create temporary input file!");
            return null;
        }
        log.info("Temporary input file created: " + tmpInFile.getAbsolutePath());

        // outfile name
        String outFileStr = tmpInFile.getAbsolutePath().replaceAll(inputFmtExt, "jp2");

        log.info("Output file name: " + outFileStr);

        StringBuffer serviceReportMessage = new StringBuffer();
        // run command
        ProcessRunner runner = new ProcessRunner();
        List<String> command = new ArrayList<String>();
        // setting up command
        // -i source.tif -o destination.jp2 Creversible=yes -rate -,1,0.5,0.25 Clevels=5
        command.add(this.kakadu_app_name);
        command.add("-i");
        command.add(tmpInFile.getAbsolutePath());
        command.add("-o");
        command.add(outFileStr);
        for(Parameter requestParm : requestParametersList) {
            ServiceParameter servParm = kduServiceParameters.getParameter(requestParm.getName());
            if(servParm != null) {
                servParm.setRequestValue(requestParm.getValue());
                if(servParm.isValid())
                    command.addAll(servParm.getCommandListItems());
                else
                    serviceReportMessage.append(servParm.getStatusMessage());
            } else
                serviceReportMessage.append("Parameter skipped: Service does not support parameter '"+requestParm.getName()+"'. ");
        }
      
        runner.setCommand(command);
        runner.setInputStream(inputStream);
        log.info("Executing command (update): " + command.toString() + " ...");
        runner.run();
        int return_code = runner.getReturnCode();
        if (return_code != 0) {
            log.error("Kakadu conversion error code: " + Integer.toString(return_code));
            log.error(runner.getProcessErrorAsString());
            return null;
        }

        tmpOutFile = new File(outFileStr);
        ServiceReport report;

        // read byte array from temporary file
        if (tmpOutFile.isFile() && tmpOutFile.canRead()) {
            binary = FileUtils.readFileIntoByteArray(tmpOutFile);
            report = new ServiceReport(Type.INFO, Status.SUCCESS, serviceReportMessage.toString()+"Wrote: " + tmpOutFile);
        } else {
            String message = "Error: Unable to read temporary file " + tmpOutFile.getAbsolutePath();
            log.error(message);
            report = new ServiceReport(Type.ERROR, Status.INSTALLATION_ERROR,
                    serviceReportMessage.toString()+message);
        }

        DigitalObject newDO = null;

        newDO = new DigitalObject.Builder(Content.byValue(binary)).build();

        return new MigrateResult(newDO, report);
    }

    /**
     * Get value of one parameter.
     *
     * @param ext Extension for which we retrieve the parameter list.
     * @return Paramter string
     */
    private String getRequestParameterValue(String parmName) {
        Iterator<Parameter> itr = this.requestParametersList.iterator();
        String paramValue = null;
        while (itr.hasNext()) {
            Parameter param = (Parameter) itr.next();
            if (param.getName() != null && param.getName().equals(parmName)) {
                paramValue = param.getValue();
                return paramValue;
            }
        }
        return null;
    }

    private void getExtensions(URI inputFormat, URI outputFormat) {
        if (inputFormat != null && outputFormat != null) {
            inputFmtExt = getFormatExt(inputFormat, false);
            outputFmtExt = getFormatExt(outputFormat, true);
        }
    }

    /**
     * Gets one extension from a set of possible extensions for the incoming
     * request planets URI (e.g. planets:fmt/ext/jpeg) which matches with one
     * format of the set of kakadu's supported input/output formats. If
     * isOutput is false, it checks against the gimp input formats ArrayList,
     * otherwise it checks against the gimp output formats HashMap.
     * @param formatUri Planets URI (e.g. planets:fmt/ext/jpeg)
     * @param isOutput Is the format an input or an output format
     * @return Format extension (e.g. "JPEG")
     */
    private String getFormatExt(URI formatUri, boolean isOutput) {
        String fmtStr = null;
        // status variable which indicates if an input/out format has been found
        // while iterating over possible matches
        boolean fmtFound = false;
        // Extensions which correspond to the format
        // planets:fmt/ext/jpg -> { "JPEG", "JPG" }
        // or can be found in the list of supported formats
        Set<String> reqInputFormatExts = FormatRegistryFactory.getFormatRegistry().getExtensions(formatUri);
        Iterator<String> itrReq = reqInputFormatExts.iterator();
        // Iterate either over input formats ArrayList or over output formats
        // HasMap
        Iterator<String> itrJasper = (isOutput) ? outputFormats.iterator()
                : inputFormats.iterator();
        // Iterate over possible extensions that correspond to the request
        // planets uri.
        while (itrReq.hasNext()) {
            // Iterate over the different extensions of the planets:fmt/ext/jpg
            // format URI, note that the relation of Planets-format-URI to
            // extensions is 1 : n.
            String reqFmtExt = normalizeExt((String) itrReq.next());
            while (itrJasper.hasNext()) {
                // Iterate over the formats that kakadu offers either as input
                // or
                // as output format.
                // See input formats in the this.init() method to see the
                // kakadu input/output formats offered by this service.
                String gimpFmtStr = (String) itrJasper.next();
                if (reqFmtExt.equalsIgnoreCase(gimpFmtStr)) {
                    // select the gimp supported format
                    fmtStr = gimpFmtStr;
                    fmtFound = true;
                    break;
                }
                if (fmtFound) {
                    break;
                }
            }
        }
        return fmtStr;
    }

    /**
     * Disambiguation (e.g. TIFF -> TIF) according to the formatMapping datas
     * structure defined in this class.
     * @param ext
     * @return Uppercase disambiguized extension string
     */
    private String normalizeExt(String ext) {
        String normExt = ext.toUpperCase();
        return ((formatMapping.containsKey(normExt)) ? (String) formatMapping.get(normExt) : normExt);
    }

    /**
     * @see eu.planets_project.services.migrate.Migrate#describe()
     */
    public ServiceDescription describe() {
        init();
        initParameters();
        List<Parameter> parameters = serviceParametersList;
        ServiceDescription.Builder builder = new ServiceDescription.Builder(
                NAME, Migrate.class.getName());
        builder.author("Sven Schlarb <shsschlarb-planets@yahoo.de>");
        builder.classname(this.getClass().getCanonicalName());
        builder.description("Kakadu Version 6.2.1 JPEG2000 sample command line" +
                "application 'kdu_compress' which shows the potential of the " +
                "JPEG 2000 Developers' Toolkit. The service uses the 'kdu_compress' " +
                "command line tool. This command line application uses a simple " +
                "TIFF file reader which can only read uncompressed TIFF files.  " +
                "This has nothing to do with Kakadu itself. In order to be able to " +
                "read compressed TIFF files, it should be sufficient " +
                "to re-compile the command line application with the symbol " +
                "KDU_INCLUDE_TIFF defined, and link it against the public-domain " +
                "LIBTIFF library. Furthermore, it is possible to compress various" +
                "inputfiles into one jpeg2000 file. But this service only supports" +
                "the one tif input file to one jp2 output file migration. " +
                "IMPORTANT: " +
                "The license conditions for this web service apply as if you" +
                "were downloading and using the kakadu executables locally. " +
                "KAKADU LICENSE NOTE: " +
                "Copyright is owned by NewSouth Innovations Proprietary Ltd, " +
                "commercial arm of the University of New South Wales, Sydney, " +
                "Australia. You are free to play around with these executables " +
                "and even to re-distribute them, so long as such use or " +
                "re-distribution is accompanied this copyright notice and is " +
                "not for commercial gain. Note: Binaries can only be used " +
                "for non-commercial purposes. If in doubt please contact " +
                "Dr. Taubman." +
                "Notice: Using this web service, you are acknowledging and " +
                "accepting these terms and conditions. ");
        FormatRegistry registry = FormatRegistryFactory.getFormatRegistry();

        MigrationPath[] mPaths = new MigrationPath[]{
            new MigrationPath(registry.createExtensionUri("tif"),
            registry.createExtensionUri("jp2"), null),
            new MigrationPath(registry.createExtensionUri("raw"),
            registry.createExtensionUri("jp2"), null),
            new MigrationPath(registry.createExtensionUri("bmp"),
            registry.createExtensionUri("jp2"), null),
            new MigrationPath(registry.createExtensionUri("pbm"),
            registry.createExtensionUri("jp2"), null),
            new MigrationPath(registry.createExtensionUri("pgm"),
            registry.createExtensionUri("jp2"), null),
            new MigrationPath(registry.createExtensionUri("ppm"),
            registry.createExtensionUri("jp2"), null)
        };
        builder.paths(mPaths);
        builder.classname(this.getClass().getCanonicalName());
        builder.version("0.1");
        builder.parameters(parameters);
        ServiceDescription mds = builder.build();

        return mds;
    }
}
