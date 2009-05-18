/*
 *
 */
package eu.planets_project.services.migration.abiword;

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
 * The AbiWordMigration migrates JPEG files to JP2 files and vice versa.
 * @author Sven Schlarb <shsschlarb-planets@yahoo.de>
 */
@Local(Migrate.class)
@Remote(Migrate.class)
@Stateless
@WebService(name = AbiWordMigration.NAME, serviceName = Migrate.NAME, targetNamespace = PlanetsServices.NS, endpointInterface = "eu.planets_project.services.migrate.Migrate")
public final class AbiWordMigration implements Migrate {

    PlanetsLogger log = PlanetsLogger.getLogger(AbiWordMigration.class);

    /** The dvi ps installation dir */
    public String abiword_install_dir;
    /** The abiword application name */
    public String abiword_app_name;
    /** The output file extension */
    // public String abiword_outfile_ext;
    private File tmpInFile;
    private File tmpOutFile;

    String inputFmtExt = null;
    String outputFmtExt = null;

    /***/
    static final String NAME = "AbiWordMigration";

    List<String> inputFormats = null;
    List<String> outputFormats = null;
    HashMap<String, String> formatMapping = null;

    /***/
    private static final long serialVersionUID = 2127494848765937613L;

    private void init() {
        // input formats
        inputFormats = new ArrayList<String>();
        inputFormats.add("doc");
        inputFormats.add("html");
        inputFormats.add("pdf");
        inputFormats.add("rtf");
        inputFormats.add("txt");
        // inputFormats.add("odt"); // Not sure that odt works on ubuntu (bug)

        // output formats and associated output parameters
        outputFormats = new ArrayList<String>();
        outputFormats.add("doc");
        outputFormats.add("html");
        outputFormats.add("pdf");
        outputFormats.add("rtf");
        outputFormats.add("txt");
        // inputFormats.add("odt"); // Not sure that odt works on ubuntu (bug)

        // Disambiguation of extensions, e.g. {"JPG","JPEG"} to {"JPEG"}
        // FIXIT This should be supported by the FormatRegistryImpl class, but
        // it does not provide the complete set at the moment.
        formatMapping = new HashMap<String, String>();
        formatMapping.put("htm", "html");
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.services.migrate.Migrate#migrate(eu.planets_project.services.datatypes.DigitalObject,
     *      java.net.URI, java.net.URI,
     *      eu.planets_project.services.datatypes.Parameter)
     */
    public MigrateResult migrate(final DigitalObject digitalObject,
            URI inputFormat, URI outputFormat, List<Parameter> parameters) {

        Properties props = new Properties();
        try {

            String strRsc = "/eu/planets_project/services/migration/abiword/abiword.properties";
            props.load(this.getClass().getResourceAsStream(strRsc));
            // config vars
            this.abiword_install_dir = props.getProperty("abiword.install.dir");
            this.abiword_app_name = props.getProperty("abiword.app.name");

        } catch (Exception e) {
            // // config vars
            this.abiword_install_dir = "/usr/bin";
            this.abiword_app_name = "abiword";
        }
        log
                .info("Using abiword install directory: "
                        + this.abiword_install_dir);
        log.info("Using abiword application name: " + this.abiword_app_name);

        init();
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
            log
                    .error("[AbiWordMigration] Unable to create temporary input file!");
            return null;
        }
        log.info("[AbiWordMigration] Temporary input file created: "
                + tmpInFile.getAbsolutePath());

        // outfile name
        String outFileStr = tmpInFile.getAbsolutePath() + "." + outputFmtExt;
        log.info("[AbiWordMigration] Output file name: " + outFileStr);

