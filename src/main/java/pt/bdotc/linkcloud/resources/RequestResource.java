package pt.bdotc.linkcloud.resources;

import pt.bdotc.linkcloud.objects.BlobObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.util.*;
import java.io.InputStream;

/**
 * Created by Lopson on 23/05/2015.
 *
 * JAX-RS examples:
 * https://gist.github.com/migmad/9ed2ba942a266d46914f
 * https://github.com/javaee-samples/javaee7-samples
 *
 * Return Codes HTTP:
 * http://stackoverflow.com/questions/4687271/jax-rs-how-to-return-json-and-http-status-code-together
 * http://docs.oracle.com/javaee/7/api/javax/ws/rs/package-summary.html
 *
 * Binary File Upload Multipart with Jersey 2:
 * https://github.com/aruld/jersey2-multipart-sample
 */

@Path("api")
public class RequestResource
{
    private static final String[] blobStoreProviders= {"azureblob"};
    private static final Set<String> providersSet= new HashSet<>(Arrays.asList(blobStoreProviders));

    private String[]
    getUsernamePassword(HttpHeaders headers)
    throws ForbiddenException, BadRequestException
    {
        List<String> authHeadersList= headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if(authHeadersList.isEmpty()) {throw new ForbiddenException();}
        String authHeader= authHeadersList.get(0);

        String authType= authHeader.split(" ")[0];
        if(!authType.equals("Basic")) {throw new BadRequestException();}

        String authDigest= authHeader.substring("Basic ".length());
        java.util.Base64.Decoder decoder= java.util.Base64.getDecoder();
        String[] result= new String(decoder.decode(authDigest)).split(":");
        if(result.length!= 2 || result[0].equals("") || result[1].equals("")) {throw new BadRequestException();}

        return result;
    }

    @GET
    @Path("{provider}/{container}/{blob}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream
    getBlob(@Context                HttpHeaders headers,
            @PathParam("provider")  String provider,
            @PathParam("container") String container,
            @PathParam("blob")      String blob)
    throws NotFoundException, ForbiddenException, InternalServerErrorException, NotSupportedException,
           BadRequestException
    {
    // Get username and password from HTTP AUTHORIZATION header
        String[] credentials= getUsernamePassword(headers); // Throws NotAuthorizedException, BadRequestException.
        String username= credentials[0];
        String password= credentials[1];

    // Make sure client is using valid CSP
        if(!providersSet.contains(provider)) {throw new NotSupportedException();}

    // Perform download of blob and return it to client
        /* Throws NotFoundException and InternalServerErrorException. */
        return BlobObject.downloadBlob(provider, container, blob, username, password);
    }

    @POST
    @Path("{provider}/{container}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void
    putBlob(@Context                    HttpHeaders headers,
            @PathParam("provider")      String provider,
            @PathParam("container")     String container,
            @FormDataParam("blob_id")   String blob,
            @FormDataParam("blob_data") InputStream blobContents,
            @FormDataParam("blob_data") FormDataContentDisposition blobDetails)
    throws NotAuthorizedException, NotSupportedException
    {
    // Get username and password from HTTP AUTHORIZATION header
        String[] credentials= getUsernamePassword(headers); // Throws NotAuthorizedException, BadRequestException.
        String username= credentials[0];
        String password= credentials[1];

    // Make sure client is using valid CSP
        if(!providersSet.contains(provider)) {throw new NotSupportedException();}

    // Get blob size and try to upload blob
        long blobSize= blobDetails.getSize();
        BlobObject.uploadBlob(provider, container, blob, username, password, blobContents, blobSize);
    }

    // Function to @GET a blob listing (return XML file)
    // Function to @DELETE blob
}
