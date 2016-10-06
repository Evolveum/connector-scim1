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

public class StandardScimTestSuite {

	protected static final String PAGESIZE = "pageSize";
	protected static final String PAGEOFFSET = "pageOffset";
	protected static final String TESTNUMBER = "testNumber";
	protected static final String USER = "user";
	protected static final String GROUP = "group";
	protected static final String USERS = "users";
	protected static final String GROUPS = "groups";
	protected static final String UPDATE = "update";
	protected static final String UID = "uid";
	protected static final String MDASH = "-";

	protected static final String CONFIGTESTPROVIDER = "configTestProvider";
	protected static final String FILTERMETHODTESTPROVIDER = "filterMethodProvider";
	protected static final String UPDATEUSERPROVIDER = "updateUserProvider";
	protected static final String UPDATEGROUPPROVIDER = "updateGroupProvider";
	protected static final String LISTALLPROVIDER = "listAllFromResourcesProvider";
	protected static final String CONSISTENCYTESTPROVIDER = "parameterConsistencyTestProvider";
	protected static final String DELETEPROVIDER = "deleteProvider";
	protected static final String CREATEPROVIDER = "createTestProvider";

	protected static final String CREATEOBJECTTEST = "createObjectTest";
	protected static final String CONFIGURATIONTEST = "configurationTest";

	private Uid userUid;
	private Uid groupUid;

	private Integer pageSize;

	private Integer pageOffset;

	private Integer testNumber = 0;

	private ScimConnector connector;

	private ScimConnectorConfiguration configuration;

	private PropertiesParser parser = new PropertiesParser(
			"../ConnIdScimConnector/testProperties/standardTest.properties");

	private final Log LOGGER = Log.getLog(StandardScimTestSuite.class);

	@DataProvider(name = FILTERMETHODTESTPROVIDER)
	public Object[][] filterMethodResourcesProvider() {

		PropertiesParser parser = getParser();

		Object object[][] = parser.fetchTestData(FILTERMETHODTESTPROVIDER);

		return object;
	}

	@DataProvider(name = UPDATEUSERPROVIDER)
	public Object[][] updateUserResourceProvider() throws Exception {
		Uid uid = getUid(USER);

		PropertiesParser parser = getParser();
		Object[][] object = parser.fetchTestData(UPDATEUSERPROVIDER);

		for (int i = 0; i < object.length; i++) {
			object[i][1] = uid;
		}

		return object;
	}

	@DataProvider(name = UPDATEGROUPPROVIDER)
	public Object[][] updateGroupResourceProvider() throws Exception {

		Uid uid = getUid(GROUP);

		PropertiesParser parser = getParser();
		Object[][] object = parser.fetchTestData(UPDATEGROUPPROVIDER);

		for (int i = 0; i < object.length; i++) {
			object[i][1] = uid;

		}

		return object;
	}

	@DataProvider(name = LISTALLPROVIDER)
	public Object[][] listAllFromResourcesProvider() {

		PropertiesParser parser = getParser();
		Object[][] object = parser.fetchTestData(LISTALLPROVIDER);

		for (int i = 0; i < object.length; i++) {
			object[i][1] = 1;

		}
		return object;
	}

	@DataProvider(name = CONSISTENCYTESTPROVIDER)
	public Object[][] parameterConsistencyResourceProvider() {

		PropertiesParser parser = getParser();

		Object object[][] = parser.fetchTestData(CONSISTENCYTESTPROVIDER);

		return object;
	}

	@DataProvider(name = DELETEPROVIDER)
	public Object[][] deleteResourceProvider() throws Exception {

		Uid user = getUid(USER);
		Uid group = getUid(GROUP);

		PropertiesParser parser = getParser();

		Object[][] object = parser.fetchTestData(DELETEPROVIDER);

		for (int i = 0; i < object.length; i++) {
			String parameterName = (String) object[i][0];

			if (parameterName.equals(USERS)) {

				object[i][1] = user;

			} else if (parameterName.equals(GROUPS)) {

				object[i][1] = group;
			}

		}

		return object;
	}

	@DataProvider(name = CREATEPROVIDER)
	public Object[][] createResourceProvider() {

		Boolean notFalse = true;

		PropertiesParser parser = getParser();

		Object[][] object = parser.fetchTestData(DELETEPROVIDER);

		for (int i = 0; i < object.length; i++) {
			String parameterName = (String) object[i][0];

			if (parameterName.equals(USERS)) {

				object[i][1] = notFalse;

			} else if (parameterName.equals(GROUPS)) {

				object[i][1] = notFalse;
			}

		}

		return object;
	}

