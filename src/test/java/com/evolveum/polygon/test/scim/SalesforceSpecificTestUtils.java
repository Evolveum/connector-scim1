package com.evolveum.polygon.test.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
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

public class SalesforceSpecificTestUtils {

	private static final Log LOGGER = Log.getLog(SalesforceSpecificTestUtils.class);

	private static final ObjectClass userClass = ObjectClass.ACCOUNT;
	private static final ObjectClass groupClass = ObjectClass.GROUP;
	private static final ObjectClass entitlementClass = new ObjectClass("Entitlements");


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
			} else if ("userName".equals(configurationParameter)) {
				scimConnectorConfiguration.setUserName(configuration.get(configurationParameter));
			} else if ("version".equals(configurationParameter)) {
				scimConnectorConfiguration.setVersion(configuration.get(configurationParameter));
			} else if ("authentication".equals(configurationParameter)) {
				scimConnectorConfiguration.setAuthentication(configuration.get(configurationParameter));
			} else if ("proxy".equals(configurationParameter)) {
				scimConnectorConfiguration.setProxyUrl(configuration.get(configurationParameter));
			} else if ("proxy_port_number".equals(configurationParameter)) {
			Integer portNumber =Integer.parseInt(configuration.get(configurationParameter));	
				scimConnectorConfiguration.setProxyPortNumber(portNumber);
			} else {

				LOGGER.warn("Occurrence of an non defined parameter");
			}
		}
		return scimConnectorConfiguration;

	}

	private static Set<Attribute> userCreateBuilder(Integer testNumber) {

		StringBuilder testAttributeString = new StringBuilder();

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		testAttributeString.append(testNumber.toString()).append("testuser");

		attributeSet.add(AttributeBuilder.build("userName", testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build("nickName", testAttributeString.toString()));

		testAttributeString = new StringBuilder(testNumber.toString()).append("testuser@testdomain.com");
		attributeSet.add(AttributeBuilder.build("emails.work.value", testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build("emails.work.primary", true));
		attributeSet.add(AttributeBuilder.build("nickName", testAttributeString.toString()));

		attributeSet.add(AttributeBuilder.build("title", "Mr."));
		attributeSet.add(AttributeBuilder.build("name.familyName", "User"));
		attributeSet.add(AttributeBuilder.build("name.givenName", "Test"));

		// attributeSet.add(AttributeBuilder.build("entitlements.default.value",
		// "00e58000000qvhqAAA"));

		attributeSet.add(AttributeBuilder.build("__ENABLE__", true));

		return attributeSet;
	}

	private static Set<Attribute> userSingleValUpdateBuilder(Integer testNumber) {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("nickName", testNumber.toString()));

		attributeSet.add(AttributeBuilder.build("name.familyName", "TestUpdate"));

		return attributeSet;

	}

	private static Set<Attribute> userMultiValUpdateBuilder(Integer testNumber) {

		StringBuilder buildUpdateEmailAdress = new StringBuilder(testNumber.toString())
				.append("testupdateuser@testdomain.com");

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("emails.work.value", buildUpdateEmailAdress.toString()));
		attributeSet.add(AttributeBuilder.build("emails.work.primary", false));

		return attributeSet;
	}

	private static Set<Attribute> userEnableUpdate() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("__ENABLE__", true));

		return attributeSet;
	}

	private static Set<Attribute> userDisableUpdate() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("__ENABLE__", false));

		return attributeSet;
	}

	private static Set<Attribute> groupCreateBuilder(Integer testNumber) {

		StringBuilder testAttributeString = new StringBuilder();

		testAttributeString.append(testNumber.toString()).append("TestGroup");

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("displayName", testAttributeString.toString()));

		return attributeSet;
	}

	private static Set<Attribute> groupSingleValUpdateBuilder(Integer testNumber) {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("displayName", testNumber.toString()));

		return attributeSet;
	}

	private static Set<Attribute> groupMultiValUpdateBuilder(Integer testNumber, Uid userTestUid) {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("members.default.value", userTestUid.getUidValue()));
		return attributeSet;
	}


	public static ArrayList<ConnectorObject> listAllfromResourcesTestUtil(String resourceName, ScimConnector conn,
			OperationOptions options) {

		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		TestSearchResultsHandler handler = new TestSearchResultsHandler();


		if ("users".equalsIgnoreCase(resourceName)) {
			conn.executeQuery(userClass, null, handler, options);

		} else if ("groups".equalsIgnoreCase(resourceName)) {
			conn.executeQuery(groupClass, null, handler, options);

		} else if ("entitlements".equalsIgnoreCase(resourceName)) {

			conn.executeQuery(entitlementClass, null, handler, options);
		}

		returnedObjects = handler.getResult();

		return returnedObjects;
	}

	public static OperationOptions getOptions(Integer pageSize, Integer pageOffset) {

		Map<String, Object> operationOptions = new HashMap<String, Object>();

		operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
		operationOptions.put("PAGED_RESULTS_OFFSET", pageOffset);
		operationOptions.put("PAGE_SIZE", pageSize);

		OperationOptions options = new OperationOptions(operationOptions);

		return options;
	}

	public static void deleteResourceTestHelper(String resourceName, Uid uid, ScimConnector conn) {

		if ("users".equalsIgnoreCase(resourceName)) {
			conn.delete(userClass, uid, null);

		} else if ("groups".equalsIgnoreCase(resourceName)) {
			conn.delete(groupClass, uid, null);

		} else {

			LOGGER.warn("Resource not supported", resourceName);
			throw new ConnectorException("Resource not supported");
		}

	}

	public static Uid createResourceTestHelper(String resourceName, Integer testNumber, ScimConnector conn) {
		Uid uid = null;

		if ("users".equals(resourceName)) {
			uid = conn.create(userClass, userCreateBuilder(testNumber), null);
		} else if ("groups".equals(resourceName)) {
			uid = conn.create(groupClass, groupCreateBuilder(testNumber), null);
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}

		// TODO test negative in salesforce/ free-plan
		// conn.create(entitlementClass, BuilderTestResource(), null);
		return uid;
	}

	public static Uid updateResourceTest(String resourceName, String updateType, Uid userTestUid,
			Uid groupTestUid, Integer testNumber, ScimConnector conn) {
		Uid uid = null;

		if ("users".equals(resourceName)) {
			if ("single".equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userSingleValUpdateBuilder(testNumber), null);
			} else if ("multi".equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userMultiValUpdateBuilder(testNumber), null);

			} else if ("enabled".equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userEnableUpdate(), null);

			} else if ("disabled".equals(updateType)) {

				uid = conn.update(userClass, userTestUid, userDisableUpdate(), null);

			}

		} else if ("groups".equals(resourceName)) {
			if ("single".equals(updateType)) {

				uid = conn.update(groupClass, groupTestUid, groupSingleValUpdateBuilder(testNumber), null);
			} else if ("multi".equals(updateType)) {
				uid = conn.update(groupClass, groupTestUid, groupMultiValUpdateBuilder(testNumber, userTestUid), null);

			}
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

		// conn.update(userClass, TEST_UID, updateTestUser(), null);
		// conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
		// conn.update(entitlementClass, ,attr, null);

	}

	public Uid addAttributeValuesTestHelper(String resourceName, Uid testUid, Integer testNumber, ScimConnector conn) {
		Uid uid = null;

		if ("users".equals(resourceName)) {
			uid = conn.update(userClass, testUid, userSingleValUpdateBuilder(testNumber), null);
		} else if ("groups".equals(resourceName)) {
			uid = conn.update(groupClass, testUid, groupCreateBuilder(testNumber), null);
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

		// conn.update(userClass, TEST_UID, updateTestUser(), null);
		// conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
		// conn.update(entitlementClass, ,attr, null);

	}

	public Uid removeAttributeValuesTestHelper(String resourceName, Uid testUid, Integer testNumber,
			ScimConnector conn) {
		Uid uid = null;

		if ("users".equals(resourceName)) {
			uid = conn.update(userClass, testUid, userSingleValUpdateBuilder(testNumber), null);
		} else if ("groups".equals(resourceName)) {
			uid = conn.update(groupClass, testUid, groupCreateBuilder(testNumber), null);
		} else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

		// conn.update(userClass, TEST_UID, updateTestUser(), null);
		// conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
		// conn.update(entitlementClass, ,attr, null);

	}

	public static ArrayList<ConnectorObject> filter(String filterType, String resourceName, Integer testNumber,
			Uid userTestUid, Uid groupTestUid, ScimConnector conn, OperationOptions options) {

		TestSearchResultsHandler handler = new TestSearchResultsHandler();

		Filter filter = getFilter(filterType, resourceName, testNumber, userTestUid, groupTestUid);

		try {
			if ("users".equalsIgnoreCase(resourceName)) {
				conn.executeQuery(userClass, filter, handler, options);
			} else if ("groups".equalsIgnoreCase(resourceName)) {
				conn.executeQuery(groupClass, filter, handler, options);
			} else if ("entitlements".equalsIgnoreCase(resourceName)) {
				conn.executeQuery(entitlementClass, filter, handler, options);
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
			if ("users".equals(resourceName)) {
				filter = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build("userName", testNumber.toString()));
			} else if ("groups".equals(resourceName)) {

				filter = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build("displayName", testNumber.toString()));
			}
		} else if ("equals".equalsIgnoreCase(filterType)) {
			if ("users".equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build("userName", testNumber.toString()));
			} else if ("groups".equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build("displayName", testNumber.toString()));
			}
		} else if ("uid".equalsIgnoreCase(filterType)) {
			if ("users".equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder.equalTo(userTestUid);
			} else if ("groups".equals(resourceName)) {
				filter = (EqualsFilter) FilterBuilder.equalTo(groupTestUid);
			}
		} else if ("startswith".equalsIgnoreCase(filterType)) {
			if ("users".equals(resourceName)) {

				filter = (StartsWithFilter) FilterBuilder
						.startsWith(AttributeBuilder.build("userName", testNumber.toString()));
			} else if ("groups".equals(resourceName)) {

				filter = (StartsWithFilter) FilterBuilder
						.startsWith(AttributeBuilder.build("displayName", testNumber.toString()));
			}
			// TODO not working with salesforce
			// {"Errors":[{"description":"Unsupported Operator :
			// ew","code":400}]}
		} else if ("endswith".equalsIgnoreCase(filterType)) {
			if ("users".equals(resourceName)) {
				filter = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build("userName", "testdomain.com"));
			} else if ("groups".equals(resourceName)) {
				filter = (EndsWithFilter) FilterBuilder
						.endsWith(AttributeBuilder.build("displayName", testNumber.toString()));
			}
		} else if ("containsall".equalsIgnoreCase(filterType)) {
			if ("groups".equals(resourceName)) {
				filter = (ContainsAllValuesFilter) FilterBuilder
						.containsAllValues(AttributeBuilder.build("members.default.value", userTestUid.getUidValue()));
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

		if ("users".equals(resourceName)) {
			attributeSet = userCreateBuilder(testNumber);

		} else if ("groups".equals(resourceName)) {

			attributeSet = groupCreateBuilder(testNumber);
		}

		return attributeSet;
	}

	public static HashMap<String, String> processResult(ArrayList<ConnectorObject> results, String resourceName, String testType, Uid userTestUid, 
			Integer testNumber) {

		HashMap<String, String> evaluationResult = new HashMap<String, String>();

		Set<Attribute> createAttributeSet = new HashSet<Attribute>();

		String createAttributeName;

		if ("users".equals(resourceName)) {
			if("createObject".equals(testType)){
				createAttributeSet = userCreateBuilder(testNumber);
			} else if("update-single".equals(testType)){
				createAttributeSet = userSingleValUpdateBuilder(testNumber);
			}else if("update-multi".equals(testType)){
				createAttributeSet = userMultiValUpdateBuilder(testNumber);
			}else if("update-disabled".equals(testType)){
				createAttributeSet = userDisableUpdate();
			}else if("update-enabled".equals(testType)){
				createAttributeSet = userEnableUpdate();
			}

		} else if ("groups".equals(resourceName)) {
			if("createObject".equals(testType)){
				createAttributeSet = groupCreateBuilder(testNumber);
			}else if("update-single".equals(testType)){
				createAttributeSet = groupSingleValUpdateBuilder(testNumber);
			}else if("update-multi".equals(testType)){
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
