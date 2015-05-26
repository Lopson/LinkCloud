package pt.bdotc.linkcloud;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import pt.bdotc.linkcloud.resources.RequestResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * Created by Lopson on 24/05/2015.
 *
 * Different from the one Miguel used; taken from the Java EE 7 examples repository.
 */
@ApplicationPath("/")
public class CloudWebApp extends Application
{
    @Override
    public Set<Class<?>>
    getClasses()
    {
        Set<Class<?>> classes = new java.util.HashSet<>();
        classes.add(RequestResource.class);
        classes.add(MultiPartFeature.class); // Jersey 2 Multipart Upload Feature

        return classes;
    }
}