        // run command
        ProcessRunner runner = new ProcessRunner();
        List<String> command = new ArrayList<String>();
        // setting up command
        // Example: abiword --input testin.jpeg --input-format jpg
        // --output-format jp2 --output testout.jp2
        // Example (short version): abiword -f testin.jpg -t jpg -F testin.jp2
        // -T jp2
        command.add(this.abiword_app_name);
        command.add("--to=" + outFileStr);
        command.add(tmpInFile.getAbsolutePath());
        runner.setCommand(command);
        runner.setInputStream(inputStream);
        log.info("[AbiWordMigration] Executing command: " + command.toString()
                + " ...");
        runner.run();
        int return_code = runner.getReturnCode();
        if (return_code != 0) {
            log.error("[AbiWordMigration] Jasper conversion error code: "
                    + Integer.toString(return_code));
            log.error("[AbiWordMigration] " + runner.getProcessErrorAsString());
            // log.error("[AbiWordMigration] Output: "+runner.getProcessOutputAsString());
            return null;
        }

        tmpOutFile = new File(outFileStr);
        ServiceReport report;
        // read byte array from temporary file
        if (tmpOutFile.isFile() && tmpOutFile.canRead()) {
            binary = FileUtils.readFileIntoByteArray(tmpOutFile);
            report = new ServiceReport(Type.INFO, Status.SUCCESS, "Wrote: "
                    + tmpOutFile);
        } else {
            String message = "Error: Unable to read temporary file "
                    + tmpOutFile.getAbsolutePath();
            log.error(message);
            report = new ServiceReport(Type.ERROR, Status.INSTALLATION_ERROR,
                    message);
        }

        DigitalObject newDO = null;

        newDO = new DigitalObject.Builder(Content.byValue(binary))
                .build();

