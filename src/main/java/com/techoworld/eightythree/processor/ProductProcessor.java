package com.techoworld.eightythree.processor;

import com.techoworld.eightythree.entity.Product;
import com.techoworld.eightythree.provider.ProductEdmProvider;
import com.techoworld.eightythree.service.ProductService;
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
public class ProductProcessor implements EntityCollectionProcessor, EntityProcessor {

    private final ProductService service;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public ProductProcessor(ProductService service) { this.service = service; }

    @Override public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata; this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest req, ODataResponse resp, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        EdmEntityType type = serviceMetadata.getEdm().getEntityType(
                new FullQualifiedName(ProductEdmProvider.NAMESPACE, "Product"));
        EntityCollection coll = new EntityCollection();
        for (Product p : service.findAll()) coll.getEntities().add(toEntity(p));

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
        Product p = service.findById(id).orElseThrow(
                () -> new ODataApplicationException("Product not found", 404, Locale.ENGLISH));

        EdmEntityType type = serviceMetadata.getEdm().getEntityType(
                new FullQualifiedName(ProductEdmProvider.NAMESPACE, "Product"));
        ODataSerializer ser = odata.createSerializer(responseFormat);
        SerializerResult result = ser.entity(serviceMetadata, type, toEntity(p),
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
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(ProductEdmProvider.NAMESPACE, "Product"))).getEntity();

        Product saved = service.save(fromEntity(e));
        Entity created = toEntity(saved);

        ODataSerializer ser = odata.createSerializer(responseFormat);
        SerializerResult result = ser.entity(serviceMetadata,
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(ProductEdmProvider.NAMESPACE, "Product")),
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
        Product existing = service.findById(id)
                .orElseThrow(() -> new ODataApplicationException("Product not found", 404, Locale.ENGLISH));

        Entity e = odata.createDeserializer(requestFormat).entity(req.getBody(),
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(ProductEdmProvider.NAMESPACE, "Product"))).getEntity();

        String name = stringProp(e, "name");
        Double price = doubleProp(e, "price");
        if (name != null) existing.setName(name);
        if (price != null) existing.setPrice(price);

        service.save(existing);
        resp.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo)
            throws ODataApplicationException {
        service.deleteById(key(uriInfo));
        resp.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    private Entity toEntity(Product p) {
        Entity e = new Entity()
                .addProperty(new Property(null, "id", ValueType.PRIMITIVE, p.getId()))
                .addProperty(new Property(null, "name", ValueType.PRIMITIVE, p.getName()))
                .addProperty(new Property(null, "price", ValueType.PRIMITIVE, p.getPrice()));
        e.setId(uri("Products", p.getId()));
        return e;
    }
    private Product fromEntity(Entity e) {
        Product p = new Product();
        p.setName(stringProp(e, "name"));
        p.setPrice(doubleProp(e, "price"));
        return p;
    }
    private String stringProp(Entity e, String n) { Property p = e.getProperty(n); return p==null?null:(String)p.getValue(); }
    private Double doubleProp(Entity e, String n) { Property p = e.getProperty(n); return p==null?null:(Double)p.getValue(); }
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
