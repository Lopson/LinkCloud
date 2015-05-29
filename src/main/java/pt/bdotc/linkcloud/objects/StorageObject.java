package pt.bdotc.linkcloud.objects;

import java.io.InputStream;

/**
 * Created by Lopson on 29/05/2015.
 *
 */
public interface StorageObject
{
    InputStream
    downloadBlob(String provider, String containerName, String blobName, String username, String password);

    void
    uploadBlob(String provider, String containerName, String blobName, String username, String password,
               InputStream blobContents, long size);

    InputStream
    listBlobs(String provider, String containerName, String username, String password);

    void
    deleteBlob(String provider, String containerName, String blobName, String username, String password);
}
