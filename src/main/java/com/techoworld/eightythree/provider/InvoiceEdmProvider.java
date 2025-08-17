package com.techoworld.eightythree.provider;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import java.util.*;
import static org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind.*;

public class InvoiceEdmProvider extends CsdlAbstractEdmProvider {
    public static final String NAMESPACE = "CRM.Invoices";
    public static final String CONTAINER_NAME = "InvoicesContainer";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    public static final String ET_NAME = "Invoice";
    public static final FullQualifiedName ET_FQN = new FullQualifiedName(NAMESPACE, ET_NAME);
    public static final String ES_NAME = "Invoices";

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        if (ET_FQN.equals(entityTypeName)) {
            List<CsdlProperty> props = Arrays.asList(
                    new CsdlProperty().setName("id").setType(Int64.getFullQualifiedName()),
                    new CsdlProperty().setName("invoiceNumber").setType(String.getFullQualifiedName()),
                    new CsdlProperty().setName("totalAmount").setType(Double.getFullQualifiedName()),
                    new CsdlProperty().setName("customerId").setType(Int64.getFullQualifiedName())
            );
            return new CsdlEntityType().setName(ET_NAME)
                    .setProperties(props)
                    .setKey(List.of(new CsdlPropertyRef().setName("id")));
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        if (CONTAINER.equals(container) && ES_NAME.equals(entitySetName)) {
            return new CsdlEntitySet().setName(ES_NAME).setType(ET_FQN);
        }
        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        return new CsdlEntityContainer()
                .setName(CONTAINER_NAME)
                .setEntitySets(List.of(getEntitySet(CONTAINER, ES_NAME)));
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        CsdlSchema s = new CsdlSchema();
        s.setNamespace(NAMESPACE);
        s.setEntityTypes(List.of(getEntityType(ET_FQN)));
        s.setEntityContainer(getEntityContainer());
        return List.of(s);
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName fqn) {
        if (fqn == null || fqn.equals(CONTAINER)) {
            return new CsdlEntityContainerInfo().setContainerName(CONTAINER);
        }
        return null;
    }
}
