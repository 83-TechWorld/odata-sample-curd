package com.techoworld.eightythree.processor;


import com.techoworld.eightythree.entity.Customer;
import com.techoworld.eightythree.provider.CustomerEdmProvider;
import com.techoworld.eightythree.service.CustomerService;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.*;
import org.apache.olingo.server.api.processor.*;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class CustomerProcessor implements EntityCollectionProcessor, EntityProcessor {

    private final CustomerService service;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public CustomerProcessor(CustomerService service) { this.service = service; }

    @Override public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata; this.serviceMetadata = serviceMetadata;
    }

    /* GET /Customers */
    @Override
    public void readEntityCollection(org.apache.olingo.server.api.ODataRequest request,
                                     org.apache.olingo.server.api.ODataResponse response,
                                     UriInfo uriInfo,
                                     ContentType requestedContentType) throws ODataApplicationException, SerializerException {
        List<Customer> customers = service.findAll();

        EntityCollection entityCollection = new EntityCollection();
        for (Customer c : customers) {
            Entity entity = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, c.getId()))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, c.getName()))
                    .addProperty(new Property(null, "Email", ValueType.PRIMITIVE, c.getEmail()));
            entity.setId(createId("Customers", c.getId()));
            entityCollection.getEntities().add(entity);
        }

        ODataSerializer serializer = odata.createSerializer(requestedContentType);

        UriResourceEntitySet uriResourceEntitySet =
                (UriResourceEntitySet) uriInfo.getUriResourceParts().getFirst();
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        ContextURL contextUrl = ContextURL.with()
                .entitySet(edmEntitySet)   // <-- pass EdmEntitySet, not string or FQN
                .suffix(ContextURL.Suffix.ENTITY)     // Include suffix if it's a single entity
                .build();

        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions
                .with()
                .contextURL(contextUrl)
                .build();

        SerializerResult serializerResult = serializer.entityCollection(
                serviceMetadata,
                edmEntitySet.getEntityType(),
                entityCollection,
                opts
        );

        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /* GET /Customers(<id>) */
    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
                           ContentType responseFormat) throws ODataApplicationException, SerializerException {

        // 1. Extract the key predicate from the request URI (e.g., /Customers(1))
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().getFirst();
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // Get the key from URI
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        Long id = Long.valueOf(keyPredicates.getFirst().getText().replace("'", "")); // strip quotes if any

        // 2. Fetch entity from DB using JPA repository/service
        Optional<Customer> customer = service.findById(id);
        if (customer.isEmpty()) {
            throw new ODataApplicationException("Customer not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // 3. Convert JPA entity -> OData Entity
        Entity odataEntity = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, customer.get().getId()))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, customer.get().getName()))
                .addProperty(new Property(null, "Email", ValueType.PRIMITIVE, customer.get().getEmail()));
        odataEntity.setId(createId("Customers", customer.get().getId()));

        // 4. Serialize to OData response
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).suffix(ContextURL.Suffix.ENTITY).build();

        EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextUrl).build();
        SerializerResult serializerResult = serializer.entity(serviceMetadata, edmEntityType, odataEntity, opts);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
    /* POST /Customers */
    @Override
    public void createEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, DeserializerException, SerializerException {

        ODataDeserializer des = odata.createDeserializer(requestFormat);
        Entity e = des.entity(req.getBody(),
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(CustomerEdmProvider.NAMESPACE, "Customer"))).getEntity();

        Customer saved = service.save(fromEntity(e));
        Entity created = toEntity(saved);

        ODataSerializer ser = odata.createSerializer(responseFormat);
        SerializerResult result = ser.entity(serviceMetadata,
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(CustomerEdmProvider.NAMESPACE, "Customer")),
                created, EntitySerializerOptions.with().build());

        resp.setContent(result.getContent());
        resp.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        resp.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        resp.setHeader(HttpHeader.LOCATION, created.getId().toASCIIString());
    }

    /* PUT /Customers(<id>) */
    @Override
    public void updateEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, DeserializerException {

        Long id = key(uriInfo);
        Customer existing = service.findById(id)
                .orElseThrow(() -> new ODataApplicationException("Customer not found", 404, Locale.ENGLISH));

        Entity e = odata.createDeserializer(requestFormat).entity(req.getBody(),
                serviceMetadata.getEdm().getEntityType(new FullQualifiedName(CustomerEdmProvider.NAMESPACE, "Customer"))).getEntity();

        String name = stringProp(e, "name");
        String email = stringProp(e, "email");
        if (name != null) existing.setName(name);
        if (email != null) existing.setEmail(email);

        service.save(existing);
        resp.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    /* DELETE /Customers(<id>) */
    @Override
    public void deleteEntity(ODataRequest req, ODataResponse resp, UriInfo uriInfo)
            throws ODataApplicationException {
        service.deleteById(key(uriInfo));
        resp.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    /* helpers */
    private Entity toEntity(Customer c) {
        Entity e = new Entity()
                .addProperty(new Property(null, "id", ValueType.PRIMITIVE, c.getId()))
                .addProperty(new Property(null, "name", ValueType.PRIMITIVE, c.getName()))
                .addProperty(new Property(null, "email", ValueType.PRIMITIVE, c.getEmail()));
        e.setId(uri("Customers", c.getId()));
        return e;
    }
    private Customer fromEntity(Entity e) {
        Customer c = new Customer();
        c.setName(stringProp(e, "name"));
        c.setEmail(stringProp(e, "email"));
        return c;
    }
    private String stringProp(Entity e, String n) {
        Property p = e.getProperty(n); return p == null ? null : (String) p.getValue();
    }
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
    private URI uri(String set, Object id) {
        try { return new URI(set + "(" + id + ")"); } catch (URISyntaxException ex) { throw new RuntimeException(ex); }
    }
}
