# Link Cloud
## A very simple RESTful service that allows an application to access Microsoft's Azure Blob Storage service

This application was created out of the frustrating experience I had while using Apache's own Deltacloud. Said
application refused to work on many different kinds of setups (different Ruby versions, different Gems versions, you
name it). Seeing as its development has seemingly stopped, I decided to make an alternative for my own needs.
 
This small Java service was developed with JAX-RS, and is tested on Glassfish 4. It does everything I want it to do,
plus it can be easily extended to use more CSPs.

Here's what it's be able to do for now:
* Download a blob (`GET` at `/api/{provider}/{container}/{blob}`);
* Upload a blob (`POST` at  `/api/{provider}/{container}/{blob}?size={file_size_in_bytes}`);
* Delete a blob (`DELETE` at `/api/{provider}/{container}/{blob}`);
* Lists all blobs in a container (`GET` at `/api/{provider}/{container}`, returns an XML file).

All login credentials must be sent to the service through Basic Authentication HTTP headers (`accountName:accountKey`).
The XML file returned by the listing method should have the following structure:

    <blobs count="2">
        <blob name="file.txt" size="10" />
        <blob name="file2.png" size="1005498" />
    </blobs>
    
Please note that the size of a file is given in bytes.

### Supported CSPs/Extending CSP Support

The program is currently capable of interacting with Microsoft's Azure Blob Storage service, treating everything as
block blobs. This interaction is described in the `AzureStorageObject` Java file, which in turn implements the
interface described in the file `StorageObject`. If you want to implement some other CSP, all you have to do is
implement this given interface, add the `StorageObject` you've created into the `providersSet` hash map located in the
`RequestResource` file and you're good to go!

