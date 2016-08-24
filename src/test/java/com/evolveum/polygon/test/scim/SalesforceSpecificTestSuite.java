package com.evolveum.polygon.test.scim;

import java.util.ArrayList;
import java.util.HashMap;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.scim.ScimConnectorConfiguration;

public class SalesforceSpecificTestSuite extends StandardScimTestSuite {

	private static Uid userUid;
	private static Uid groupUid;
	private static Uid entitlementUid = new Uid("00e58000000cqxLAAQ");

	private static Integer pageSize;

	private static Integer pageOffset;

	private static Integer testNumber = 0;

	private static ScimConnector connector;

	private static ScimConnectorConfiguration configuration;

	private static final Log LOGGER = Log.getLog(SalesforceSpecificTestSuite.class);

	@DataProvider(name = "filterMethodProvider")
	public static Object[][] filterMethodResourcesProvider() {

		return new Object[][] { { "users", "uid" }, { "groups", "uid" }, { "users", "contains" },
				{ "groups", "contains" }, { "users", "startswith" }, { "groups", "startswith" }, { "users", "equals" },
				{ "groups", "equals" }, { "groups", "containsall" } };
	}

	@DataProvider(name = "updateUserProvider")
	public static Object[][] updateUserResourceProvider() throws Exception {
		Uid uid = getUid("user");

		return new Object[][] { { "single", uid }, { "multi", uid }, { "disabled", uid }, { "enabled", uid } };
	}

	@DataProvider(name = "updateGroupProvider")
	public static Object[][] updateGroupResourceProvider() throws Exception {
		Uid uid = getUid("group");

		return new Object[][] { { "single", uid }, { "multi", uid } };
	}

	@DataProvider(name = "listAllfromResourcesProvider")
	public static Object[][] listAllfromResourcesProvider() {
		return new Object[][] { { 1, "users" }, { 1, "groups" }, { 1, "entitlements" } };
	}

	@DataProvider(name = "deleteProvider")
	public static Object[][] deleteResourceProvider() {

		return new Object[][] { { "users", userUid }, { "groups", groupUid } };
	}

	@DataProvider(name = "configTestProvider")
	public static Object[][] configurationResourcesProvider() {

		pageSize = 1;
		pageOffset = 1;

		testNumber = 73;

		HashMap<String, String> configurationParameters = new HashMap<String, String>();

		configurationParameters.put("clientID", "");
		configurationParameters.put("clientSecret", "");
		configurationParameters.put("endpoint", "/services/scim");
		configurationParameters.put("loginUrl", "https://login.salesforce.com");
		configurationParameters.put("password", "");
		configurationParameters.put("service", "");
		configurationParameters.put("userName", "");
		configurationParameters.put("version", "/v1");

		configurationParameters.put("authentication", "**");
		configurationParameters.put("proxy", "");
		configurationParameters.put("proxy_port_number", "");

		return new Object[][] { { configurationParameters, true } };
	}

	@Test(priority = 1, dataProvider = "configTestProvider")
	public void configurationTest(HashMap<String, String> configurationParameters, Boolean assertionVariable) {

		groupUid = null;
		userUid = null;

		configuration = SalesforceSpecificTestUtils.buildConfiguration(configurationParameters);

		Boolean isValid = SalesforceSpecificTestUtils.isConfigurationValid(configuration);

		if (isValid) {

			connector = new ScimConnector();
			connector.init(configuration);
			connector.schema();
		}

		Assert.assertEquals(isValid, assertionVariable);

	}

	@Test(priority = 2, dependsOnMethods = { "configurationTest" }, dataProvider = "createTestProvider")
	private void createObjectTest(String resourceName, Boolean assertParameter) {

		Boolean resourceWasCreated = false;

		if ("users".equals(resourceName)) {
			userUid = SalesforceSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (userUid != null) {
				resourceWasCreated = true;
			}
		} else if ("groups".equals(resourceName)) {

			groupUid = SalesforceSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (groupUid != null) {
				resourceWasCreated = true;
			}
		}

		if (userUid != null) {
			resourceWasCreated = true;
		}

		Assert.assertEquals(resourceWasCreated, assertParameter);

	}

	@Test(priority = 2, dependsOnMethods = { "createObjectTest" }, dataProvider = "parameterConsistencyTestProvider")
	private void parameterConsistencyTest(String resourceName, String filterType) {

		StringBuilder testType = new StringBuilder("createObject");

		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector,
				options);

