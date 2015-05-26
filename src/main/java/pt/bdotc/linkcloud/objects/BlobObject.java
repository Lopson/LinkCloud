package pt.bdotc.linkcloud.objects;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import javax.ejb.Stateless;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

/**
 * Created by Lopson on 23/05/2015.
 *
 * Download blob:
 * https://community.rackspace.com/developers/f/7/t/3563
 *
 * Blob upload and container creation:
 * https://jclouds.apache.org/start/blobstore/
 *
 * Random Code:
 *  BlobMetadata metadata = cloudStorage.blobMetadata(container, blobName);
 *  Long blobSize= metadata.getContentMetadata().getContentLength();
 *
 * Azure SDK:
 *  http://dl.windowsazure.com/storage/javadoc/
 *  http://azure.microsoft.com/en-us/documentation/articles/storage-java-how-to-use-blob-storage/
 *  http://blogs.msdn.com/b/jmstall/archive/2014/06/12/azure-storage-naming-rules.aspx
 */
@Stateless
public class BlobObject
{
    private static CloudStorageAccount
    initStorageConnection(String username, String password)
    throws BadRequestException, InternalServerErrorException
    {
    // Validate username and build connection string
        /* Azure SDK doesn't validate username for us. */
        if(!username.matches("[a-z1-9]+")) {throw new BadRequestException("Invalid container name");}
        final String storageConnectionString= "DefaultEndpointsProtocol=https;" +
                                              "AccountName=" + username + ";" +
                                              "AccountKey=" + password;

        try
        {
            return CloudStorageAccount.parse(storageConnectionString);
        }
        catch(InvalidKeyException keyException)
        {
        // Key is malformed, hence the bad request code
            throw new BadRequestException("Bad key given");
        }
        catch(URISyntaxException badURIException)
        {
        // Bad storageConnectionString, should never happen
            throw new InternalServerErrorException("Error parsing connection string");
        }
    }

    private static CloudBlobContainer
    initBlobContainer(CloudBlobClient blobClient, String containerName)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
        try
        {
            return blobClient.getContainerReference(containerName);
        }
        catch(StorageException containerError)
        {
            int httpStatusCode= containerError.getHttpStatusCode();

            if     (httpStatusCode== 400) {throw new BadRequestException("Invalid container name " + containerName);}
            else if(httpStatusCode== 404) {throw new NotFoundException("Container" + containerName + "not found");}
            else                          {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch(URISyntaxException error)
        {
            throw new InternalServerErrorException("Error encountered when parsing container " + containerName);
        }
    }

    @SuppressWarnings("unused")
    public static InputStream
    downloadBlob(String provider, String containerName, String blobName, String username, String password)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
        CloudStorageAccount azureAccount= initStorageConnection(username, password);
        CloudBlobClient blobClient = azureAccount.createCloudBlobClient();
        CloudBlobContainer container = initBlobContainer(blobClient, containerName);

        try
        {
            CloudBlockBlob blockBlob= container.getBlockBlobReference(blobName);
            return blockBlob.openInputStream();
        }
        catch (StorageException blobError)
        {
            int httpStatusCode= blobError.getHttpStatusCode();

            if     (httpStatusCode== 400) {throw new BadRequestException("Invalid blob name " + blobName);}
            else if(httpStatusCode== 404) {throw new NotFoundException("Blob " + blobName + " not found");}
            else                          {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch (URISyntaxException e)
        {
            throw new InternalServerErrorException("Error encountered when parsing blob " + blobName);
        }
    }

    @SuppressWarnings("unused")
    public static void
    uploadBlob(String provider, String containerName, String blobName, String username, String password,
               InputStream blobContents, long blobSize)
    {
        CloudStorageAccount azureAccount= initStorageConnection(username, password);
        CloudBlobClient blobClient = azureAccount.createCloudBlobClient();
        CloudBlobContainer container = initBlobContainer(blobClient, containerName);


    }

    // container.createIfNotExists();
}