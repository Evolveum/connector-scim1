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
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;

/**
 * @author Macik
 * 
 *         A class that contains the methods needed for construction of the
 *         "User" scim core schema.
 */

public class UserSchemaBuilder {

	private static final Log LOGGER = Log.getLog(UserSchemaBuilder.class);

	/**
	 * Builds the "ObjectClassInfo" object which carries the schema information
	 * for a single resource.
	 * 
	 * @return An instance of ObjectClassInfo with the constructed schema
	 *         information.
	 **/
	public static ObjectClassInfo getUserSchema() {

		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("userName").setRequired(true).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.familyName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.givenName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.middleName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificPrefix").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificSuffix").build());

		builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

		builder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);

		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("nickName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("title").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("userType").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("locale").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("preferredLanguage").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("id").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements.default.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("entitlements.default.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.work.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.work.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.other.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.pager.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.pager.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.fax.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.fax.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.mobile.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.mobile.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.aim.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.aim.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.xmpp.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.xmpp.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.skype.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.skype.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.qq.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.qq.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.yahoo.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.yahoo.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.other.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.msn.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.msn.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.icq.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.icq.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.gtalk.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.gtalk.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("photos.photo.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("photos.photo.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("photos.thumbnail.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("photos.thumbnail.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.streetAddress").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.locality").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.region").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.postalCode").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.country").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.streetAddress").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.locality").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.region").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.postalCode").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.country").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.streetAddress").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.locality").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.region").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.postalCode").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.country").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("groups.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("groups.default.display").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements.default.display").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("roles.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("roles.default.display").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("roles.default.primary").setType(Boolean.class).build());

		ObjectClassInfo userSchemaInfo = builder.build();
		LOGGER.info("The constructed User core schema: {0}", userSchemaInfo);
		return userSchemaInfo;
	}
}
