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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.scim.ScimConnectorConfiguration;
/**
 * 
 * @author Macik
 *
 */
public class StandardScimTestUtils {

	protected static final String USERNAME = "userName";
	protected static final String NICKNAME = "nickName";
	protected static final String USERS = "users";
	protected static final String GROUPS = "groups";
	protected static final String SINGLE = "single";
	protected static final String MULTI = "multi";
	protected static final String UPDATESINGLE = "update-single";
	protected static final String UPDATEMULTI = "update-multi";
	protected static final String CREATE = "createObject";

	protected static final String EMAILWORKVALUE = "emails.work.value";
	protected static final String EMAILWORKPRIMARY = "emails.work.primary";
	protected static final String FAMILYNAME = "name.familyName";
	protected static final String DISPLAYNAME = "displayName";
	protected static final String MEMBERSDEFAULT = "members.default.value";

	private static final Log LOGGER = Log.getLog(StandardScimTestUtils.class);

	protected static final ObjectClass userClass = ObjectClass.ACCOUNT;
	protected static final ObjectClass groupClass = ObjectClass.GROUP;

	public static ScimConnectorConfiguration buildConfiguration(HashMap<String, String> configuration) {
		ScimConnectorConfiguration scimConnectorConfiguration = new ScimConnectorConfiguration();

		for (String configurationParameter : configuration.keySet()) {

			if ("clientID".equals(configurationParameter)) {
				scimConnectorConfiguration.setClientID(configuration.get(configurationParameter));
			} else if ("clientSecret".equals(configurationParameter)) {
				scimConnectorConfiguration.setClientSecret(configuration.get(configurationParameter));
			} else if ("endpoint".equals(configurationParameter)) {
				scimConnectorConfiguration.setEndpoint(configuration.get(configurationParameter));
			} else if ("loginUrl".equals(configurationParameter)) {
				scimConnectorConfiguration.setLoginURL(configuration.get(configurationParameter));
			} else if ("password".equals(configurationParameter)) {
				scimConnectorConfiguration.setPassword(configuration.get(configurationParameter));
			} else if ("service".equals(configurationParameter)) {
				scimConnectorConfiguration.setService(configuration.get(configurationParameter));
			} else if (USERNAME.equals(configurationParameter)) {
				scimConnectorConfiguration.setUserName(configuration.get(configurationParameter));
			} else if ("version".equals(configurationParameter)) {
				scimConnectorConfiguration.setVersion(configuration.get(configurationParameter));
			} else if ("authentication".equals(configurationParameter)) {
				scimConnectorConfiguration.setAuthentication(configuration.get(configurationParameter));
			} else if ("baseurl".equals(configurationParameter)) {
				scimConnectorConfiguration.setBaseUrl(configuration.get(configurationParameter));
			} else if ("token".equals(configurationParameter)) {
				scimConnectorConfiguration.setToken(configuration.get(configurationParameter));
			} else if ("proxy".equals(configurationParameter)) {
				scimConnectorConfiguration.setProxyUrl(configuration.get(configurationParameter));
			} else if ("proxy_port_number".equals(configurationParameter)) {
				String parameterValue = configuration.get(configurationParameter);
				if (!parameterValue.isEmpty()) {
					Integer portNumber = Integer.parseInt(parameterValue);
					scimConnectorConfiguration.setProxyPortNumber(portNumber);
				} else
					scimConnectorConfiguration.setProxyPortNumber(null);
			} else {

				LOGGER.warn("Occurrence of an non defined parameter");
			}
		}
		return scimConnectorConfiguration;

	}

	private static Set<Attribute> userCreateBuilder(Integer testNumber) {

		StringBuilder testAttributeString = new StringBuilder();

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		testAttributeString.append(testNumber.toString()).append("testusertestuser@testdomain.com");

		attributeSet.add(AttributeBuilder.build(USERNAME, testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build(NICKNAME, testAttributeString.toString()));

		attributeSet.add(AttributeBuilder.build(EMAILWORKVALUE, testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build(EMAILWORKPRIMARY, true));

		attributeSet.add(AttributeBuilder.build("title", "Mr."));
		attributeSet.add(AttributeBuilder.build(FAMILYNAME, "User"));
		attributeSet.add(AttributeBuilder.build("name.givenName", "Test"));

		attributeSet.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, true));

		return attributeSet;
	}

