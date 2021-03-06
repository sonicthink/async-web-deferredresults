package com.codebreeze.rest.server;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.annotation.WebServlet;


public class JettyDriver extends AbstractDriver{
    public static void main(final String... args) throws Exception {
        final EchoServiceConfiguration echoServiceConfiguration = parseParamsWithJCommander(args);
        final Server server = new Server(echoServiceConfiguration.port);
        final ServletHolder servletHolder = new ServletHolder(new AsyncDispatcherServlet(getContext()));
        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        servletContextHandler.addServlet(servletHolder, "/*");
        servletContextHandler.addEventListener(new ContextLoaderListener());

        servletContextHandler.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());

        server.setHandler(servletContextHandler);
        server.start();
        server.join();
    }



    @WebServlet(asyncSupported = true)
    private static class AsyncDispatcherServlet extends DispatcherServlet{

        public AsyncDispatcherServlet(final WebApplicationContext wac) {
            super(wac);
        }
    }
}
