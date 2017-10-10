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
package com.evolveum.polygon.test.scim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.test.scim.PropertiesParser;

/**
 * 
 * @author Macik
 *
 */

public class SlackSpecificTestUtils extends StandardScimTestUtils {

	private static final Log LOGGER = Log.getLog(SlackSpecificTestUtils.class);

	private static Set<Attribute> userCreateBuilder(Integer testNumber) {

		StringBuilder testAttributeString = new StringBuilder();

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		testAttributeString.append(testNumber.toString()).append("testuser");

		attributeSet.add(AttributeBuilder.build(USERNAME, testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build(NICKNAME, testAttributeString.toString()));

		testAttributeString = new StringBuilder(testNumber.toString()).append("testuser@testdomain.com");
		attributeSet.add(AttributeBuilder.build(EMAILWORKVALUE, testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build(EMAILWORKPRIMARY, true));

		attributeSet.add(AttributeBuilder.build("title", "Mr."));
		attributeSet.add(AttributeBuilder.build(FAMILYNAME, "Ušer"));
		attributeSet.add(AttributeBuilder.build("name.givenName", "Tesť"));

		attributeSet.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, true));
		return attributeSet;
	}

	public static Uid createResourceTestHelper(String resourceName, Integer testNumber, ScimConnector conn) {
		Uid uid = null;

		if (USERS.equals(resourceName)) {
			uid = conn.create(userClass, userCreateBuilder(testNumber), null);
		} else if (GROUPS.equals(resourceName)) {
			uid = conn.create(groupClass, groupCreateBuilder(testNumber), null);
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;
	}

	protected static Set<Attribute> userMultiValUpdateBuilder(Integer testNumber) {

		StringBuilder buildUpdateEmailAdress = new StringBuilder(testNumber.toString())
				.append("testupdateuser@testdomain.com");

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("emails.default.value", buildUpdateEmailAdress.toString()));
		attributeSet.add(AttributeBuilder.build("emails.default.primary", true));

		return attributeSet;
	}

	public static List<ConnectorObject> filter(String filterType, String resourceName, Integer testNumber,
			Uid userTestUid, Uid groupTestUid, ScimConnector conn, OperationOptions options) {

		TestSearchResultsHandler handler = new TestSearchResultsHandler();

		Filter filter = getFilter(filterType, resourceName, testNumber, userTestUid, groupTestUid);

			if (USERS.equalsIgnoreCase(resourceName)) {
				conn.executeQuery(userClass, filter, handler, options);
			} else if (GROUPS.equalsIgnoreCase(resourceName)) {
				conn.executeQuery(groupClass, filter, handler, options);
			} else {
				LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
			}

		return handler.getResult();
	}

