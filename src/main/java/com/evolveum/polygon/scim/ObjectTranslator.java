package com.evolveum.polygon.scim;

import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.json.JSONObject;

public interface ObjectTranslator {
	
	JSONObject translateSetToJson(Set<Attribute> attributes);
}
