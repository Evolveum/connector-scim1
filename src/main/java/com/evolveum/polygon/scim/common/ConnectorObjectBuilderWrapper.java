package com.evolveum.polygon.scim.common;

import java.util.*;
import java.util.stream.Collectors;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;

import com.evolveum.polygon.scim.UserSchemaBuilder;
import com.evolveum.polygon.scim.GroupDataBuilder;


/**
 * @author Vlad Kislitsyn
 * 
 *         Wrapper class for class ConnectorObject
 *         Validates attributes based on schema definition
 */

public class ConnectorObjectBuilderWrapper {

    private static final Log LOGGER = Log.getLog(ConnectorObjectBuilderWrapper.class);

    final private Set<String> attributesName;
    final private ConnectorObjectBuilder cob;
    final private ObjectClass oclass;
    final private List<String> missedAttributes = new ArrayList<>();

    public ConnectorObjectBuilderWrapper(ObjectClass oclass) {
        this.oclass = oclass;
        ObjectClassInfo schema;
        if (ObjectClass.ACCOUNT.equals(oclass)) {
            schema = UserSchemaBuilder.getUserSchema();
        }
        else if (ObjectClass.GROUP.equals(oclass)) {
            schema = GroupDataBuilder.getGroupSchema();
        }
        else {
            LOGGER.error("Unsupported object class: {0}. Schema is not defined", oclass.getDisplayNameKey());
            throw new ConnectorException("Unsupported object class");
        }
        this.attributesName = schema.getAttributeInfo()
                .stream()
                .map(AttributeInfo::getName)
                .collect(Collectors.toSet());
        this.cob = new ConnectorObjectBuilder();
        this.cob.setObjectClass(oclass);
    }

    public ConnectorObject build() {
        if (!missedAttributes.isEmpty()) {
            String className = this.oclass.getDisplayNameKey();
            LOGGER.warn("The attributes \"{0}\" were omitted from the connId object {1} build.", missedAttributes.toString(), className);
        }
        return cob.build();
    }

    public ConnectorObjectBuilderWrapper setUid(String uid) {
        cob.setUid(uid);
        return this;
    }
      
    public ConnectorObjectBuilderWrapper setUid(Uid uid) {
        cob.setUid(uid);
        return this;
    }
      
    public ConnectorObjectBuilderWrapper setName(String name) {
        cob.setName(name);
        return this;
    }
      
    public ConnectorObjectBuilderWrapper setName(Name name) {
        cob.setName(name);
        return this;
    }

    public ConnectorObjectBuilderWrapper addAttribute(Attribute... attrs) {
        if (checkAttributes(attrs)) {
            cob.addAttribute(attrs);
        }
        return this;
    }
      
    public ConnectorObjectBuilderWrapper addAttributes(Collection<Attribute> attrs) {
        if (checkAttributes(attrs)) {
            cob.addAttributes(attrs);
        }
        return this;
    }
      
    public ConnectorObjectBuilderWrapper addAttribute(String name, Object... objs) {
        if (checkAttribute(name)) {
            cob.addAttribute(name, objs);
        }
        return this;
    }

    public ConnectorObjectBuilderWrapper addAttribute(String name, Collection<?> obj) {
        if (checkAttribute(name)) {
            cob.addAttribute(name, obj);
        }
        return this;
    }

    private boolean checkAttribute(Attribute attribute) {
        String name = attribute.getName();
        if (!attributesName.contains(name)) {
            missedAttributes.add(name);
            return false;
        }
        return true;
    }

    private boolean checkAttribute(String name) {
        if (!attributesName.contains(name)) {
            missedAttributes.add(name);
            return false;
        }
        return true;
    }

    private boolean checkAttributes(Collection<Attribute> attrs) {
        List<String> localMissedAttrubutes = attrs
                .stream()
                .filter(this::checkAttribute)
                .map(Attribute::getName)
                .collect(Collectors.toList());
        missedAttributes.addAll(localMissedAttrubutes);
        return localMissedAttrubutes.isEmpty();
    }

    private boolean checkAttributes(Attribute... attrs) {
        List<String> localMissedAttrubutes = Arrays.stream(attrs)
                .filter(this::checkAttribute)
                .map(Attribute::getName)
                .collect(Collectors.toList());
        missedAttributes.addAll(localMissedAttrubutes);
        return localMissedAttrubutes.isEmpty();
    }
}
