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

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

/**
 * @author Macik
 * 
 *         A class that contains the methods needed for construction of json
 *         object representations of provided data sets. Attributes are
 *         translated to json objects and arrays of json objects depending on
 *         the attributes and dictionary. The dictionary is set to translate the
 *         attributes to correspond to the SCIM group core schema representation
 */
public class GroupDataBuilder {

	private static final Log LOGGER = Log.getLog(GroupDataBuilder.class);

	/**
	 * Builds the "ObjectClassInfo" object which carries the schema information
	 * for a single resource.
	 * 
	 * @return An instance of ObjectClassInfo with the constructed schema
	 *         information.
	 **/
	public static ObjectClassInfo getGroupSchema() {

		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.setType(ObjectClass.GROUP_NAME);
		builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").setRequired(true).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("members.Group.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.Group.display").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.User.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.User.display").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("members.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.default.display").build());
		ObjectClassInfo groupSchemaInfo = builder.build();

		LOGGER.info("The constructed group schema representation: {0}", groupSchemaInfo);

		return groupSchemaInfo;
	}
}