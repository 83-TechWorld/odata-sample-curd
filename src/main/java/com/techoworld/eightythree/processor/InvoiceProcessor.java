package com.techoworld.eightythree.processor;

import com.techoworld.eightythree.entity.Invoice;
import com.techoworld.eightythree.provider.InvoiceEdmProvider;
import com.techoworld.eightythree.service.InvoiceService;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.*;
import org.apache.olingo.server.api.processor.*;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Component
public class InvoiceProcessor implements EntityCollectionProcessor, EntityProcessor {

    private final InvoiceService service;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public InvoiceProcessor(InvoiceService service) { this.service = service; }

    @Override public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata; this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest req, ODataResponse resp, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        EdmEntityType type = serviceMetadata.getEdm().getEntityType(
                new FullQualifiedName(InvoiceEdmProvider.NAMESPACE, "Invoice"));
        EntityCollection coll = new EntityCollection();
        for (Invoice i : service.findAll()) coll.getEntities().add(toEntity(i));

        ODataSerializer ser = odata.createSerializer(responseFormat);
        SerializerResult result = ser.entityCollection(serviceMetadata, type, coll,
                EntityCollectionSerializerOptions.with().build());

        resp.setContent(result.getContent());
        resp.setStatusCode(HttpStatusCode.OK.getStatusCode());
        resp.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void readEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        Long id = key(uriInfo);
        Invoice inv = service.findById(id).orElseThrow(
                () -> new ODataApplicationException("Invoice not found", 404, Locale.ENGLISH));

        EdmEntityType type = serviceMetadata.getEdm().getEntityType(
                new FullQualifiedName(InvoiceEdmProvider.NAMESPACE, "Invoice"));
        ODataSerializer ser = odata.createSerializer(responseFormat);
        SerializerResult result = ser.entity(serviceMetadata, type, toEntity(inv),
                EntitySerializerOptions.with().build());

        resp.setContent(result.getContent());
        resp.setStatusCode(HttpStatusCode.OK.getStatusCode());
        resp.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, DeserializerException, SerializerException {

        Entity e = odata.createDeserializer(requestFormat).entity(req.getBody(),
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(InvoiceEdmProvider.NAMESPACE, "Invoice"))).getEntity();

        Invoice saved = service.save(fromEntity(e));
        Entity created = toEntity(saved);

        ODataSerializer ser = odata.createSerializer(responseFormat);
        SerializerResult result = ser.entity(serviceMetadata,
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(InvoiceEdmProvider.NAMESPACE, "Invoice")),
                created, EntitySerializerOptions.with().build());

        resp.setContent(result.getContent());
        resp.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        resp.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        resp.setHeader(HttpHeader.LOCATION, created.getId().toASCIIString());
    }

    @Override
    public void updateEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, DeserializerException {

        Long id = key(uriInfo);
        Invoice existing = service.findById(id)
                .orElseThrow(() -> new ODataApplicationException("Invoice not found", 404, Locale.ENGLISH));

        Entity e = odata.createDeserializer(requestFormat).entity(req.getBody(),
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(InvoiceEdmProvider.NAMESPACE, "Invoice"))).getEntity();

        String invoiceNumber = stringProp(e, "invoiceNumber");
        Double totalAmount = doubleProp(e, "totalAmount");
        Long customerId = longProp(e, "customerId");

        if (invoiceNumber != null) existing.setInvoiceNumber(invoiceNumber);
        if (totalAmount != null) existing.setTotalAmount(totalAmount);
        if (customerId != null) existing.setCustomerId(customerId);

        service.save(existing);
        resp.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo)
            throws ODataApplicationException {
        service.deleteById(key(uriInfo));
        resp.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    /* helpers */
    private Entity toEntity(Invoice i) {
        Entity e = new Entity()
                .addProperty(new Property(null, "id", ValueType.PRIMITIVE, i.getId()))
                .addProperty(new Property(null, "invoiceNumber", ValueType.PRIMITIVE, i.getInvoiceNumber()))
                .addProperty(new Property(null, "totalAmount", ValueType.PRIMITIVE, i.getTotalAmount()))
                .addProperty(new Property(null, "customerId", ValueType.PRIMITIVE, i.getCustomerId()));
        e.setId(uri("Invoices", i.getId()));
        return e;
    }
    private Invoice fromEntity(Entity e) {
        Invoice i = new Invoice();
        i.setInvoiceNumber(stringProp(e, "invoiceNumber"));
        i.setTotalAmount(doubleProp(e, "totalAmount"));
        i.setCustomerId(longProp(e, "customerId"));
        return i;
    }
    private String stringProp(Entity e, String n){ Property p=e.getProperty(n); return p==null?null:(String)p.getValue();}
    private Double doubleProp(Entity e, String n){ Property p=e.getProperty(n); return p==null?null:(Double)p.getValue();}
    private Long longProp(Entity e, String n){ Property p=e.getProperty(n); return p==null?null:((Number)p.getValue()).longValue();}
    private Long key(UriInfo uriInfo) {
        // Get first part of URI (e.g. Customers(1))
        if (uriInfo.getUriResourceParts().isEmpty()) {
            throw new IllegalArgumentException("No resource parts in URI");
        }

        UriResource firstResource = uriInfo.getUriResourceParts().get(0);

        if (firstResource instanceof UriResourceEntitySet) {
            UriResourceEntitySet entitySet = (UriResourceEntitySet) firstResource;

            if (!entitySet.getKeyPredicates().isEmpty()) {
                String keyValue = String.valueOf(entitySet.getKeyPredicates().get(0));
                return Long.valueOf(keyValue);
            } else {
                throw new IllegalArgumentException("No key predicates found in URI");
            }
        }

        throw new IllegalArgumentException("First resource is not an EntitySet: " + firstResource.getKind());
    }
    private URI uri(String set, Object id){ try{return new URI(set+"("+id+")");}catch(URISyntaxException ex){throw new RuntimeException(ex);} }
}
