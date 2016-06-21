package com.evolveum.polygon.scim;

import java.util.Map;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.json.JSONObject;

public interface ObjectTranslator {
	
	JSONObject translateSetToJson(Set<Attribute> passedAttributeSet,Set<Attribute> orgIdAttributeset);
	JSONObject translateSetToJson(Set<Attribute> passedAttributeSet,Set<Attribute> orgIdAttributeset,Map<String, Map<String, Object>> attributeMap);
}