		HashMap<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, resourceName,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

	}

	@Test(priority = 7, dependsOnMethods = { "createObjectTest" }, dataProvider = "filterMethodProvider")
	public void filterMethodTest(String resourceName, String filterType) {

		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SalesforceSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertFalse(returnedObjects.isEmpty());

	}

	@Test(priority = 6, dependsOnMethods = { "createObjectTest" }, dataProvider = "listAllfromResourcesProvider")
	private void listAllTest(int numberOfResources, String resourceName) {
		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SalesforceSpecificTestUtils.listAllfromResourcesTestUtil(resourceName, connector, options);

		Assert.assertEquals(returnedObjects.size(), numberOfResources);

	}

	@Test(priority = 3, dependsOnMethods = { "createObjectTest" }, dataProvider = "updateUserProvider")
	private void updateUserTest(String updateType, Uid uid) {

		Uid returnedUid = SalesforceSpecificTestUtils.updateResourceTest("users", updateType, userUid, groupUid,
				entitlementUid, testNumber, connector);

		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter("uid", "users", testNumber, userUid, groupUid, connector, options);

		HashMap<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, "users",
				testType.toString(), userUid, testNumber);

		if (!"disabled".equals(updateType)) {
			for (String attributeName : evaluationResults.keySet()) {

				String nameValue = evaluationResults.get(attributeName);

				Assert.assertEquals(nameValue, attributeName);
			}
		}

		Assert.assertEquals(returnedUid, uid);

	}

	@Test(priority = 4, dependsOnMethods = { "createObjectTest" }, dataProvider = "updateGroupProvider")
	private void updateGroupTest(String updateType, Uid uid) {

		Uid returnedUid = SalesforceSpecificTestUtils.updateResourceTest("groups", updateType, userUid, groupUid,
				entitlementUid, testNumber, connector);

		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter("uid", "groups", testNumber, userUid, groupUid, connector, options);

		HashMap<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, "groups",
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(returnedUid, uid);

	}

	@Test(priority = 5, dependsOnMethods = { "createObjectTest" })
	private void updateEntitlemenTest() throws Exception {

		Uid returnedUid = SalesforceSpecificTestUtils.updateResourceTest("entitlements", "multi", userUid, groupUid,
				entitlementUid, testNumber, connector);

		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append("multi");

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter("uid", "entitlements", testNumber, userUid, groupUid, connector,
				options);

		HashMap<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, "entitlements",
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(returnedUid, entitlementUid);

	}

	@Test(priority = 8, dependsOnMethods = { "createObjectTest" }, dataProvider = "deleteProvider")
	private void deleteObjectTest(String resourceName, Uid uid) {

		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		SalesforceSpecificTestUtils.deleteResourceTestHelper(resourceName, uid, connector);
		returnedObjects = SalesforceSpecificTestUtils.filter("uid", resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertTrue(returnedObjects.isEmpty());

	}

	public static Uid getUid(String resourceName) throws Exception {
		Uid uid = null;

		if ("user".equals(resourceName)) {
			uid = userUid;

		} else if ("group".equals(resourceName)) {

			uid = groupUid;
		} else if ("entitlement".equals(resourceName)) {

			uid = entitlementUid;
		} else {
			LOGGER.warn("Resource name not defined: {0}", resourceName);
		}

		if (uid == null) {
			throw new Exception("Uid not set");
		}
		return uid;
	}

	@AfterMethod
	private void cleanup(ITestResult result) throws Exception {
		if (result.getStatus() == ITestResult.FAILURE) {

			String methodThatFailed = result.getMethod().getMethodName();

			if ("createObjectTest".equals(methodThatFailed)) {

				if (userUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", "users");
					deleteObjectTest("users", userUid);
				} else {
					LOGGER.warn(
							"Test failure, uid value of resource \"User\" is null. No resource deletion operation was atempted");
				}
				if (groupUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", "groups");
					deleteObjectTest("groups", groupUid);
				} else

				{
					LOGGER.warn(
							"Test failure, uid value of resource \"Groups\" is null. No resource deletion operation was atempted");
				}

				throw new Exception("Test failure while creating the resource objects, test suite will not continue.");

			} else if ("updateUserResourceObjectTest".equals(methodThatFailed)) {
				if (userUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", "users");
					deleteObjectTest("users", userUid);
				}

				if (groupUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", "groups");
					deleteObjectTest("groups", groupUid);
				}

				throw new Exception(
						"Test failure while updating the \"User\" resource objects, test suite will not continue.");
			}

		}

	}

}
