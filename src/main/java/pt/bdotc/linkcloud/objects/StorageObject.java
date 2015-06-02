package pt.bdotc.linkcloud.objects;

import java.io.InputStream;

/**
 * Interface for creating interaction classes for different Cloud Service Providers (CSP). This Interface contains
 * the following methods:
 * <ul>
 *    <li>Blob download {@code (downloadBlob)};</li>
 *    <li>Blob upload {@code (uploadBlob)};</li>
 *    <li>Blob deletion {@code (deleteBlob)};</li>
 *    <li>Blob listing {@code (listBlobs)}.</li>
 * </ul>
 */
public interface StorageObject
{
    /** String with the name of the root element of the XML responses of the listBlobs. */
    String XML_ROOT = "blobs";
    /** String with the name of the attribute of the {@link #XML_ROOT} that's to contain a counting of blobs
     *  inside a container. */
    String XML_ROOT_COUNT= "count";
    /** String with the name of the child element of the XML that describes a blob. */
    String XML_BLOB= "blob";
    /** String with the name of the attribute of the {@link #XML_BLOB} element that's to contain a blob's name. */
    String XML_BLOB_NAME= "name";
    /** String with the name of the attribute of the {@link #XML_BLOB} element that's to contain a blob's size
     *  in bytes. This element should be a {@code long} value. */
    String XML_BLOB_SIZE= "size";

/*---------------------
* --- BLOB REQUESTS ---
* ---------------------*/

    /**
     * Interface for the download of a blob from some CSP. Classes that implement this interface should make an active
     * effort to do no local caching, using instead {@link java.io.InputStream} and {@link java.io.OutputStream}.
     *
     * @param containerName The name of the container to access.
     * @param blobName The name of the blob to download.
     * @param username The name of the account to use in some CSP.
     * @param password The password of the account in the CSP.
     * @return An {@link java.io.InputStream} object that the application server will use to send the content to the
     *         client.
     */
    InputStream
    downloadBlob(String containerName, String blobName, String username, String password);

    /**
     * Interface for the upload of a blob into some CSP. Classes that implement this interface should try and avoid any
     * kind of caching, using streaming objects like {@link java.io.InputStream} and {@link java.io.OutputStream}. Also
     * note that an upload operation should overwrite an existing blob with the same name by default.
     *
     * @param containerName The name of the container to access.
     * @param blobName The name of the blob to create or overwrite.
     * @param username The name of the account to use in some CSP.
     * @param password The password of the account in the CSP.
     * @param blobContents An {@link java.io.InputStream} with the contents to upload.
     * @param size The size of the contents to upload in bytes.
     */
    void
    uploadBlob(String containerName, String blobName, String username, String password,
               InputStream blobContents, long size);

    /**
     * Interface for the deletion of a given blob.
     *
     * @param containerName The name of the container to access.
     * @param blobName The name of the blob to delete.
     * @param username The username of the account on a CSP.
     * @param password The password of the account to use on a CSP.
     */
    void
    deleteBlob(String containerName, String blobName, String username, String password);

/*--------------------------
* --- CONTAINER REQUESTS ---
* --------------------------*/

    /**
     * Interface for the listing of blobs that belong to a certain container in a CSP. This method has to return an XML
     * file with the following formatting:
     *
     * <pre>
     * {@code
     *
     * <XML_ROOT XML_ROOT_COUNT="number_of_blobs">
     *     <XML_BLOB XML_BLOB_NAME="name_of_a_blob" XML_BLOB_SIZE="blob_size_in_bytes" />
     * </XML_ROOT>
     * }
     * </pre>
     *
     * Here's an example with the predefined values for this Interface:
     *
     * <pre>
     * {@code
     *
     * <blobs count="3">
     *     <blob name="image1.jpg" size="10244" />
     *     <blob name="music1.flac" size="55048321" />
     *     <blob name="video1.mkv" size="348648321" />
     * </XML_ROOT>
     * }
     * </pre>
     *
     * Creating XML files in Java means that the file will reside in memory while it's being built. This means that,
     * for containers with a large number of blobs, the program might resort to using a significant amount of memory.
     *
     * @param containerName The name of the container to access.
     * @param username The name of the account in a CSP.
     * @param password The password of the account to use in a CSP.
     * @return An {@link java.io.InputStream} object that the application server will use to stream the XML file to
     * the client.
     */
    InputStream
    listBlobs(String containerName, String username, String password);

    /**
     * Interface for the creation of a container if and only if it doesn't exist. Should give no feedback in regards to
     * whether or not the container was created.
     *
     * @param containerName The name of the container to create.
     * @param username The username of the account of the CSP used.
     * @param password The password of the account of the CSP used.
     */
    void
    createContainerIfNotExists(String containerName, String username, String password);

    /**
     * Interface for testing whether or not a specific container exists.
     *
     * @param containerName The name of the container to test.
     * @param username The username of the account to use in a CSP.
     * @param password The password of the CSP account.
     * @return {@code true} if container exists; {@code false} if it doesn't.
     */
    boolean
    containerExists(String containerName, String username, String password);
}
