package com.techoworld.eightythree.servlet;

import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.*;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GenericODataServlet extends HttpServlet {

    private final CsdlEdmProvider provider;
    private final List<Object> processors;

    public GenericODataServlet(CsdlEdmProvider provider, List<Object> processors) {
        this.provider = provider;
        this.processors = processors;
    }

    @Override
    public void init() throws ServletException {
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, getServletContext());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        OData odata = OData.newInstance();
        ServiceMetadata metadata = odata.createServiceMetadata(provider, new ArrayList<>());
        ODataHttpHandler handler = odata.createHandler(metadata);

        for (Object p : processors) {
            if (p instanceof EntityProcessor) {
                handler.register((EntityProcessor) p);
            } else if (p instanceof EntityCollectionProcessor) {
                handler.register((EntityCollectionProcessor) p);
            } else if (p instanceof PrimitiveProcessor) {
                handler.register((PrimitiveProcessor) p);
            } else if (p instanceof ComplexProcessor) {
                handler.register((ComplexProcessor) p);
            } else if (p instanceof BatchProcessor) {
                handler.register((BatchProcessor) p);
            } else {
                throw new IllegalArgumentException("Unsupported processor type: " + p.getClass());
            }
        }

        handler.process(req, resp);
    }
}
