package pt.bdotc.linkcloud.objects;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ejb.Stateless;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private static final String XMLROOT= "blobs";

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
    // Setup access to container
        CloudStorageAccount azureAccount= initStorageConnection(username, password);
        CloudBlobClient     blobClient= azureAccount.createCloudBlobClient();
        CloudBlobContainer  container= initBlobContainer(blobClient, containerName);

        try
        {
        // Download blob
            CloudBlockBlob blockBlob= container.getBlockBlobReference(blobName);
            return blockBlob.openInputStream();
        }
        catch (StorageException blobError)
        {
        // 400 for invalid name; 404 for missing; anything else is error
            int httpStatusCode= blobError.getHttpStatusCode();

            if     (httpStatusCode== 400) {throw new BadRequestException("Invalid blob name " + blobName);}
            else if(httpStatusCode== 404) {throw new NotFoundException("Blob " + blobName + " not found");}
            else                          {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch (URISyntaxException e)
        {
        // Should never happen
            throw new InternalServerErrorException("Error encountered when parsing blob " + blobName);
        }
    }

    @SuppressWarnings("unused")
    public static void
    uploadBlob(String provider, String containerName, String blobName, String username, String password,
               InputStream blobContents, long blobSize)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudStorageAccount azureAccount= initStorageConnection(username, password);
        CloudBlobClient     blobClient= azureAccount.createCloudBlobClient();
        CloudBlobContainer  container= initBlobContainer(blobClient, containerName);


    }

    @SuppressWarnings("unused")
    public static InputStream
    listBlobs(String provider, String containerName, String username, String password)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudStorageAccount azureAccount= initStorageConnection(username, password);
        CloudBlobClient     blobClient= azureAccount.createCloudBlobClient();
        CloudBlobContainer  container= initBlobContainer(blobClient, containerName);

        try
        {
        // Prepare XML building
            DocumentBuilderFactory xmlFactory= DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder= xmlFactory.newDocumentBuilder();
            Document doc= docBuilder.newDocument();
            doc.setXmlStandalone(true);
            long blobCounter= 0;

        // Create root element
            Element rootElement= doc.createElement(XMLROOT);
            rootElement.setAttribute("count", Long.toString(blobCounter));
            doc.appendChild(rootElement);

        // Iterate through all blobs
            for(ListBlobItem blobItem : container.listBlobs())
            {
                if(blobItem instanceof CloudBlob)
                {
                // Get blob metadata
                    CloudBlob blob= (CloudBlob) blobItem;
                    String blobName= blob.getName();
                    long blobSize= blob.getProperties().getLength();

                    Element blobEntry= doc.createElement("blob");
                    blobEntry.setAttribute("name", blobName);
                    blobEntry.setAttribute("size", Long.toString(blobSize));

                // Create blob entry and update blob count in root element
                    blobCounter++;
                    rootElement.appendChild(blobEntry);
                    rootElement.setAttribute("count", Long.toString(blobCounter));
                }
            }

        // Prepare XML file output
            TransformerFactory transformerFactory= TransformerFactory.newInstance();
            Transformer transformer= transformerFactory.newTransformer();
            DOMSource source= new DOMSource(doc);

        // Output XML file to memory
            ByteArrayOutputStream outStream= new ByteArrayOutputStream();
            Result result= new StreamResult(outStream);
            transformer.transform(source, result);

        // Return InputStream to XML file
            return new ByteArrayInputStream(outStream.toByteArray());
        }
        catch (Exception exception)
        {
            throw new InternalServerErrorException("Error generating XML file for container " + containerName);
        }
    }

    // container.createIfNotExists();
}