# Link Cloud
## A very simple RESTful service that allows an application to access Microsoft's Azure Blob Storage service

This application was created out of the frustrating experience I had while using Apache's own Deltacloud. Said
application refused to work on many different kinds of setups (different Ruby versions, different Gems versions, you
name it). Seeing as its development has seemingly stopped, I decided to make an alternative one for my own needs.
 
This small Java service is being developed with JAX-RS, and is tested on Glassfish 4. It's far from complete and is
probably full of newbie mistakes, seeing as this is my first time using these Java technologies.

Here's what it'll be able to do:
* Download a blob (`GET` at `/api/{provider}/{container}/{blob}`);
* Upload a blob (`POST` at  `/api/{provider}/{container}`);
* Delete a blob (`DELETE` at `/api/{provider}/{container}/{blob}`);
* Lists all blobs in a container (`GET` at `/api/{provider}/{container}`).

The program can be easily modified to support multiple cloud service providers. All login credentials must be sent to
the service through Basic Authentication HTTP headers (`accountName:accountKey`).

