package com.evolveum.polygon.test.scim;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

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

public class SlackSpecificTestSuite extends StandardScimTestSuite {

	private Uid userUid;
	private Uid groupUid;

	private Integer pageSize;

	private Integer pageOffset;

	private Integer testNumber = 0;

	private ScimConnector connector;

	private ScimConnectorConfiguration configuration;

	private final static Log LOGGER = Log.getLog(SlackSpecificTestSuite.class);

	public PropertiesParser parser = new PropertiesParser("../ConnIdScimConnector/testProperties/slackTest.properties");

	@DataProvider(name = "configTestProvider")
	public Object[][] configurationResourcesProvider() {

		int width = 2;

		List<String> nonConnectionParameters = new ArrayList<String>();

		nonConnectionParameters.add("pageSize");
		nonConnectionParameters.add("pageOffset");
		nonConnectionParameters.add("testNumber");

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

		configuration = SlackSpecificTestUtils.buildConfiguration(configurationParameters);

		Boolean isValid = SlackSpecificTestUtils.isConfigurationValid(configuration);

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
			userUid = SlackSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (userUid != null) {
				resourceWasCreated = true;
			}
		} else if ("groups".equals(resourceName)) {

			groupUid = SlackSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
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

		LOGGER.info("Processing trough the \"parameter consistency\" for the resource: \"{0}\"", resourceName);

		StringBuilder testType = new StringBuilder("createObject");

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SlackSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Map<String, String> evaluationResults = SlackSpecificTestUtils.processResult(result, resourceName,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

	}

	@Test(priority = 6, dependsOnMethods = { "createObjectTest" }, dataProvider = "filterMethodProvider")
	public void filterMethodTest(String filterType, String resourceName) {

		LOGGER.info(
				"Processing trought the \"filter methods test\" for the resource \"{0}\" and the evaluated filter is \"{1}\" ",
				resourceName, filterType);

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SlackSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertFalse(returnedObjects.isEmpty());

	}

	@Test(priority = 5, dependsOnMethods = { "createObjectTest" }, dataProvider = "listAllFromResourcesProvider")
	private void listAllTest(String resourceName, int numberOfResources) {
		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SlackSpecificTestUtils.listAllfromResourcesTestUtil(resourceName, connector, options);

		Assert.assertEquals(returnedObjects.size(), numberOfResources);

	}

	@Test(priority = 3, dependsOnMethods = { "createObjectTest" }, dataProvider = "updateUserProvider")
	private void updateUserTest(String updateType, Uid uid) {

		Uid returnedUid = SlackSpecificTestUtils.updateResourceTest("users", updateType, userUid, groupUid, testNumber,
				connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SlackSpecificTestUtils.filter("uid", "users", testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = SlackSpecificTestUtils.processResult(result, "users",
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 4, dependsOnMethods = { "createObjectTest" }, dataProvider = "updateGroupProvider")
	private void updateGroupTest(String updateType, Uid uid) {

		LOGGER.info("Processing trought the \"updateGroupTest\" and the \"{0}\" update type.", updateType);

		Uid returnedUid = SlackSpecificTestUtils.updateResourceTest("groups", updateType, userUid, groupUid, testNumber,
				connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder("update").append("-").append(updateType);

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SlackSpecificTestUtils.filter("uid", "groups", testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = SlackSpecificTestUtils.processResult(result, "groups",
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 7, dependsOnMethods = { "createObjectTest" }, dataProvider = "deleteProvider")
	private void deleteObjectTest(String resourceName, Uid uid) {

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		SlackSpecificTestUtils.deleteResourceTestHelper(resourceName, uid, connector);
		returnedObjects = SlackSpecificTestUtils.filter("uid", resourceName, testNumber, userUid, groupUid, connector,
				options);

		Assert.assertTrue(returnedObjects.isEmpty());

	}

	public Uid getUid(String resourceName) throws Exception {
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

	public static Object[][] fetchTestData(String testName) {

		boolean isSearchedElement = false;
		boolean boundariesWereSet = false;
		String value = "";
		String startName = "";
		int iterator = 0;
		int lenght = 0;
		int width = 0;
		Object[][] configurationObject = null;

		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {
			XMLEventReader eventReader = factory.createXMLEventReader(
					new InputStreamReader(new FileInputStream("slackTestData.xml"), StandardCharsets.UTF_8));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				Integer code = event.getEventType();

				if (boundariesWereSet) {
					configurationObject = new Object[lenght][width];
					boundariesWereSet = false;
				}

				if (code == XMLStreamConstants.START_ELEMENT) {
					StartElement startElement = event.asStartElement();
					startName = startElement.getName().getLocalPart();

					if (testName.equals(startName)) {
						isSearchedElement = true;
					}
				} else if (code == XMLStreamConstants.CHARACTERS) {

					Characters characters = event.asCharacters();
					if (isSearchedElement) {
						if (!characters.isWhiteSpace()) {
							if ("lenght".equals(startName)) {

								value = characters.getData().toString();
								lenght = Integer.parseInt(value);
							} else if ("width".equals(startName)) {
								value = characters.getData().toString();
								width = Integer.parseInt(value);

								boundariesWereSet = true;
							} else {

								value = characters.getData().toString();
								configurationObject[iterator][0] = startName;
								configurationObject[iterator][1] = value;
								iterator++;
							}
						}
					}

				} else if (code == XMLStreamConstants.END_ELEMENT) {
					EndElement endElement = event.asEndElement();

					if ("filterMethodProvider".equals(endElement.getName().getLocalPart())) {

						isSearchedElement = false;

					}
					/*
					 * 
					 * for(int a = 0;a <configurationObject.length; a++){
					 * for(int b = 0; b<configurationObject[a].length; b++){
					 * LOGGER.info(
					 * "Parsed data possition L:{0}, W:{1}, the value: {2}",
					 * a,b,configurationObject[a][b]);
					 * 
					 * }
					 * 
					 * 
					 * } }
					 */

				}
			}

		} catch (FileNotFoundException e) {
			LOGGER.error("File not found: {0}", e.getLocalizedMessage());
			e.printStackTrace();
		} catch (XMLStreamException e) {
			LOGGER.error("XML stream exception has occured: {0}", e.getLocalizedMessage());
			e.printStackTrace();
		}

		return configurationObject;
	}

	public PropertiesParser getParser() {

		return parser;
	}

}
