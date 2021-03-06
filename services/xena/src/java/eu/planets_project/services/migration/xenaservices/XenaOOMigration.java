package eu.planets_project.services.migration.xenaservices;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebService;

import org.apache.commons.io.IOUtils;

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
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.ServiceUtils;

/**
 * @author Georg Petz <georg.petz@onb.ac.at> *
 */
@Local(Migrate.class)
@Remote(Migrate.class)
@Stateless
@WebService(name = XenaOOMigration.NAME, serviceName = Migrate.NAME,
targetNamespace = PlanetsServices.NS,
endpointInterface = "eu.planets_project.services.migrate.Migrate")
public class XenaOOMigration implements Migrate, Serializable {
    private static final FormatRegistry format = FormatRegistryFactory
            .getFormatRegistry();
    private static final URI PRONOMPDFAID = format.createPronomUri("fmt/95");
    private static final URI PRONOMPDFID = format.createPronomUri("fmt/18");
    

    /**
     * all suported odf file formats
     */
    public enum OdfFormat {

        ODT, ODS, ODG, ODF;

        public static OdfFormat toOddFormat(String str) {
            try {
                return valueOf(str);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    };

    /**
     * all supported ms office file formats
     */
    public enum MSOfficeFormat {

        DOC, XLS;

        public static MSOfficeFormat toMSOfficeFormat(String str) {
            try {
                return valueOf(str);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    };
    private static Logger log = Logger.getLogger(XenaOOMigration.class.getName());
    private static final long serialVersionUID = 3952711367037433051L;
    static final String NAME = "XenaOOMigration";

    /**
     * checks if the odf format exists in the registry
     * @param formatUri the format uri for checking
     * @return the odf format if it exists, else null
     */
    private OdfFormat getOdfFormatExt(URI formatUri) {
        Set<String> reqInputFormatExts = format.getExtensions(formatUri);
        OdfFormat ext = null;

        for (String inFormat : reqInputFormatExts) {
            ext = OdfFormat.toOddFormat(inFormat.toUpperCase());
        }

        if (ext != null) {
            return ext;
        } else {
            return null;
        }
    }

    /**
     * checks if the ms office format exists in the registry
     * @param formatUri the format uri for checking
     * @return the ms office format if it exists, else null
     */
    private MSOfficeFormat getMsOfficeFormatExt(URI formatUri) {
        Set<String> reqInputFormatExts = format.getExtensions(formatUri);
        MSOfficeFormat ext = null;

        for (String inFormat : reqInputFormatExts) {

            ext = MSOfficeFormat.toMSOfficeFormat(inFormat.toUpperCase());
        }

        if (ext != null) {
            return ext;
        } else {
            return null;
        }
    }

//    private boolean isFormatExt(URI formatURI, String ext) {
//        FormatRegistryImpl fmtRegImpl = new FormatRegistryImpl();
//        Format uriFormatObj = fmtRegImpl.getFormatForURI(formatURI);
//        Set<String> reqInputFormatExts = uriFormatObj.getExtensions();
//
//        for (String inFormat : reqInputFormatExts) {
//            if (inFormat.equals(ext)) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     *
     * @param digitalObject
     * @param inputFormat
     * @param outputFormat
     * @param parameters
     * @return
     */
    public MigrateResult migrate(DigitalObject digitalObject, URI inputFormat, URI outputFormat, List<Parameter> parameters) {
        InputStream inputStream = digitalObject.getContent().getInputStream();
        XenaOOMigrations xena = new XenaOOMigrations();

        OdfFormat odfExtInput = getOdfFormatExt(inputFormat);
        MSOfficeFormat msOfficeExtInput = getMsOfficeFormatExt(inputFormat);

        //OO Input
        if (odfExtInput != null) {
            xena.setOoffice_import_filter(XenaOOMigrations.IMPORT_FILTER_NONE);

            System.out.print(odfExtInput.toString() + " to ");

        } //MS Input
        else if (msOfficeExtInput != null) {

            System.out.print(msOfficeExtInput.toString() + " to ");

            switch (msOfficeExtInput) {
                case DOC:
                    xena.setOoffice_import_filter(XenaOOMigrations.IMPORT_FILTER_DOC);

                case XLS:
                    xena.setOoffice_import_filter(XenaOOMigrations.IMPORT_FILTER_NONE);
            }
        }

        if (outputFormat.equals(PRONOMPDFAID)) {

            System.out.println("pdfa");

            xena.setOoffice_export_filter(XenaOOMigrations.EXPORT_FILTER_PDF);
            xena.setPdfa(true);

        } else if (outputFormat.equals(PRONOMPDFID)) {

            System.out.println("pdf");

            xena.setOoffice_export_filter(XenaOOMigrations.EXPORT_FILTER_PDF);
            xena.setPdfa(false);
        }

        byte[] binary = null;
        try {
            binary = xena.migrate(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Sanity check the result:
        if( binary == null ) {
            return new MigrateResult(null, ServiceUtils.createErrorReport("OpenOffice invocation failed. No output file was created."));
        }

        DigitalObject newDO = null;
        ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS, "OK");
        newDO =
                new DigitalObject.Builder(Content.byValue(binary)).build();
        return new MigrateResult(newDO, report);

    }

    /**
     * 
     * @return
     */
    public ServiceDescription describe() {
        ServiceDescription.Builder builder = new ServiceDescription.Builder(NAME, Migrate.class.getName());

        builder.author(
                "Georg Petz <georg.petz@onb.ac.at");
        builder.classname(
                this.getClass().getCanonicalName());
        builder.description(
                "XENA OO Wrapper");

        ArrayList<MigrationPath> mPathsList = new ArrayList<MigrationPath>();


        //from supported odf formats -> PDF (1.4)
        for (OdfFormat odfExt : OdfFormat.values()) {
            mPathsList.add(new MigrationPath(format.createExtensionUri(odfExt.toString()), format.createPronomUri("fmt/18"), null));
        }

        //from supported odf formats -> Portable Document Format - Archival
        for (OdfFormat odfExt : OdfFormat.values()) {
            mPathsList.add(new MigrationPath(format.createExtensionUri(odfExt.toString()), format.createPronomUri("fmt/95"), null));
        }

        //from supported MS Office formats -> Portable Document Format - Archival
        for (MSOfficeFormat msOfficeExt : MSOfficeFormat.values()) {
            mPathsList.add(new MigrationPath(format.createExtensionUri(msOfficeExt.toString()), format.createPronomUri("fmt/95"), null));
        }

        //from supported MS Office formats ->PDF (1.4)
        for (MSOfficeFormat msOfficeExt : MSOfficeFormat.values()) {
            mPathsList.add(new MigrationPath(format.createExtensionUri(msOfficeExt.toString()), format.createPronomUri("fmt/18"), null));
        }

//        mPathsList.add(new MigrationPath(Format.extensionToURI("doc"), Format.extensionToURI("odt"), null));

        builder.paths(mPathsList.toArray(new MigrationPath[mPathsList.size()]));

        builder.classname(
                this.getClass().getCanonicalName());
        builder.version(
                "0.2");

        ServiceDescription mds = builder.build();

        return mds;
    }
}
