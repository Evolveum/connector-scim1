package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

import com.evolveum.polygon.scim.common.HttpPatch;

public class SalesforceHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SalesforceHandlingStrategy.class);
	private static final String ID = "id";
	private static final String TYPE = "type";
	private static final String DEFAULT = "default";
	private static final String SCHEMATYPE = "urn:scim:schemas:extension:enterprise:1.0";
	private static final String SUBATTRIBUTES = "subAttributes";
	private static final String MULTIVALUED = "multiValued";
	private static final String CANONICALVALUES = "canonicalValues";
	private static final String REFERENCETYPES = "referenceTypes";
	private static final String CONTENTTYPE = "application/json";

	@Override
	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException {

		LOGGER.info("Building the connector object from provided json");

		if (resourceJsonObject == null) {
			LOGGER.error(
					"Empty json object was passed from data provider. Error occurrence while building connector object");
			throw new ConnectorException(
					"Empty json object was passed from data provider. Error occurrence while building connector object");
		}

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid(resourceJsonObject.getString(ID));

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
						"Processing trought \"schema inconsistencies\" workaround. Because of the \"{0}\" resoure attribute.",
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
							if (TYPE.equals(s.intern())) {
								objectKeyName = objectNameBilder.append(".").append(((JSONObject) o).get(s)).toString();
								objectNameBilder.delete(0, objectNameBilder.length());
								break;
							}
						}

						for (String s : ((JSONObject) o).keySet()) {

							if (TYPE.equals(s.intern())) {
							} else {

								if (!"".equals(objectKeyName)) {
									objectNameBilder = objectNameBilder.append(objectKeyName).append(".")
											.append(s.intern());
								} else {
									objectKeyName = objectNameBilder.append(".").append(DEFAULT).toString();
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
	public HashSet<Attribute> attributeInjection(HashSet<Attribute> injectedAttributeSet,
			HashMap<String, Object> autoriazationData) {

		JSONObject loginObject = null;
		String orgID = null;

		if (autoriazationData.containsKey("json")) {

			loginObject = (JSONObject) autoriazationData.get("json");
		}

		if (loginObject != null) {
			if (loginObject.has(ID)) {
				orgID = loginObject.getString(ID);
				String idParts[] = orgID.split("\\/");
				orgID = idParts[4];
			}
		} else {

			LOGGER.info("No json object returned after login");
		}
		// injection of organization ID into the set of attributes
		if (orgID != null) {
			LOGGER.info("The organization ID is: {0}", orgID);

			// TODO schema version might change
			injectedAttributeSet.add(AttributeBuilder.build("schema.type", SCHEMATYPE));

			injectedAttributeSet.add(AttributeBuilder.build("schema.organization", orgID));
		} else {
			LOGGER.warn("No organization ID specified in instance URL");
		}
		return injectedAttributeSet;

	}

	@Override
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> parseAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser) {

		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();

		if (attribute.has(SUBATTRIBUTES)) {
			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();
			subAttributes = (JSONArray) attribute.get(SUBATTRIBUTES);
			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if ("name".equals(subAttributeNameKeys.intern())) {
						attributeName = attribute.get(subAttributeNameKeys).toString();
						break;
					}
				}
			}
			for (String nameKey : attribute.keySet()) {
				if (MULTIVALUED.equals(nameKey.intern())) {
					isMultiValued = (Boolean) attribute.get(nameKey);
					break;
				}

			}

			for (int i = 0; i < subAttributes.length(); i++) {
				JSONObject subAttribute = new JSONObject();
				subAttribute = subAttributes.getJSONObject(i);
				subAttributeMap = parser.parseSubAttribute(subAttribute, subAttributeMap);
			}
			for (String typeKey : subAttributeMap.keySet()) {
				if (TYPE.equals(typeKey.intern())) {
					hasTypeValues = true;
					break;
				}
			}

			if (hasTypeValues) {
				Map<String, Object> typeObject = new HashMap<String, Object>();
				typeObject = (Map<String, Object>) subAttributeMap.get(TYPE);
				if (typeObject.containsKey(CANONICALVALUES) || typeObject.containsKey(REFERENCETYPES)) {
					JSONArray referenceValues = new JSONArray();
					if (typeObject.containsKey(CANONICALVALUES)) {
						referenceValues = (JSONArray) typeObject.get(CANONICALVALUES);
					} else {
						referenceValues = (JSONArray) typeObject.get(REFERENCETYPES);
					}

					for (int j = 0; j < referenceValues.length(); j++) {
						JSONObject referenceValue = new JSONObject();

						/*
						 * Salesforce scim schema inconsistencies workaround
						 * (canonicalValues,referenceTypes) defined as array of
						 * json objects -> should be defined as array of string
						 * values
						 */
						LOGGER.warn(
								"Processing trought Salesforce scim schema inconsistencies workaround (canonicalValues,referenceTypes) ");
						referenceValue = ((JSONArray) referenceValues).getJSONObject(j);
						for (String subAttributeKeyNames : subAttributeMap.keySet()) {
							if (!TYPE.equals(subAttributeKeyNames.intern())) { // TODO
								// some
								// other
								// complex
								// attribute
								// names
								// may
								// be
								// used
								StringBuilder complexAttrName = new StringBuilder(attributeName);
								attributeMap.put(
										complexAttrName.append(".").append(referenceValue.get("value")).append(".")
												.append(subAttributeKeyNames).toString(),
										(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
								isComplex = true;

							}
						}
					}
				} else {
					ArrayList<String> defaultReferenceTypeValues = new ArrayList<String>();
					defaultReferenceTypeValues.add("User");
					defaultReferenceTypeValues.add("Group");

					defaultReferenceTypeValues.add("external");
					defaultReferenceTypeValues.add("uri");

					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						if (!TYPE.equals(subAttributeKeyNames.intern())) {
							for (String defaultTypeReferenceValues : defaultReferenceTypeValues) {
								StringBuilder complexAttrName = new StringBuilder(attributeName);
								complexAttrName.append(".").append(defaultTypeReferenceValues);
								attributeMap.put(complexAttrName.append(".").append(subAttributeKeyNames).toString(),
										(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
								isComplex = true;
							}
						}

					}

				}

			} else {
				if ("roles".equals(attributeName)) {

					LOGGER.warn(
							"Processing trought salesforce \"schema inconsistencies\" workaround. Because of the \"{0}\" resoure attribute.",
							attributeName);

					isMultiValued = true;
				}

				if (!isMultiValued) {
					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);
						attributeMap.put(complexAttrName.append(".").append(subAttributeKeyNames).toString(),
								(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
						isComplex = true;
					}
				} else {
					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);

						HashMap<String, Object> subattributeKeyMap = (HashMap<String, Object>) subAttributeMap
								.get(subAttributeKeyNames);

						for (String attributePropertie : subattributeKeyMap.keySet()) {

							if (MULTIVALUED.equals(attributePropertie)) {
								subattributeKeyMap.put(MULTIVALUED, true);
							}
						}

						attributeMap.put(complexAttrName.append(".").append(DEFAULT).append(".")
								.append(subAttributeKeyNames).toString(), subattributeKeyMap);
						isComplex = true;
					}

				}
			}

		} else {

			for (String attributeNameKeys : attribute.keySet()) {

				if ("name".equals(attributeNameKeys.intern())) {
					attributeName = attribute.get(attributeNameKeys).toString();

				} else {
					attributeObjects.put(attributeNameKeys, attribute.get(attributeNameKeys));
				}

			}
		}
		if (!isComplex) {
			attributeMap.put(attributeName, attributeObjects);
		}
		return attributeMap;

	}

	@Override
	public Uid groupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader,
			CrudManagerScim manager) {

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
					bodyContent.setContentType(CONTENTTYPE);

					HttpPatch httpPatch = new HttpPatch(uri);
					httpPatch.addHeader(authHeader);
					httpPatch.addHeader(PRETTYPRINTHEADER);
					httpPatch.setEntity(bodyContent);

					response = httpClient.execute(httpPatch);
					statusCode = response.getStatusLine().getStatusCode();
					LOGGER.info("status code: {0}", statusCode);
					if (statusCode == 200 || statusCode == 201) {
						LOGGER.info("Update of resource was succesfull");
						responseString = EntityUtils.toString(response.getEntity());
						json = new JSONObject(responseString);
						id = new Uid(json.getString(ID));
						LOGGER.ok("Json response: {0}", json.toString(1));
						return id;
					} else {
						responseString = EntityUtils.toString(response.getEntity());

						manager.onNoSuccess(response, "updating object");
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

	@Override
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder) {
		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName.intern());

		if (!"active".equals(attributeName)) {
			Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
			schemaSubPropertiesMap = attributeMap.get(attributeName);
			for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
				if (SUBATTRIBUTES.equals(subPropertieName.intern())) {
					// TODO check positive cases
					infoBuilder = new AttributeInfoBuilder(attributeName.intern());
					JSONArray jsonArray = new JSONArray();

					jsonArray = ((JSONArray) schemaSubPropertiesMap.get(subPropertieName));
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject attribute = new JSONObject();
						attribute = jsonArray.getJSONObject(i);
					}
					break;
				} else {
					schemaBuilder.subPropertiesChecker(infoBuilder, schemaSubPropertiesMap, subPropertieName);
					if ("members.User.value".equals(attributeName) || "members.Group.value".equals(attributeName)
							|| "members.default.value".equals(attributeName)
							|| "members.default.display".equals(attributeName)) {
						infoBuilder.setMultiValued(true);
					}
				}
			}
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
		}
		return builder;
	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {
		return attributeMapList;
	}

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {
		return jsonObject;
	}

	@Override
	public String checkFilter(Filter filter) {
		return "";
	}

	@Override
	public StringBuilder retrieveFilterQuery(StringBuilder queryUriSnippet, char prefixChar, Filter query) {
		LOGGER.info("Processing trought the \" filter\" workaround for the provider: {0}", "salesforce");

		StringBuilder filterSnippet = new StringBuilder();
		filterSnippet = query.accept(new FilterHandler(), "salesforce");

		return queryUriSnippet.append(prefixChar).append("filter=").append(filterSnippet.toString());
	}

	@Override
	public HashSet<Attribute> addAttributeToInject(HashSet<Attribute> injectetAttributeSet) {
		return injectetAttributeSet;
	}
}
