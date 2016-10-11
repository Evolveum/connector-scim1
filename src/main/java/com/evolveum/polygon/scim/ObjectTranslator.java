/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.polygon.scim;

import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.json.JSONObject;

/**
 * 
 * @author Macik
 * 
 *         Interface which defines the basic json data builder method.
 */
public interface ObjectTranslator {

	String DELETE = "delete";
	String DELIMITER = "\\.";
	String DEFAULT = "default";
	String TYPE = "type";
	String OPERATION = "operation";
	String DOT = ".";
	String BLANK = "blank";
	String SCHEMA = "schema";

	/**
	 * Constructs a json object representation out of the provided data set and
	 * schema dictionary. The json object representation will contain only
	 * attributes which comply to the provided schema and operation attributes
	 * as defined in the SCIM patch specification.
	 * 
	 * @param imsAttributes
	 *            A set of attributes provided by the identity management
	 *            system.
	 *            <p>
	 *            e.g. [Attribute: {Name=name.familyName, Value=[Watson]},
	 *            Attribute: {Name=name.givenName, Value=[John]}]
	 * @param injectedAttributes
	 *            A set of attributes which are injected into the provided set.
	 *            <p>
	 *            e.g. [Attribute: {Name=name.middleName, Value=[Hamish]}]
	 * @return The complete json representation of the provided data set.
	 */
	JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes);
}
