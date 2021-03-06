<%@ page
  import="java.io.*,java.net.URLDecoder,javax.activation.MimetypesFileTypeMap,eu.planets_project.services.datatypes.DigitalObject,eu.planets_project.ifr.core.storage.utils.DigitalObjectDiskCache"
%><% 

// Pick up the parameters:
String sid = request.getParameter("sid");

// Decode the file name (might contain spaces and on) and prepare file object.
sid = URLDecoder.decode(sid, "UTF-8");

// Open the file:
DigitalObject f = DigitalObjectDiskCache.recoverDigitalObject(sid);

// Does this DOB exist?
if( f != null ) {
    response.setContentType( "application/octet-stream" );
  response.setHeader("Content-Disposition","attachment;filename=floppy.img");

    // Now stream out the data:
    DataInputStream in = new DataInputStream(f.getContent().read());
    ServletOutputStream op = response.getOutputStream();
    byte[] bbuf = new byte[2*1024];
    int length = 0;

    try {
        while ((in != null) && ((length = in.read(bbuf)) != -1))
            {
            op.write(bbuf,0,length);
            }
    } finally {
            in.close();
            op.flush();
            op.close();
    }

}
%>