	@DataProvider(name = CONFIGTESTPROVIDER)
	public Object[][] configurationResourcesProvider() {
		PropertiesParser parser = getParser();

		int width = 2;

		List<String> nonConnectionParameters = new ArrayList<String>();

		nonConnectionParameters.add(PAGESIZE);
		nonConnectionParameters.add(PAGEOFFSET);
		nonConnectionParameters.add(TESTNUMBER);

		Map<String, String> configurationParameters = new HashMap<String, String>();

		Object[][] object = parser.fetchTestData(CONFIGTESTPROVIDER);
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

						if (name.equals(PAGESIZE)) {

							pageSize = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals(PAGEOFFSET)) {
							pageOffset = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals(TESTNUMBER)) {
							testNumber = Integer.parseInt(value);
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

	@Test(priority = 1, dataProvider = CONFIGTESTPROVIDER)
	public void configurationTest(HashMap<String, String> configurationParameters, Boolean assertionVariable) {

		groupUid = null;
		userUid = null;

		configuration = StandardScimTestUtils.buildConfiguration(configurationParameters);

		Boolean isValid = StandardScimTestUtils.isConfigurationValid(configuration);

		if (isValid) {

			connector = new ScimConnector();
			connector.init(configuration);
			connector.test();
			connector.schema();
		}

		Assert.assertEquals(isValid, assertionVariable);

	}

	@Test(priority = 2, dependsOnMethods = { CONFIGURATIONTEST }, dataProvider = CREATEPROVIDER)
	private void createObjectTest(String resourceName, Boolean assertParameter) {

		Boolean resourceWasCreated = false;

		if (USERS.equals(resourceName)) {
			userUid = StandardScimTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (userUid != null) {
				resourceWasCreated = true;
			}
		} else if (GROUPS.equals(resourceName)) {

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

	@Test(priority = 2, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = CONSISTENCYTESTPROVIDER)
	private void parameterConsistencyTest(String resourceName, String filterType) {

		StringBuilder testType = new StringBuilder("createObject");

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		result = StandardScimTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Map<String, String> evaluationResults = StandardScimTestUtils.processResult(result, resourceName,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

	}

	@Test(priority = 6, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = FILTERMETHODTESTPROVIDER)
	public void filterMethodTest(String filterType, String resourceName) {

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = StandardScimTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertFalse(returnedObjects.isEmpty());

	}

	@Test(priority = 5, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = LISTALLPROVIDER)
	private void listAllTest(String resourceName, int numberOfResources) {
		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = StandardScimTestUtils.listAllfromResourcesTestUtil(resourceName, connector, options);

		Assert.assertEquals(returnedObjects.size(), numberOfResources);

	}

	@Test(priority = 3, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = UPDATEUSERPROVIDER)
	private void updateUserTest(String updateType, Uid uid) {

		Uid returnedUid = StandardScimTestUtils.updateResourceTest(USERS, updateType, userUid, groupUid, testNumber,
				connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder(UPDATE).append(MDASH).append(updateType);

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		result = StandardScimTestUtils.filter(UID, USERS, testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = StandardScimTestUtils.processResult(result, USERS, testType.toString(),
				userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 4, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = UPDATEGROUPPROVIDER)
	private void updateGroupTest(String updateType, Uid uid) {

		Uid returnedUid = StandardScimTestUtils.updateResourceTest(GROUPS, updateType, userUid, groupUid, testNumber,
				connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder(UPDATE).append(MDASH).append(updateType);

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		result = StandardScimTestUtils.filter(UID, GROUPS, testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = StandardScimTestUtils.processResult(result, GROUPS, testType.toString(),
				userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 7, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = DELETEPROVIDER)
	private void deleteObjectTest(String resourceName, Uid uid) {

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = StandardScimTestUtils.getOptions(pageSize, pageOffset);

		StandardScimTestUtils.deleteResourceTestHelper(resourceName, uid, connector);
		returnedObjects = StandardScimTestUtils.filter(UID, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Assert.assertTrue(returnedObjects.isEmpty());

	}

	public Uid getUid(String resourceName) throws Exception {
		Uid uid = null;

		if (USER.equals(resourceName)) {
			uid = userUid;

		} else if (GROUP.equals(resourceName)) {

			uid = groupUid;
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

			if (CREATEOBJECTTEST.equals(methodThatFailed)) {

				if (userUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", USERS);
					deleteObjectTest(USERS, userUid);
				} else {
					LOGGER.warn(
							"Test failure, uid value of resource \"User\" is null. No resource deletion operation was atempted");
				}
				if (groupUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", GROUPS);
					deleteObjectTest(GROUPS, groupUid);
				} else

				{
					LOGGER.warn(
							"Test failure, uid value of resource \"Groups\" is null. No resource deletion operation was atempted");
				}

				throw new Exception("Test failure while creating the resource objects, test suite will not continue.");

			} else if ("updateUserTest".equals(methodThatFailed)) {
				if (userUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", USERS);
					deleteObjectTest(USERS, userUid);
				}

				if (groupUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", GROUPS);
					deleteObjectTest(GROUPS, groupUid);
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
