package com.evolveum.polygon.scim;

import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.json.JSONObject;

public interface ObjectTranslator {
	/**
	 * Constructs a json object representation out of the provided data set and
	 * schema dictionary. The json object representation will contain only
	 * attributes which comply to the provided schema and operation attributes
	 * as defined in the SCIM patch specification.
	 * 
	 * @param passedAttributeSet
	 *            A set of attributes provided by the identity management
	 *            system.
	 * @param orgIdAttributeset
	 *            A set of attributes which are injected into the provided set.
	 * @return The complete json representation of the provided data set.
	 */
	JSONObject translateSetToJson(Set<Attribute> passedAttributeSet, Set<Attribute> orgIdAttributeset);
}