        return new MigrateResult(newDO, report);
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
     * format of the set of abiword's supported input/output formats. If
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
        Set<String> reqInputFormatExts = FormatRegistryFactory
                .getFormatRegistry().getExtensions(formatUri);
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
                // Iterate over the formats that abiword offers either as input
                // or
                // as output format.
                // See input formats in the this.init() method to see the
                // abiword input/output formats offered by this service.
                String gimpFmtStr = (String) itrJasper.next();
                if (reqFmtExt.equalsIgnoreCase(gimpFmtStr)) {
                    // select the gimp supported format
                    fmtStr = gimpFmtStr;
                    fmtFound = true;
                    break;
                }
                if (fmtFound)
                    break;
            }
        }
        return fmtStr;
    }

    /**
     * Disambiguation (e.g. JPG -> JPEG) according to the formatMapping datas
     * structure defined in this class.
     * @param ext
     * @return Uppercase disambiguized extension string
     */
    private String normalizeExt(String ext) {
        String normExt = ext.toUpperCase();
        return ((formatMapping.containsKey(normExt)) ? (String) formatMapping
                .get(normExt) : normExt);
    }

    /**
     * @see eu.planets_project.services.migrate.Migrate#describe()
     */
    public ServiceDescription describe() {
        ServiceDescription.Builder builder = new ServiceDescription.Builder(
                NAME, Migrate.class.getName());

        builder.author("Sven Schlarb <shsschlarb-planets@yahoo.de>");
        builder.classname(this.getClass().getCanonicalName());
        builder
                .description("This service provides Abiword (Version: GNOME AbiWord-2.4 2.4.6) "
                        + "document conversions between the formats doc, html, pdf, rtf, txt, odt "
                        + "(all directions).\n\n"
                        + "The OpenOffice conversion (odt) has been left out in this implementation because"
                        + "of a bug in the Ubuntu Linux distribution (exporter for odt does not work):\n"
                        + "https://bugs.launchpad.net/ubuntu/+source/abiword/+bug/24195\n"
                        + "This functionality can be easily added afterwards. With Abiword, you can write "
                        + "additional so called exporters which add new migration functionality. "
                        + "Therefore, this service is extensible in the functionality it offers.");
        FormatRegistry registry = FormatRegistryFactory.getFormatRegistry();
        MigrationPath[] mPaths = new MigrationPath[] {
                new MigrationPath(registry.createExtensionUri("doc"), registry
                        .createExtensionUri("html"), null),
                new MigrationPath(registry.createExtensionUri("doc"), registry
                        .createExtensionUri("pdf"), null),
                new MigrationPath(registry.createExtensionUri("doc"), registry
                        .createExtensionUri("rtf"), null),
                new MigrationPath(registry.createExtensionUri("doc"), registry
                        .createExtensionUri("txt"), null),
                // new MigrationPath(Format.extensionToURI("doc"),
                // Format.extensionToURI("odt"),null), // Not sure that odt
                // works on ubuntu (bug)
                new MigrationPath(registry.createExtensionUri("html"), registry
                        .createExtensionUri("doc"), null),
                new MigrationPath(registry.createExtensionUri("html"), registry
                        .createExtensionUri("pdf"), null),
                new MigrationPath(registry.createExtensionUri("html"), registry
                        .createExtensionUri("rtf"), null),
                new MigrationPath(registry.createExtensionUri("html"), registry
                        .createExtensionUri("txt"), null),
                // new MigrationPath(Format.extensionToURI("html"),
                // Format.extensionToURI("odt"),null), // Not sure that odt
                // works on ubuntu (bug)
                new MigrationPath(registry.createExtensionUri("pdf"), registry
                        .createExtensionUri("doc"), null),
                new MigrationPath(registry.createExtensionUri("pdf"), registry
                        .createExtensionUri("html"), null),
                new MigrationPath(registry.createExtensionUri("pdf"), registry
                        .createExtensionUri("rtf"), null),
                new MigrationPath(registry.createExtensionUri("pdf"), registry
                        .createExtensionUri("txt"), null),
                // new MigrationPath(Format.extensionToURI("pdf"),
                // Format.extensionToURI("odt"),null), // Not sure that odt
                // works on ubuntu (bug)
                new MigrationPath(registry.createExtensionUri("rtf"), registry
                        .createExtensionUri("doc"), null),
                new MigrationPath(registry.createExtensionUri("rtf"), registry
                        .createExtensionUri("html"), null),
                new MigrationPath(registry.createExtensionUri("rtf"), registry
                        .createExtensionUri("pdf"), null),
                new MigrationPath(registry.createExtensionUri("rtf"), registry
                        .createExtensionUri("txt"), null),
                // new MigrationPath(Format.extensionToURI("rtf"),
                // Format.extensionToURI("odt"),null), // Not sure that odt
                // works on ubuntu (bug)
                new MigrationPath(registry.createExtensionUri("txt"), registry
                        .createExtensionUri("doc"), null),
                new MigrationPath(registry.createExtensionUri("txt"), registry
                        .createExtensionUri("html"), null),
                new MigrationPath(registry.createExtensionUri("txt"), registry
                        .createExtensionUri("pdf"), null),
                new MigrationPath(registry.createExtensionUri("txt"), registry
                        .createExtensionUri("rtf"), null) /*
                                                       * , new
                                                       * MigrationPath(Format
                                                       * .extensionToURI("txt"),
                                                       * Format
                                                       * .extensionToURI("odt"
                                                       * ),null), new
                                                       * MigrationPath
                                                       * (Format.extensionToURI
                                                       * ("odt"),
                                                       * Format.extensionToURI
                                                       * ("doc"),null), new
                                                       * MigrationPath
                                                       * (Format.extensionToURI
                                                       * ("odt"),
                                                       * Format.extensionToURI
                                                       * ("html"),null), new
                                                       * MigrationPath
                                                       * (Format.extensionToURI
                                                       * ("odt"),
                                                       * Format.extensionToURI
                                                       * ("pdf"),null), new
                                                       * MigrationPath
                                                       * (Format.extensionToURI
                                                       * ("odt"),
                                                       * Format.extensionToURI
                                                       * ("rtf"),null), new
                                                       * MigrationPath
                                                       * (Format.extensionToURI
                                                       * ("odt"),
                                                       * Format.extensionToURI
                                                       * ("txt"),null),
                                                       */// Not sure that odt
                                                         // works on ubuntu
                                                         // (bug)
        };
        builder.paths(mPaths);
        builder.classname(this.getClass().getCanonicalName());
        builder.version("0.1");

        ServiceDescription mds = builder.build();

        return mds;
    }
}
