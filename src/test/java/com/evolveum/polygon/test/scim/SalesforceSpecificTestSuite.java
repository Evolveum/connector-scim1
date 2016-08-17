package com.evolveum.polygon.test.scim;

import java.util.ArrayList;
import java.util.HashMap;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.scim.ScimConnectorConfiguration;

public class SalesforceSpecificTestSuite {

	private static Uid userUid;
	private static Uid groupUid;

	private static Integer pageSize;

	private static Integer pageOffset;

	private static Integer testNumber = 0;

	private static ScimConnector connector;

	private static ScimConnectorConfiguration configuration;

	private static final Log LOGGER = Log.getLog(SalesforceSpecificTestSuite.class);

	@DataProvider(name = "filterMethodTestProvider")
	public static Object[][] filterMethodTestResourcesProvider() {

		// TODO test issues with eq filter slack

		return new Object[][] { { "users", "uid" },{ "groups", "uid" }, { "users", "contains" }, { "groups", "contains" },
				 { "users", "startswith" }, { "groups", "startswith" },{ "users", "endswith" }, { "groups", "endswith" },
				{ "users", "equals" }, { "groups", "equals" }, { "groups", "containsall" } };
	}

	@DataProvider(name = "updateUserResourceObjectTestProvider")
	public static Object[][] updateUserResourceObjectTestResourceProvider() throws Exception {
		Uid uid = getUid("user");

		return new Object[][] { { "single", uid }, { "multi", uid }, { "disabled", uid }, { "enabled", uid } };
	}

	@DataProvider(name = "updateGroupResourceObjectTestProvider")
	public static Object[][] updateGroupResourceObjectTestResourceProvider() throws Exception {
		Uid uid = getUid("group");

		return new Object[][] { { "single", uid }, { "multi", uid } };
	}

	@DataProvider(name = "listAllfromResourcesProvider")
	public static Object[][] listAllfromResourcesProvider() {
		return new Object[][] { { 1, "users" }, { 1, "groups" } };
	}

	@DataProvider(name = "parameterConsistencyTestProvider")
	public static Object[][] parameterConsistencyTestProvider() {

		return new Object[][] { { "groups", "uid" }, { "users", "uid" } };
	}

	@DataProvider(name = "deletetObjectfromResourcesProvider")
	public static Object[][] deletetObjectfromResourcesResourceProvider() {

		return new Object[][] { { "users", userUid }, { "groups", groupUid } };
	}

	@DataProvider(name = "createTestProvider")
	public static Object[][] createTestResourceProvider() {

		return new Object[][] { { "users", true }, { "groups", true } };
	}

	@DataProvider(name = "configTestProvider")
	public static Object[][] configurationTestResourcesProvider() {

		pageSize = 1;
		pageOffset = 1;

		testNumber = 1;

		HashMap<String, String> configurationParameters = new HashMap<String, String>();
		configurationParameters.put("clientID", "**");
		configurationParameters.put("clientSecret", "**");
		configurationParameters.put("endpoint", "**");
		configurationParameters.put("loginUrl", "");
		configurationParameters.put("password", "**");
		configurationParameters.put("service", "**");
		configurationParameters.put("userName", "**");
		configurationParameters.put("version", "**");
		configurationParameters.put("authentication", "**");
		configurationParameters.put("proxy", "**");
		configurationParameters.put("proxy_port_number", "**");

		return new Object[][] { { configurationParameters, true } };
	}

	@Test(priority = 1, dataProvider = "configTestProvider")
	public void configurationTest(HashMap<String, String> configurationParameters, Boolean assertionVariable) {

		groupUid = null;
		userUid = null;

		configuration = StandardScimTestUtils.buildConfiguration(configurationParameters);

		Boolean isValid = StandardScimTestUtils.isConfigurationValid(configuration);

		if (isValid) {

			connector = new ScimConnector();
			connector.init(configuration);
		}

		Assert.assertEquals(isValid, assertionVariable);

	}

	@Test(priority = 2, dependsOnMethods = { "configurationTest" }, dataProvider = "createTestProvider")
	private void createObjectOnResourcesTest(String resourceName, Boolean assertParameter) {

		Boolean resourceWasCreated = false;


		if ("users".equals(resourceName)) {
			userUid = StandardScimTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (userUid != null) {
				resourceWasCreated = true;
			}
		} else if ("groups".equals(resourceName)) {

			groupUid = StandardScimTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (groupUid != null) {
				resourceWasCreated = true;
			}
		}

		if (userUid != null) {
			resourceWasCreated = true;
		}

		Assert.assertEquals(resourceWasCreated, assertParameter);

	}

