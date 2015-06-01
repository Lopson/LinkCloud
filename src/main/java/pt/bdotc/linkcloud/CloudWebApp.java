package pt.bdotc.linkcloud;

import pt.bdotc.linkcloud.resources.RequestResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * The start of the JAX-RS application.
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

        return classes;
    }
}
