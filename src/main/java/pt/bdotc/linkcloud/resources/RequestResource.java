package pt.bdotc.linkcloud.resources;

import pt.bdotc.linkcloud.objects.AzureStorageObject;
import pt.bdotc.linkcloud.objects.StorageObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    /** Hash Map that contains the StorageObject implementations for all supported CSPs. */
    private static final HashMap<String, StorageObject> providersSet=  new HashMap<>();
    static
    {
        providersSet.put("azureblob", new AzureStorageObject());
    }

    /**
     * Validates the Authorization HTTP header of the client and retrieve username and password from it. Also checks if
     * the CSP the user is trying to access is valid, i.e., it's a supported CSP.
     *
     * @param headers The HTTP headers of the client's request.
     * @param provider The CSP the user wants to access.
     * @return A list of {@code String} of 2 elements with the username at position 0 and the password at position 1.
     * @throws ForbiddenException Thrown when there's no Authorization header in the client's request.
     * @throws BadRequestException Thrown when the Authorization header is not of the {@code Basic} type or if it
     *         doesn't have a username, a password or both.
     * @throws NotSupportedException Thrown when the user is trying to use an unsupported CSP.
     */
    private String[]
    getCredentialsValidateCSP(HttpHeaders headers, String provider)
    throws ForbiddenException, BadRequestException, NotSupportedException
    {
    // Get Authorization HTTP header
        List<String> authHeadersList= headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if(authHeadersList.isEmpty()) {throw new ForbiddenException();}
        String authHeader= authHeadersList.get(0);

    // Make sure authorization is of the Basic type
        String authType= authHeader.split(" ")[0];
        if(!authType.equals("Basic")) {throw new BadRequestException();}

    // Decode header and get username and password from it
        String authDigest= authHeader.substring("Basic ".length());
        java.util.Base64.Decoder decoder= java.util.Base64.getDecoder();
        String[] result= new String(decoder.decode(authDigest)).split(":");
        if(result.length!= 2 || result[0].equals("") || result[1].equals("")) {throw new BadRequestException();}

    // Make sure client is using valid CSP
        if(!providersSet.containsKey(provider)) {throw new NotSupportedException();}

        return result;
    }

    /**
     * A {@code GET} HTTP request for the download of a blob. Its path is {@code "{provider}/{container}/{blob}"} and
     * it returns an {@code application/octet_stream}.
     *
     * @param headers The HTTP headers of the client's request.
     * @param provider The provider the client's trying to access.
     * @param container The container in which the blob to download resides.
     * @param blob The name of the blob to download.
     * @return An {@link InputStream} with the contents of the blob that's being downloaded.
     * @throws ForbiddenException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws BadRequestException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws NotSupportedException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws NotFoundException See the StorageObject classes implemented.
     * @throws InternalServerErrorException See the StorageObject classes implemented.
     */
    @GET
    @Path("{provider}/{container}/{blob}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream
    getBlob(@Context                HttpHeaders headers,
            @PathParam("provider")  String provider,
            @PathParam("container") String container,
            @PathParam("blob")      String blob)
    throws ForbiddenException, BadRequestException, NotSupportedException, NotFoundException,
           InternalServerErrorException
    {
    // Get username and password from HTTP AUTHORIZATION header
        String[] credentials= getCredentialsValidateCSP(headers, provider);
        String username= credentials[0];
        String password= credentials[1];

    // Perform download of blob and return it to client
        return providersSet.get(provider).downloadBlob(container, blob, username, password);
    }

    /**
     * A {@code POST} HTTP request for the upload of a blob. Its path is
     * {@code "{provider}/{container}/{blob}?size=number_of_bytes"}, consuming an {@code application/octet_stream}, and
     * it returns a 200 HTTP code if the upload was successful.
     *
     * The reason why the program's not using a multipart form is because this program's meant exclusively for
     * programmed interaction, that is, it's not meant to be used with a web page alongside it. Changing this to a
     * multipart form shouldn't be too hard though. In that case, I recommend putting the blob's size as a field of the
     * form instead of a query parameter in the URL.
     *
     * @param headers The HTTP headers of the client's request.
     * @param provider The CSP the user wants to use.
     * @param container The container into which the blob is to be uploaded.
     * @param blob The name of the blob.
     * @param size The size of the blob. A parameter in the URL of the client's request.
     * @param content The contents to be uploaded.
     * @return A 200 HTTP code in case of success.
     * @throws ForbiddenException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws BadRequestException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws NotSupportedException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     */
    @POST
    @Path("{provider}/{container}/{blob}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response
    putBlob(@Context                    HttpHeaders headers,
            @PathParam("provider")      String provider,
            @PathParam("container")     String container,
            @PathParam("blob")          String blob,
            @QueryParam("size")         long size,
            InputStream content)
    throws ForbiddenException, BadRequestException, NotSupportedException
    {
    // Get username and password from HTTP AUTHORIZATION header
        String[] credentials= getCredentialsValidateCSP(headers, provider);
        String username= credentials[0];
        String password= credentials[1];

    // Get blob size and try to upload blob
        providersSet.get(provider).uploadBlob(container, blob, username, password, content, size);
        return Response.ok().build();
    }

    /**
     * A {@code GET} HTTP request that returns an XML file listing all of the blobs inside a container.
     *
     * @param headers The HTTP headers of the client's request.
     * @param provider The CSP that's to be accessed.
     * @param container The container that's to be listed.
     * @return An {@link InputStream} with the XML file.
     * @throws ForbiddenException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws BadRequestException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws NotSupportedException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws InternalServerErrorException See the StorageObject classes implemented.
     * @throws NotFoundException See the StorageObject classes implemented.
     */
    @GET
    @Path("{provider}/{container}")
    @Produces(MediaType.APPLICATION_XML)
    public InputStream
    listBlobs(@Context                    HttpHeaders headers,
              @PathParam("provider")      String provider,
              @PathParam("container")     String container)
    throws ForbiddenException, BadRequestException, NotSupportedException, InternalServerErrorException,
           NotFoundException
    {
    // Get username and password from HTTP AUTHORIZATION header
        String[] credentials= getCredentialsValidateCSP(headers, provider);
        String username= credentials[0];
        String password= credentials[1];

    // Get XML list and return it
        return providersSet.get(provider).listBlobs(container, username, password);
    }

    /**
     * A {@code DELETE} HTTP request for the deletion of a blob. If the blob doesn't exist, this method returns a 404
     * HTTP code.
     *
     * @param headers The HTTP headers of the client's request.
     * @param provider The CSP that's to be accessed.
     * @param container The container that's to be altered.
     * @param blob The blob that's to be deleted.
     * @return A 200 HTTP code in case of success.
     * @throws ForbiddenException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws BadRequestException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws NotSupportedException See {@code getCredentialsValidateCSP} and the StorageObject classes implemented.
     * @throws NotFoundException See the StorageObject classes implemented.
     * @throws InternalServerErrorException See the StorageObject classes implemented.
     */
    @DELETE
    @Path("{provider}/{container}/{blob}")
    public Response
    deleteBlob(@Context                HttpHeaders headers,
               @PathParam("provider")  String provider,
               @PathParam("container") String container,
               @PathParam("blob")      String blob)
    throws ForbiddenException, BadRequestException, NotSupportedException, NotFoundException,
           InternalServerErrorException
    {
    // Get username and password from HTTP AUTHORIZATION header
        String[] credentials= getCredentialsValidateCSP(headers, provider);
        String username= credentials[0];
        String password= credentials[1];

    // Perform deletion of blob
        providersSet.get(provider).deleteBlob(container, blob, username, password);
        return Response.ok().build();
    }
}
