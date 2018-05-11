package ca.bc.gov.nrs.dm.microservice.api;

import java.net.InetSocketAddress;

import org.apache.cxf.cdi.CXFCdiServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.weld.environment.servlet.BeanManagerResourceBindingListener;
import org.jboss.weld.environment.servlet.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import io.prometheus.client.exporter.MetricsServlet;

@javax.annotation.Generated(value = "class com.quartech.codegen.FuseGenerator", date = "2017-04-24T09:07:23.579-07:00")
public class ApplicationStarter {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationStarter.class);
	
    public static void main(final String[] args) throws Exception {
        startServer().join();
    }

    public static Server startServer() throws Exception {

        String port = System.getProperty("HTTP_PORT");
        
        if (port == null) {
            port = System.getenv("HTTP_PORT");
        }
        if (port == null) {
            port = "8080";
        }
        Integer num = Integer.parseInt(port);
        String service = Systems.getEnvVarOrSystemProperty("WEB_CONTEXT_PATH", "WEB_CONTEXT_PATH", "");
        String servicesPath = "/servicesList";

        String servletContextPath = "/" + service;

        LOG.info("***************************************");
        LOG.info("Starting REST server at:         http://localhost:" + port + servletContextPath);
        LOG.info("View the services at:            http://localhost:" + port + servletContextPath + servicesPath);
        LOG.info("***************************************");

        InetSocketAddress inetaddr = new InetSocketAddress("0.0.0.0", num);
        final Server server = new Server(inetaddr);

        // Register and map the dispatcher servlet
        final ServletHolder servletHolder = new ServletHolder(new CXFCdiServlet());

        // change default service list URI
        servletHolder.setInitParameter("service-list-path", servicesPath);

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
        context.addEventListener(new Listener());
        context.addEventListener(new BeanManagerResourceBindingListener());

        String servletPath = "/*";
        if (Strings.isNotBlank(service)) {
            servletPath = servletContextPath + "/*";
        }
        context.addServlet(servletHolder, servletPath);

        server.setHandler(context);
        
        
        server.start();
        
        return server;
    }

}
