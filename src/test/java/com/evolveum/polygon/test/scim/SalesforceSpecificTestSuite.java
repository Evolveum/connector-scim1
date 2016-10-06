package com.evolveum.polygon.test.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private Uid userUid;
	private Uid groupUid;
	private Uid testEntitlementUid;

	private Integer pageSize;

	private Integer pageOffset;

	private Integer testNumber = 0;

	private ScimConnector connector;

	private ScimConnectorConfiguration configuration;

	private PropertiesParser parser = new PropertiesParser("../ConnIdScimConnector/testProperties/salesforceTest.properties");

	private final Log LOGGER = Log.getLog(SalesforceSpecificTestSuite.class);

	@DataProvider(name = "configTestProvider")
	public Object[][] configurationResourcesProvider() {

		PropertiesParser parser = getParser();

		int width = 2;

		List<String> nonConnectionParameters = new ArrayList<String>();

		nonConnectionParameters.add("pageSize");
		nonConnectionParameters.add("pageOffset");
		nonConnectionParameters.add("testNumber");
		nonConnectionParameters.add("testEntitlementUid");

		Map<String, String> configurationParameters = new HashMap<String, String>();

		Object[][] object = parser.fetchTestData("configTestProvider");
		String name = "";
		String value = "";
		for (int i = 0; i < object.length; i++) {

			for (int j = 0; j < width; j++) {
				if (j == 0) {
					name = (String) object[i][j];
				} else {
					value = (String) object[i][j];
				}
				if (nonConnectionParameters.contains(name)) {

					if (!value.isEmpty()) {

						if (name.equals("pageSize")) {

							pageSize = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals("pageOffset")) {
							pageOffset = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals("testNumber")) {
							testNumber = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals("testEntitlementUid")) {
							testEntitlementUid = new Uid((String) value);
							name = "";
							value = "";
						}

					}

				} else {
					if (!name.isEmpty() && !value.isEmpty()) {
						configurationParameters.put(name, value);
						name = "";
						value = "";
					}

				}

			}

		}
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
			connector.test();
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

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Map<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, resourceName,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

	}

	@Test(priority = 7, dependsOnMethods = { "createObjectTest" }, dataProvider = "filterMethodProvider")
	public void filterMethodTest(String filterType, String resourceName) {

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SalesforceSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertFalse(returnedObjects.isEmpty());

	}

	@Test(priority = 6, dependsOnMethods = { "createObjectTest" }, dataProvider = "listAllFromResourcesProvider")
	private void listAllTest(String resourceName, int numberOfResources) {
		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SalesforceSpecificTestUtils.listAllfromResourcesTestUtil(resourceName, connector, options);

		Assert.assertEquals(returnedObjects.size(), numberOfResources);

	}

	@Test(priority = 3, dependsOnMethods = { "createObjectTest" }, dataProvider = "updateUserProvider")
	private void updateUserTest(String updateType, Uid uid) {

		Uid returnedUid = SalesforceSpecificTestUtils.updateResourceTest("users", updateType, userUid, groupUid,
				testEntitlementUid, testNumber, connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter("uid", "users", testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, "users",
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
				testEntitlementUid, testNumber, connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter("uid", "groups", testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, "groups",
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
				testEntitlementUid, testNumber, connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append("multi");

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SalesforceSpecificTestUtils.filter("uid", "entitlements", testNumber, userUid, groupUid, connector,
				options);

		Map<String, String> evaluationResults = SalesforceSpecificTestUtils.processResult(result, "entitlements",
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(returnedUid, testEntitlementUid);

	}

	@Test(priority = 8, dependsOnMethods = { "createObjectTest" }, dataProvider = "deleteProvider")
	private void deleteObjectTest(String resourceName, Uid uid) {

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SalesforceSpecificTestUtils.getOptions(pageSize, pageOffset);

		SalesforceSpecificTestUtils.deleteResourceTestHelper(resourceName, uid, connector);
		returnedObjects = SalesforceSpecificTestUtils.filter("uid", resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertTrue(returnedObjects.isEmpty());

	}

	public Uid getUid(String resourceName) throws Exception {
		Uid uid = null;

		if ("user".equals(resourceName)) {
			uid = userUid;

		} else if ("group".equals(resourceName)) {

			uid = groupUid;
		} else if ("entitlement".equals(resourceName)) {

			uid = testEntitlementUid;
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

	public PropertiesParser getParser() {

		return parser;
	}

}