	private static Filter getFilter(String filterType, String resourceName, Integer testNumber,
			Uid userTestUid, Uid groupTestUid) {
		Filter filter = null;
		StringBuilder idName = new StringBuilder(testNumber.toString()).append("Test").append(" ").append("Group");
		if ("contains".equalsIgnoreCase(filterType)) {
			if (USERS.equals(resourceName)) {
				filter = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build(USERNAME, testNumber.toString()));
			} else if (GROUPS.equals(resourceName)) {

				filter = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build(DISPLAYNAME, testNumber.toString()));
			}
		} else if ("equals".equalsIgnoreCase(filterType)) {
	
			if (USERS.equals(resourceName)) {
				
				filter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(USERNAME, testNumber.toString()));
			} else if (GROUPS.equals(resourceName)) {
				
				filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build(DISPLAYNAME, idName.toString()));
			}
		} else if ("uid".equalsIgnoreCase(filterType)) {
			if (USERS.equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder.equalTo(userTestUid);
			} else if (GROUPS.equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder.equalTo(groupTestUid);
			}
		} else if ("startswith".equalsIgnoreCase(filterType)) {
			if (USERS.equals(resourceName)) {

				filter = (StartsWithFilter) FilterBuilder
						.startsWith(AttributeBuilder.build(USERNAME, testNumber.toString()));
			} else if (GROUPS.equals(resourceName)) {

				filter = (StartsWithFilter) FilterBuilder
						.startsWith(AttributeBuilder.build(DISPLAYNAME, testNumber.toString()));
			}
			// TODO check
		}else if ("or".equalsIgnoreCase(filterType)) {
			if (USERS.equals(resourceName)) {

				Filter leftFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(USERNAME, testNumber.toString()));
				
				Filter rightFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(USERNAME, "test"));
				
				filter = (OrFilter) FilterBuilder.or(leftFilter, rightFilter);
				
			} else if (GROUPS.equals(resourceName)) {
				
				Filter leftFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(DISPLAYNAME, idName.toString()));
				
				Filter rightFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(DISPLAYNAME, "test"));
				
				filter = (OrFilter) FilterBuilder.or(leftFilter, rightFilter);
			}
			// TODO check
		}else if ("and".equalsIgnoreCase(filterType)) {
			if (USERS.equals(resourceName)) {

				Filter leftFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(USERNAME, testNumber.toString()));
				
				Filter rightFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(USERNAME, testNumber.toString()));
				
				filter = (AndFilter) FilterBuilder.and(leftFilter, rightFilter);
				
			} else if (GROUPS.equals(resourceName)) {

				Filter leftFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(DISPLAYNAME, idName.toString()));
				
				Filter rightFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(DISPLAYNAME,idName.toString()));
				
				filter = (AndFilter) FilterBuilder.and(leftFilter, rightFilter);
			}
			// TODO check
		} else if ("containsall".equalsIgnoreCase(filterType)) {
			if (GROUPS.equals(resourceName)) {
				filter = (ContainsAllValuesFilter) FilterBuilder
						.containsAllValues(AttributeBuilder.build(MEMBERSDEFAULT, userTestUid.getUidValue()));
			}
		} else if ("userequals".equalsIgnoreCase(filterType)) {
			if (GROUPS.equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build(MEMBERSDEFAULT, userTestUid.getUidValue()));
			}
		}

		return filter;
	}

	public Set<Attribute> getAttributeSet(String resourceName, Integer testNumber) {

		Set<Attribute> attributeSet = new HashSet<>();

		if (USERS.equals(resourceName)) {
			attributeSet = userCreateBuilder(testNumber);

		} else if (GROUPS.equals(resourceName)) {

			attributeSet = groupCreateBuilder(testNumber);
		}

		return attributeSet;
	}

	public static Map<String, String> processResult(List<ConnectorObject> result2, String resourceName, String testType,
			Uid userTestUid, Integer testNumber) {

		Map<String, String> evaluationResult = new HashMap<String, String>();

		Set<Attribute> createAttributeSet = new HashSet<Attribute>();
		String notPressentAttribute = "";
		String createAttributeName;

		if (USERS.equals(resourceName)) {
			if (CREATE.equals(testType)) {
				createAttributeSet = userCreateBuilder(testNumber);
			} else if (UPDATESINGLE.equals(testType)) {
				createAttributeSet = userSingleValUpdateBuilder(testNumber);
			} else if (UPDATEMULTI.equals(testType)) {
				createAttributeSet = userMultiValUpdateBuilder(testNumber);
			} else if ("update-disabled".equals(testType)) {
				createAttributeSet = userDisableUpdate();
			} else if ("update-enabled".equals(testType)) {
				createAttributeSet = userEnableUpdate();
			}
			notPressentAttribute = USERNAME;
		} else if (GROUPS.equals(resourceName)) {
			if (CREATE.equals(testType)) {
				createAttributeSet = groupCreateBuilder(testNumber);
			} else if (UPDATESINGLE.equals(testType)) {
				createAttributeSet = groupSingleValUpdateBuilder(testNumber);
			} else if (UPDATEMULTI.equals(testType)) {
				groupMultiValUpdateBuilder(testNumber, userTestUid);
			}
			notPressentAttribute = DISPLAYNAME;
		}

		for (Attribute createAttribute : createAttributeSet) {
			createAttributeName = createAttribute.getName();

			if (!notPressentAttribute.equals(createAttributeName)) {
				evaluationResult.put(createAttributeName, "#AttributeNameNotFound#");
			}
		}
		for (ConnectorObject result : result2) {
			Set<Attribute> returnedAttributeSet = new HashSet<Attribute>();

			returnedAttributeSet = result.getAttributes();

			for (Attribute attribute : returnedAttributeSet) {

				String returnedAttributeName = attribute.getName();
				LOGGER.info("The attribute Name: {0}", returnedAttributeName);

				for (Attribute createAttribute : createAttributeSet) {
					createAttributeName = createAttribute.getName();

					if (createAttributeName.equals(returnedAttributeName)) {

						if (createAttribute.getValue().equals(attribute.getValue())) {

							evaluationResult.replace(createAttributeName, returnedAttributeName);
							break;

						} else {

							evaluationResult.replace(createAttributeName,
									"The returned value does not correspond to the value which vas set");
						}
					}

				}
			}

		}

		return evaluationResult;
	}

}
