package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import com.evolveum.polygon.scim.common.HttpPatch;

public class SalesforceHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SalesforceHandlingStrategy.class);

	@Override
	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException {

		LOGGER.info("Building the connector object from provided json");

		if (resourceJsonObject == null) {
			LOGGER.error(
					"Empty json object was passed from data provider. Error ocourance while building connector object");
			throw new ConnectorException(
					"Empty json object was passed from data provider. Error ocourance while building connector object");
		}

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid(resourceJsonObject.getString("id"));

		if ("Users".equals(resourceEndPoint)) {
			cob.setName(resourceJsonObject.getString("userName"));
		} else if ("Groups".equals(resourceEndPoint)) {

			cob.setName(resourceJsonObject.getString("displayName"));
			cob.setObjectClass(ObjectClass.GROUP);
		} else {
			cob.setName(resourceJsonObject.getString("displayName"));
			ObjectClass objectClass = new ObjectClass(resourceEndPoint);
			;
			cob.setObjectClass(objectClass);

		}
		for (String key : resourceJsonObject.keySet()) {
			Object attribute = resourceJsonObject.get(key);
			if ("meta".equals(key.intern()) || "alias".equals(key.intern()) || "schemas".equals(key.intern())) {

				LOGGER.warn(
						"Processing trought salesforce \"schema inconsistencies\" workaround. Because of the \"{0}\" resoure attribute.",
						key.intern());
			} else

			if (attribute instanceof JSONArray) {

				JSONArray jArray = (JSONArray) attribute;

				Map<String, Collection<Object>> multivaluedAttributeMap = new HashMap<String, Collection<Object>>();
				Collection<Object> attributeValues = new ArrayList<Object>();

				for (Object o : jArray) {
					StringBuilder objectNameBilder = new StringBuilder(key.intern());
					String objectKeyName = "";
					if (o instanceof JSONObject) {
						for (String s : ((JSONObject) o).keySet()) {
							if ("type".equals(s.intern())) {
								objectKeyName = objectNameBilder.append(".").append(((JSONObject) o).get(s)).toString();
								objectNameBilder.delete(0, objectNameBilder.length());
								break;
							}
						}

						for (String s : ((JSONObject) o).keySet()) {

							if ("type".equals(s.intern())) {
							} else {

								if (!"".equals(objectKeyName)) {
									objectNameBilder = objectNameBilder.append(objectKeyName).append(".")
											.append(s.intern());
								} else {
									objectKeyName = objectNameBilder.append(".").append("default").toString();
									objectNameBilder = objectNameBilder.append(".").append(s.intern());
								}

								if (attributeValues.isEmpty()) {
									attributeValues.add(((JSONObject) o).get(s));
									multivaluedAttributeMap.put(objectNameBilder.toString(), attributeValues);
								} else {
									if (multivaluedAttributeMap.containsKey(objectNameBilder.toString())) {
										attributeValues = multivaluedAttributeMap.get(objectNameBilder.toString());
										attributeValues.add(((JSONObject) o).get(s));
									} else {
										Collection<Object> newAttributeValues = new ArrayList<Object>();
										newAttributeValues.add(((JSONObject) o).get(s));
										multivaluedAttributeMap.put(objectNameBilder.toString(), newAttributeValues);
									}

								}
								objectNameBilder.delete(0, objectNameBilder.length());

							}
						}

						//

					} else {
						objectKeyName = objectNameBilder.append(".").append(o.toString()).toString();
						cob.addAttribute(objectKeyName, o);
					}
				}

				if (!multivaluedAttributeMap.isEmpty()) {
					for (String attributeName : multivaluedAttributeMap.keySet()) {
						cob.addAttribute(attributeName, multivaluedAttributeMap.get(attributeName));
					}

				}

			} else if (attribute instanceof JSONObject) {
				for (String s : ((JSONObject) attribute).keySet()) {

					StringBuilder objectNameBilder = new StringBuilder(key.intern());
					cob.addAttribute(objectNameBilder.append(".").append(s).toString(),
							((JSONObject) attribute).get(s));

				}

			} else {

				if ("active".equals(key)) {
					cob.addAttribute("__ENABLE__", resourceJsonObject.get(key));
				} else {

					if (!resourceJsonObject.get(key).equals(null)) {

						cob.addAttribute(key.intern(), resourceJsonObject.get(key));
					} else {
						cob.addAttribute(key.intern(), "");

					}
				}
			}
		}
		ConnectorObject finalConnectorObject = cob.build();
		LOGGER.info("The connector object returned for the processed json: {0}", finalConnectorObject);
		return finalConnectorObject;

	}

	@Override
	public Uid createEntity(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			HashSet<Attribute> injectedAttributeSet) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitContainsAllValuesFilter(String p, ContainsAllValuesFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void parseAttribute(JSONObject attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public ObjectClassInfo buildSchema(Map<String, Map<String, Object>> attributeMap, String objectTypeName,
			String providerName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uid specialGroupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri,
			Header authHeader) {

		HttpClient httpClient = HttpClientBuilder.create().build();
		Uid id = null;
		Integer statusCode = response.getStatusLine().getStatusCode();

		LOGGER.warn(
				"Status code from first update query: {0}. Processing trought Salesforce \"group/member update\" workaround. ",
				statusCode);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(PRETTYPRINTHEADER);

		try {
			response = httpClient.execute(httpGet);

			statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("status code: {0}", statusCode);
			if (statusCode == 200) {

				String responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {

					JSONObject json = new JSONObject(responseString);
					LOGGER.info("Json object returned from service provider: {0}", json);
					for (String attributeName : jsonObject.keySet()) {

						json.put(attributeName, jsonObject.get(attributeName));
					}

					StringEntity bodyContent = new StringEntity(json.toString(1));
					bodyContent.setContentType("application/json");

					HttpPatch httpPatch = new HttpPatch(uri);

					httpPatch.setEntity(bodyContent);

					response = httpClient.execute(httpPatch);

					// TODO fix bug in group update
					statusCode = response.getStatusLine().getStatusCode();
					LOGGER.info("status code: {0}", statusCode);
					if (statusCode == 200 || statusCode == 201) {
						LOGGER.info("Update of resource was succesfull");
						responseString = EntityUtils.toString(response.getEntity());
						json = new JSONObject(responseString);
						id = new Uid(json.getString("id"));
						LOGGER.ok("Json response: {0}", json.toString(1));
						return id;
					} else {
						responseString = EntityUtils.toString(response.getEntity());

						LOGGER.error("An error has occured while updating the resource {0}", responseString);
					}

				}
			}

		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has occurred while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has occurred while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(
					"An protocol exception has occurred while in the process of updating a resource object, Possible mismatch in the interpretation of the HTTP specification.",
					e);
		} catch (IOException e) {

			LOGGER.error(
					"An error has occurred while processing the http response. Occurrence in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object",
					e);

		}

		return id;
	}

}
