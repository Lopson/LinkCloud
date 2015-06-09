package pt.bdotc.linkcloud.objects;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ejb.Stateless;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

/**
 * An implementation of the {@link StorageObject} interface for the Microsoft Azure Storage Service. All content that's
 * either downloaded or uploaded is streamed through the application server.
 */
@Stateless
public class AzureStorageObject implements StorageObject
{
    /**
     * Gives the caller a {@link com.microsoft.azure.storage.CloudStorageAccount CloudStorageAccount} object that can
     * be used to access a storage account. Also validates the username of the storage account, seeing as Azure's
     * Storage SDK doesn't do that out-of-the-box.
     *
     * @param username The name of the storage account.
     * @param password The access key of the storage account.
     * @return A {@link com.microsoft.azure.storage.CloudStorageAccount CloudStorageAccount} object that describes a
     *         storage account.
     * @throws BadRequestException Thrown when an invalid username or key is given.
     * @throws InternalServerErrorException Thrown when the creation of the connection string fails.
     */
    private CloudStorageAccount
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
            throw new BadRequestException("Bad key given");
        }
        catch(URISyntaxException badURIException)
        {
            throw new InternalServerErrorException("Error parsing connection string");
        }
    }

    /**
     * Returns a {@link com.microsoft.azure.storage.blob.CloudBlobContainer CloudBlobContainer} describing a blob
     * container, used to access blobs within it.
     *
     * @param username The username of the Azure account.
     * @param password The password of the Azure account.
     * @param containerName The name of the container to access.
     * @return A {@link com.microsoft.azure.storage.blob.CloudBlobContainer CloudBlobContainer} that represents the
     *         container that the invoker wants to access.
     * @throws BadRequestException Thrown when the name of the container is invalid. See also
     *         {@link #initStorageConnection} method.
     * @throws InternalServerErrorException Thrown when some other HTTP error that's not 400 is encountered. See
     *         also {@link #initStorageConnection} method.
     */
    private CloudBlobContainer
    initBlobContainer(String username, String password, String containerName)
    throws BadRequestException, InternalServerErrorException
    {
        CloudStorageAccount azureAccount= initStorageConnection(username, password);
        CloudBlobClient     blobClient= azureAccount.createCloudBlobClient();

        try
        {
            return blobClient.getContainerReference(containerName);
        }
        catch(StorageException containerError)
        {
            int httpStatusCode= containerError.getHttpStatusCode();

            if(httpStatusCode== 400) {throw new BadRequestException("Invalid container name " + containerName);}
            else                     {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch(URISyntaxException error)
        {
            throw new InternalServerErrorException("Error encountered when parsing container " + containerName);
        }
    }

    /**
     * Returns a {@link com.microsoft.azure.storage.blob.CloudBlobContainer CloudBlobContainer} describing a blob
     * container, used to access blobs within it. Unlike {@link #initBlobContainer}, this one only returns this object
     * if the container itself exists. Works as a wrapper around {@link #initBlobContainer}.
     *
     * @param username The username of the Azure account.
     * @param password The password of the Azure account.
     * @param containerName The name of the container to access.
     * @return A {@link com.microsoft.azure.storage.blob.CloudBlobContainer CloudBlobContainer} that represents the
     *         container that the invoker wants to access. Returned only if the container actually exists.
     * @throws BadRequestException Thrown when the name of the container is invalid. See also
     *         {@link #initStorageConnection} method.
     * @throws InternalServerErrorException Thrown when an error occurs while testing the container's existance. See
     *         also {@link #initStorageConnection} method.
     * @throws NotFoundException Thrown when the container doesn't exist.
     */
    private CloudBlobContainer
    initBlobContainerIfExists(String username, String password, String containerName)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
        CloudBlobContainer container= initBlobContainer(username, password, containerName);

        try
        {
            if(!container.exists()) {throw new NotFoundException("Container " + containerName + " doesn't exist.");}
            else                    {return container;}
        }
        catch(StorageException error)
        {
            throw new InternalServerErrorException("Error encountered when checking if container " + containerName +
                                                   " exists or not.");
        }
    }

    /**
     * Downloads a blob from a specific container belonging to a specific Azure Storage account. Streams the blob's
     * content from this application into the client.
     *
     * @param containerName The name of the container that has the blob to download.
     * @param blobName The name of the blob to download.
     * @param username The username of the Azure Storage account to use.
     * @param password The password of the Azure Storage account.
     * @return An {@link java.io.InputStream InputStream} object that the server will use to send the blob's content
     *         into the client. This object is automatically closed by the server itself.
     * @throws BadRequestException Thrown when the given blob name is invalid. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     * @throws InternalServerErrorException Thrown when a non 400 or 404 HTTP error is encountered. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     * @throws NotFoundException Thrown when the given blob doesn't exist. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     */
    public InputStream
    downloadBlob(String containerName, String blobName, String username, String password)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudBlobContainer container= initBlobContainerIfExists(username, password, containerName);

        try
        {
        // Download blob
            CloudBlockBlob blockBlob= container.getBlockBlobReference(blobName);
            return blockBlob.openInputStream();
        }
        catch(StorageException blobError)
        {
        // 400 for invalid name; 404 for missing; anything else is error
            int httpStatusCode= blobError.getHttpStatusCode();

            if     (httpStatusCode== 400) {throw new BadRequestException("Invalid blob name " + blobName);}
            else if(httpStatusCode== 404) {throw new NotFoundException("Blob " + blobName + " not found");}
            else                          {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch(URISyntaxException e)
        {
        // Should never happen
            throw new InternalServerErrorException("Error encountered when parsing blob " + blobName);
        }
    }

    /**
     * Uploads a blob into a specific container belonging to a given Azure Storage account. The contents that are to be
     * put into a blob with a given name are streamed from the client into the Storage account. Note that if the blob
     * already exists, it'll be overwritten.
     *
     * @param containerName The name of the container in which the blob will be created.
     * @param blobName The name of the blob to create or overwrite.
     * @param username The username of the Azure Storage account to use.
     * @param password The password of the Azure Storage account.
     * @throws BadRequestException Thrown when the given blob name is invalid. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     * @throws InternalServerErrorException Thrown when a non 400 HTTP error is encountered, when an IO error occurs
     *         while sending the content into the Storage account, or when an error is encountered when trying to get
     *         the blob's URI. See also the {@link #initBlobContainerIfExists} method of this class.
     * @throws NotFoundException See the {@link #initBlobContainerIfExists} method of this class.
     */
    public void
    uploadBlob(String containerName, String blobName, String username, String password,
               InputStream blobContents, long size)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudBlobContainer container= initBlobContainerIfExists(username, password, containerName);

        try
        {
            /* AFAIK, the upload() method is still streaming, not caching. This also has the
             * advantage of guaranteeing that there'll be no SegFaults while reading from the
             * InputStream given, since it'll read exactly size bytes from it. */
            CloudBlockBlob blockBlob= container.getBlockBlobReference(blobName);
            blockBlob.upload(blobContents, size);
        }
        catch(StorageException blobError)
        {
        // 400 for invalid name; 404 for missing; anything else is error
            int httpStatusCode= blobError.getHttpStatusCode();

            if(httpStatusCode== 400) {throw new BadRequestException("Invalid blob name " + blobName);}
            else                     {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch(IOException | URISyntaxException e)
        {
        // Should never happen
            throw new InternalServerErrorException("Error encountered when parsing blob " + blobName);
        }

    }

    /**
     * Deletes a given block from a given container with the given Azure Storage account credentials.
     *
     * @param containerName The name of the container in which the blob to be deleted exists.
     * @param blobName The name of the blob to delete.
     * @param username The Storage account's username.
     * @param password The Storage account's password.
     * @throws BadRequestException Thrown when the given blob name is invalid. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     * @throws InternalServerErrorException Thrown when a non 400 or 404 HTTP error is encountered. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     * @throws NotFoundException Thrown when the blob to be deleted doesn't exist. See also the
     *         {@link #initBlobContainerIfExists} method of this class.
     */
    public void
    deleteBlob(String containerName, String blobName, String username, String password)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudBlobContainer container= initBlobContainerIfExists(username, password, containerName);

        try
        {
            CloudBlockBlob blockBlob = container.getBlockBlobReference(blobName);
            blockBlob.delete();
        }
        catch(StorageException blobError)
        {
        // 400 for invalid name; 404 for missing; anything else is error
            int httpStatusCode= blobError.getHttpStatusCode();

            if     (httpStatusCode== 400) {throw new BadRequestException("Invalid blob name " + blobName);}
            else if(httpStatusCode== 404) {throw new NotFoundException("Blob " + blobName + " not found");}
            else                          {throw new InternalServerErrorException("Unknown error encountered");}
        }
        catch(URISyntaxException e)
        {
        // Should never happen
            throw new InternalServerErrorException("Error encountered when parsing blob " + blobName);
        }
    }

    /**
     * Lists the blobs that exist in a given container belonging to a given Storage account. The list is built as an
     * XML file to be sent into the client. The XML file that this method builds resides in memory until it's fully
     * sent to the client.
     *
     * @param containerName The name of the container that's to be accessed.
     * @param username The username of the Storage account to use.
     * @param password The password of the Storage account.
     * @return An {@link java.io.InputStream} object that the server will use to send the XML file.
     * @throws BadRequestException See the {@link #initBlobContainerIfExists} method of this class.
     * @throws InternalServerErrorException Thrown when any kind of exception that rises from the creation of the XML
     *         file is caught. See also the {@link #initBlobContainerIfExists} method of this class.
     * @throws NotFoundException See the {@link #initBlobContainerIfExists} method of this class.
     */
    public InputStream
    listBlobs(String containerName, String username, String password)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudBlobContainer container= initBlobContainerIfExists(username, password, containerName);

        try
        {
        // Prepare XML building
            DocumentBuilderFactory xmlFactory= DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder= xmlFactory.newDocumentBuilder();
            Document doc= docBuilder.newDocument();
            doc.setXmlStandalone(true);
            long blobCounter= 0;

        // Create root element
            Element rootElement= doc.createElement(XML_ROOT);
            rootElement.setAttribute(XML_ROOT_COUNT, Long.toString(blobCounter));
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

                    Element blobEntry= doc.createElement(XML_BLOB);
                    blobEntry.setAttribute(XML_BLOB_NAME, blobName);
                    blobEntry.setAttribute(XML_BLOB_SIZE, Long.toString(blobSize));

                // Create blob entry and update blob count in root element
                    blobCounter++;
                    rootElement.appendChild(blobEntry);
                    rootElement.setAttribute(XML_ROOT_COUNT, Long.toString(blobCounter));
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

    /**
     * Creates a container, but only if it doesn't exist.
     *
     * @param containerName The name of the container to create.
     * @param username The username of the Azure account.
     * @param password The password of the Azure account.
     * @throws BadRequestException See the {@link #initBlobContainerIfExists} method of this class.
     * @throws InternalServerErrorException Thrown when an error is encountered while creating the container.See also
     *         the {@link #initBlobContainerIfExists} method of this class.
     */
    public void
    createContainerIfNotExists(String containerName, String username, String password)
    throws BadRequestException, InternalServerErrorException
    {
    // Setup access to container
        CloudBlobContainer container= initBlobContainer(username, password, containerName);

        try                       {container.createIfNotExists();}
        catch(StorageException e) {throw new InternalServerErrorException("Error creating container " + containerName);}
    }

    /**
     * Checks if a given container exists or not.
     *
     * @param containerName The name of the container to test.
     * @param username The username of the account to use in a CSP.
     * @param password The password of the CSP account.
     * @return {@code true} if container exists; {@code false} if it doesn't.
     * @throws BadRequestException See {@link #initBlobContainerIfExists}.
     * @throws InternalServerErrorException See {@link #initBlobContainerIfExists}.
     */
    public boolean
    containerExists(String containerName, String username, String password)
    throws BadRequestException, InternalServerErrorException
    {
        try
        {
            initBlobContainerIfExists(username, password, containerName);
            return true;
        }
        catch(NotFoundException containerMissing)
        {
            return false;
        }
    }

    /**
     * Deletes a container.
     *
     * @param containerName The name of the container to delete.
     * @param username The username of the Azure account.
     * @param password The password of the Azure account.
     * @throws BadRequestException Thrown when the given blob name is invalid.
     *         See also {@link #initBlobContainerIfExists}.
     * @throws InternalServerErrorException Thrown when an unknown error is encountered.
     *         See also {@link #initBlobContainerIfExists}.
     * @throws NotFoundException Thrown when the given container doesn't exist.
     */
    public void
    deleteContainer(String containerName, String username, String password)
    throws BadRequestException, InternalServerErrorException, NotFoundException
    {
    // Setup access to container
        CloudBlobContainer container= initBlobContainerIfExists(username, password, containerName);

        try
        {
            container.delete();
        }
        catch(StorageException blobError)
        {
        // 400 for invalid name; 404 for missing; anything else is error
            int httpStatusCode= blobError.getHttpStatusCode();

            if     (httpStatusCode== 400) {throw new BadRequestException("Invalid container name " + containerName);}
            else if(httpStatusCode== 404) {throw new NotFoundException("Container " + containerName + " not found");}
            else                          {throw new InternalServerErrorException("Unknown error encountered");}
        }
    }
}