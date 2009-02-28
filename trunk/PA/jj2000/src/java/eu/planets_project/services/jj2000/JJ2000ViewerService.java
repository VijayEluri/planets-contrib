/**
 * 
 */
package eu.planets_project.services.jj2000;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jws.WebService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.view.CreateView;
import eu.planets_project.services.view.CreateViewResult;
import eu.planets_project.services.view.ViewStatus;

/**
 * A viewer service based on the JJ2000 code.
 * 
 * Caches the digital object locally and manages viewer sessions for them.
 * 
 * @author <a href="mailto:Andrew.Jackson@bl.uk">Andy Jackson</a>
 *
 */

@WebService(name = JJ2000ViewerService.NAME, 
        serviceName = CreateView.NAME, 
        targetNamespace = PlanetsServices.NS,
        endpointInterface = "eu.planets_project.services.view.CreateView" )
        
public class JJ2000ViewerService implements CreateView {

    /** The name of the service */
    public static final String NAME="JJ2000 Viewer Service";

    /** The default context path */
    private static final String CONTEXT_PATH = "/pserv-pa-jj2000/";

    /** A logger */
    private static Log log = LogFactory.getLog(JJ2000ViewerService.class);

    /* (non-Javadoc)
     * @see eu.planets_project.services.view.CreateView#describe()
     */
    public ServiceDescription describe() {
        ServiceDescription.Builder mds = new ServiceDescription.Builder(NAME, CreateView.class.getCanonicalName());
        mds.description("A JPEG 2000 viewer service. Uses the JJ2000 reference implementation. See http://jj2000.epfl.ch/ for copyright information.");
        mds.author("Andrew Jackson <Andrew.Jackson@bl.uk>");
        mds.classname(this.getClass().getCanonicalName());
        // Add a link to the JJ2000 homepage.
        try {
            mds.furtherInfo(new URI("http://jj2000.epfl.ch/"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return mds.build();
    }

    private static CreateViewResult returnWithErrorMessage(String message) {
        ServiceReport rep = new ServiceReport();
        log.error(message);
        rep.setErrorState(ServiceReport.ERROR);
        rep.setError("message");
        return new CreateViewResult(null, null, rep);
    }

    /* (non-Javadoc)
     * @see eu.planets_project.services.view.CreateView#createView(java.util.List)
     */
    public CreateViewResult createView(List<DigitalObject> digitalObjects) {
        return createViewerSession( digitalObjects );
    }

    /**
     * 
     * @param digitalObjects
     * @return
     */
    public static CreateViewResult createViewerSession(List<DigitalObject> digitalObjects) {
        // Store copies of the viewable digital objects:
        for( DigitalObject dob : digitalObjects ) {
            // Can only cope if the object is 'simple':
            if( dob.getContained() != null ) {
                return returnWithErrorMessage("The Content of the DigitalObject should not be NULL.");
            }
        }
        
        String sessionID = cacheDigitalObjects(digitalObjects);
        URL sessionURL;
        try {
            sessionURL = new URL(CONTEXT_PATH+"view.jsp?sid="+sessionID);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return returnWithErrorMessage("Failed to construct session URL.");
        }
        
        // Create a service report:
        ServiceReport rep = new ServiceReport();
        rep.setErrorState(ServiceReport.SUCCESS);

        // Return the view id:
        return new CreateViewResult(sessionURL, sessionID, rep);
    }

    /* (non-Javadoc)
     * @see eu.planets_project.services.view.CreateView#getViewStatus(java.lang.String)
     */
    public ViewStatus getViewStatus(String sessionIdentifier) {
        // Lookup this cache:
        File cache = findCacheDir( sessionIdentifier );
        
        // Default to 'inactive'
        ViewStatus vs = new ViewStatus( ViewStatus.INACTIVE, null );
        
        // If it's active, return info:
        if( cache != null && cache.exists() && cache.isDirectory() ) {
            vs = new ViewStatus( ViewStatus.ACTIVE, null );
        }
        return vs;
    }
    
    
    /* -- Cache Management --- */
    
    private static File findCacheDir( String sessionId ) {
        // For security reasons, do not allow directory separators:
        if( sessionId.contains("/") ) return null;
        if( sessionId.contains("\\") ) return null;
        return new File(System.getProperty("java.io.tmpdir"), "pserv-pa-jj2000/"+sessionId);
    }

    /**
     * @param digitalObjects
     * @return
     */
    private static String cacheDigitalObjects( List<DigitalObject> digitalObjects ) {
        // Generate a UUID to act as the session ID:
        String sessionId = UUID.randomUUID().toString();
        
        // Create a directory in the temp space, and store the DOs in there.
        File cachedir = findCacheDir( sessionId );
        cachedir.mkdir();
        
        log.info("Created cache dir: " + cachedir.getAbsolutePath() );
        
        // Store Digital Objects:
        
        return sessionId;
    }

    /**
     * @param sessionId
     * @return
     */
    public static List<DigitalObject> recoverDigitalObjects( String sessionId ) {
        List<DigitalObject> dobs = new ArrayList<DigitalObject>();
        // Lookup stored items:
        
        // Parse back into DigObjects:
        
        return dobs;
    }

    /**
     * @param sessionId
     * @param i
     * @return
     */
    public static DigitalObject findCachedDigitalObject( String sessionId, int i ) {
        List<DigitalObject> digitalObjects = recoverDigitalObjects(sessionId);
        if( digitalObjects == null ) return null;
        if( digitalObjects.size() == 0 ) return null;
        // Range check:
        if( i < 0 ) i = 0;
        if( i >= digitalObjects.size() ) i = 0;
        // Return:
        return digitalObjects.get(i);
    }
    
}