	protected static Set<Attribute> userSingleValUpdateBuilder(Integer testNumber) {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(NICKNAME, testNumber.toString()));

		attributeSet.add(AttributeBuilder.build(FAMILYNAME, "TestUpdate"));

		return attributeSet;

	}

	protected static Set<Attribute> userMultiValUpdateBuilder(Integer testNumber) {

		StringBuilder buildUpdateEmailAdress = new StringBuilder(testNumber.toString())
				.append("testupdateuser@testdomain.com");

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(EMAILWORKVALUE, buildUpdateEmailAdress.toString()));
		attributeSet.add(AttributeBuilder.build(EMAILWORKPRIMARY, false));

		return attributeSet;
	}

	protected static Set<Attribute> userEnableUpdate() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, true));

		return attributeSet;
	}

	protected static Set<Attribute> userDisableUpdate() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, false));

		return attributeSet;
	}

	protected static Set<Attribute> groupCreateBuilder(Integer testNumber) {

		StringBuilder testAttributeString = new StringBuilder();

		testAttributeString.append(testNumber.toString()).append("TestGroup");

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(DISPLAYNAME, testAttributeString.toString()));

		return attributeSet;
	}

	protected static Set<Attribute> groupSingleValUpdateBuilder(Integer testNumber) {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(DISPLAYNAME, testNumber.toString()));

		return attributeSet;
	}

	protected static Set<Attribute> groupMultiValUpdateBuilder(Integer testNumber, Uid userTestUid) {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build(MEMBERSDEFAULT, userTestUid.getUidValue()));
		return attributeSet;
	}

	public static List<ConnectorObject> listAllfromResourcesTestUtil(String resourceName, ScimConnector conn,
			OperationOptions options) {

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		TestSearchResultsHandler handler = new TestSearchResultsHandler();

		if (USERS.equalsIgnoreCase(resourceName)) {
			conn.executeQuery(userClass, null, handler, options);

		} else if (GROUPS.equalsIgnoreCase(resourceName)) {
			conn.executeQuery(groupClass, null, handler, options);

		} else {

			LOGGER.warn("Resource not supported", resourceName);
			throw new ConnectorException("Resource not supported");
		}

		returnedObjects = handler.getResult();

		return returnedObjects;
	}

	public static OperationOptions getOptions(Integer pageSize, Integer pageOffset) {

		Map<String, Object> operationOptions = new HashMap<String, Object>();

		operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
		operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, pageOffset);
		operationOptions.put(OperationOptions.OP_PAGE_SIZE, pageSize);

		OperationOptions options = new OperationOptions(operationOptions);

		return options;
	}

	public static void deleteResourceTestHelper(String resourceName, Uid uid, ScimConnector conn) {

		if (USERS.equalsIgnoreCase(resourceName)) {
			conn.delete(userClass, uid, null);

		} else if (GROUPS.equalsIgnoreCase(resourceName)) {
			conn.delete(groupClass, uid, null);

		} else {

			LOGGER.warn("Resource not supported", resourceName);
			throw new ConnectorException("Resource not supported");
		}

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

		// TODO test negative in salesforce/ free-plan
		// conn.create(entitlementClass, BuilderTestResource(), null);
		return uid;
	}

	public static Uid updateResourceTest(String resourceName, String updateType, Uid userTestUid, Uid groupTestUid,
			Integer testNumber, ScimConnector conn) {
		Uid uid = null;

		if (USERS.equals(resourceName)) {
			if (SINGLE.equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userSingleValUpdateBuilder(testNumber), null);
			} else if (MULTI.equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userMultiValUpdateBuilder(testNumber), null);

			} else if ("enabled".equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userEnableUpdate(), null);

			} else if ("disabled".equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userDisableUpdate(), null);

			}

		} else if (GROUPS.equals(resourceName)) {
			if (SINGLE.equals(updateType)) {

				uid = conn.update(groupClass, groupTestUid, groupSingleValUpdateBuilder(testNumber), null);
			} else if (MULTI.equals(updateType)) {
				uid = conn.update(groupClass, groupTestUid, groupMultiValUpdateBuilder(testNumber, userTestUid), null);

			}
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

	}

	public Uid addAttributeValuesTestHelper(String resourceName, Uid testUid, Integer testNumber, ScimConnector conn) {
		Uid uid = null;

		if (USERS.equals(resourceName)) {
			uid = conn.update(userClass, testUid, userSingleValUpdateBuilder(testNumber), null);
		} else if (GROUPS.equals(resourceName)) {
			uid = conn.update(groupClass, testUid, groupCreateBuilder(testNumber), null);
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

	}

	public Uid removeAttributeValuesTestHelper(String resourceName, Uid testUid, Integer testNumber,
			ScimConnector conn) {
		Uid uid = null;

		if (USERS.equals(resourceName)) {
			uid = conn.update(userClass, testUid, userSingleValUpdateBuilder(testNumber), null);
		} else if (GROUPS.equals(resourceName)) {
			uid = conn.update(groupClass, testUid, groupCreateBuilder(testNumber), null);
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

	}

	public static List<ConnectorObject> filter(String filterType, String resourceName, Integer testNumber,
			Uid userTestUid, Uid groupTestUid, ScimConnector conn, OperationOptions options) {

		TestSearchResultsHandler handler = new TestSearchResultsHandler();

		Filter filter = getFilter(filterType, resourceName, testNumber, userTestUid, groupTestUid);

		try {
			if (USERS.equalsIgnoreCase(resourceName)) {
				conn.executeQuery(userClass, filter, handler, options);
			} else if (GROUPS.equalsIgnoreCase(resourceName)) {
				conn.executeQuery(groupClass, filter, handler, options);
			} else {
				LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
			}

		} catch (Exception e) {
			LOGGER.warn("An exception has occurred while processing the filter method test: {0}", e.getMessage());
			;
		}

		return handler.getResult();
	}

	private static AttributeFilter getFilter(String filterType, String resourceName, Integer testNumber,
			Uid userTestUid, Uid groupTestUid) {
		AttributeFilter filter = null;

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
						.equalTo(AttributeBuilder.build(DISPLAYNAME, testNumber.toString()));
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

		} else if ("endswith".equalsIgnoreCase(filterType)) {
			if (USERS.equals(resourceName)) {
				filter = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build(USERNAME, "testdomain.com"));
			} else if (GROUPS.equals(resourceName)) {
				filter = (EndsWithFilter) FilterBuilder
						.endsWith(AttributeBuilder.build(DISPLAYNAME, testNumber.toString()));
			}
		} else if ("containsall".equalsIgnoreCase(filterType)) {
			if (GROUPS.equals(resourceName)) {
				filter = (ContainsAllValuesFilter) FilterBuilder
						.containsAllValues(AttributeBuilder.build(MEMBERSDEFAULT, userTestUid.getUidValue()));
			}
		}

		return filter;
	}

	public static boolean isConfigurationValid(ScimConnectorConfiguration connectorConfiguration) {

		try {
			connectorConfiguration.validate();
		} catch (Exception e) {
			return false;
		}
		return true;
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

	public static Map<String, String> processResult(List<ConnectorObject> results, String resourceName, String testType,
			Uid userTestUid, Integer testNumber) {

		Map<String, String> evaluationResult = new HashMap<String, String>();

		Set<Attribute> createAttributeSet = new HashSet<Attribute>();

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

		} else if (GROUPS.equals(resourceName)) {
			if (CREATE.equals(testType)) {
				createAttributeSet = groupCreateBuilder(testNumber);
			} else if (UPDATESINGLE.equals(testType)) {
				createAttributeSet = groupSingleValUpdateBuilder(testNumber);
			} else if (UPDATEMULTI.equals(testType)) {
				groupMultiValUpdateBuilder(testNumber, userTestUid);
			}
		}

		for (Attribute createAttribute : createAttributeSet) {
			createAttributeName = createAttribute.getName();

			evaluationResult.put(createAttributeName, "#AttributeNameNotFound#");
		}
		for (ConnectorObject result : results) {
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