	@Test(priority = 2, dependsOnMethods = {
			"createObjectOnResourcesTest" }, dataProvider = "parameterConsistencyTestProvider")
	private void parameterConsistencyTest(String resourceName, String filterType) {

		StringBuilder testType = new StringBuilder("createObject");

		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		result = StandardScimTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector, options);

		HashMap<String, String> evaluationResults = StandardScimTestUtils.processResult(result, resourceName,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

	}

	@Test(priority = 6, dependsOnMethods = { "createObjectOnResourcesTest" }, dataProvider = "filterMethodTestProvider")
	public void filterMethodTest(String resourceName, String filterType) {

		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = StandardScimTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Assert.assertFalse(returnedObjects.isEmpty());

	}

	@Test(priority = 5, dependsOnMethods = {
			"createObjectOnResourcesTest" }, dataProvider = "listAllfromResourcesProvider")
	private void listAllfromResourcesTest(int numberOfResources, String resourceName) {
		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = StandardScimTestUtils.listAllfromResourcesTestUtil(resourceName, connector, options);

		Assert.assertEquals(returnedObjects.size(), numberOfResources);

	}

	@Test(priority = 3, dependsOnMethods = {
			"createObjectOnResourcesTest" }, dataProvider = "updateUserResourceObjectTestProvider")
	private void updateUserResourceObjectTest(String updateType, Uid uid) {

		Uid returnedUid = StandardScimTestUtils.updateResourceTest("users", updateType, userUid, groupUid, testNumber,
				connector);

		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		result = StandardScimTestUtils.filter("uid", "users", testNumber, userUid, groupUid, connector, options);

		HashMap<String, String> evaluationResults = StandardScimTestUtils.processResult(result, "users", testType.toString(),
				userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 4, dependsOnMethods = {
			"createObjectOnResourcesTest" }, dataProvider = "updateGroupResourceObjectTestProvider")
	private void updateGroupResourceObjectTest(String updateType, Uid uid) {

		Uid returnedUid = StandardScimTestUtils.updateResourceTest("groups", updateType, userUid, groupUid, testNumber,
				connector);
		
		
		ArrayList<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		result = StandardScimTestUtils.filter("uid", "groups", testNumber, userUid, groupUid, connector, options);

		HashMap<String, String> evaluationResults = StandardScimTestUtils.processResult(result, "groups", testType.toString(),
				userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}
		

		Assert.assertEquals(uid, returnedUid);

	}

	// @AfterMethod
	private void cleanup(ITestResult result) {
		if (result.getStatus() == ITestResult.FAILURE) {
			if (userUid != null) {
				LOGGER.warn("Atempting to delete resource: {0}", "users");
				deleteObjectfromResourcesTest("users", userUid);
			} else if (groupUid != null) {
				LOGGER.warn("Atempting to delete resource: {0}", "groups");
				deleteObjectfromResourcesTest("groups", groupUid);
			} else {
				LOGGER.warn(
						"Test failure, uid values of resource objects are null. No resource deletion operation was atempted");
			}
		}

	}

	@Test(priority = 7, dependsOnMethods = {
			"createObjectOnResourcesTest" }, dataProvider = "deletetObjectfromResourcesProvider")
	private void deleteObjectfromResourcesTest(String resourceName, Uid uid) {

		ArrayList<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		StandardScimTestUtils.deleteResourceTestHelper(resourceName, uid, connector);
		returnedObjects = StandardScimTestUtils.filter("uid", resourceName, testNumber, userUid, groupUid, connector, options);

		Assert.assertTrue(returnedObjects.isEmpty());

	}

	public static Uid getUid(String resourceName) throws Exception {
		Uid uid = null;

		if ("user".equals(resourceName)) {
			uid = userUid;

		} else if ("group".equals(resourceName)) {

			uid = groupUid;
		} else {
			LOGGER.warn("Resource name not defined: {0}", resourceName);
		}

		if (uid == null) {
			throw new Exception("Uid not set");
		}
		return uid;
	}

}
