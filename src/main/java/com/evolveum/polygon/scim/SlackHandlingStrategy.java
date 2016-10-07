package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Matus
 *
 *         The "HandlingStrategy" methods implementation for the "Slack"
 *         service.
 *
 */

public class SlackHandlingStrategy extends StandardScimHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SlackHandlingStrategy.class);
	private static final String SCHEMAVALUE = "urn:scim:schemas:core:1.0";
	private static final String NICKNAME = "nickName";
	private static final String USERNAME = "userName";
	private static final String TITLE = "title";
	private static final String SCHEMAS = "schemas";
	private static final String PROFILEURL = "profileUrl";
	private static final String TIMEZONE = "timezone";
	private static final String EXTERNALID = "externalId";
	private static final String PHOTOS = "photos";
	private static final String READONLY = "readOnly";
	private static final String SCHEMA = "schema";
	private static final String REQUIRED = "required";
	private static final String CASEEXSACT = "caseExact";
	private static final String STRING = "string";

	private static final String EMAILSDEFAULTVALUE = "emails.default.value";
	private static final String EMAILSDEFAULTPRIMARY = "emails.default.primary";
	private static final String ATTRIBUTES = "attributes";
	private static final String GROUPS = "groups";

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {

		LOGGER.warn("Processing trough slack missing schema attributes workaround for the resource: \"{0}\"",
				resourceName);
		if (USERS.equals(resourceName)) {

			Map<String, String> missingAttributes = new HashMap<String, String>();
			missingAttributes.put(USERNAME, USERNAME);
			missingAttributes.put(NICKNAME, NICKNAME);
			missingAttributes.put(TITLE, TITLE);
			missingAttributes.put(SCHEMAS, SCHEMAS);
			missingAttributes.put(PROFILEURL, PROFILEURL);
			missingAttributes.put(DISPLAYNAME, DISPLAYNAME);
			missingAttributes.put(TIMEZONE, TIMEZONE);
			missingAttributes.put(EXTERNALID, EXTERNALID);
			missingAttributes.put(ACTIVE, ACTIVE);
			missingAttributes.put(PHOTOS, PHOTOS);

			if (jsonObject.has(ATTRIBUTES)) {

				JSONArray attributesArray = new JSONArray();

				attributesArray = jsonObject.getJSONArray(ATTRIBUTES);
				for (int indexValue = 0; indexValue < attributesArray.length(); indexValue++) {
					JSONObject subAttributes = attributesArray.getJSONObject(indexValue);

					if (subAttributes.has(NAME)) {

						if (USERNAME.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(USERNAME);
						} else if (NICKNAME.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(NICKNAME);
						} else if (TITLE.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(TITLE);
						} else if (SCHEMAS.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(SCHEMAS);
						} else if (PROFILEURL.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(PROFILEURL);
						} else if (DISPLAYNAME.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(DISPLAYNAME);
						} else if (TIMEZONE.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(TIMEZONE);
						} else if (EXTERNALID.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(EXTERNALID);
						} else if (ACTIVE.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(ACTIVE);
						} else if (PHOTOS.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(PHOTOS);
						}
					}
				}

				for (String missingAttributeName : missingAttributes.keySet()) {

					LOGGER.warn("Building schema definition for the attribute: \"{0}\"", missingAttributeName);

					if (USERNAME.equals(missingAttributeName)) {
						JSONObject userName = new JSONObject();

						userName.put(SCHEMA, SCHEMAVALUE);
						userName.put(NAME, USERNAME);
						userName.put(READONLY, false);
						userName.put(TYPE, STRING);
						userName.put(REQUIRED, true);
						userName.put(CASEEXSACT, false);

						attributesArray.put(userName);

					} else if (ACTIVE.equals(missingAttributeName)) {
						JSONObject active = new JSONObject();

						active.put(SCHEMA, SCHEMAVALUE);
						active.put(NAME, ACTIVE);
						active.put(READONLY, false);
						active.put(TYPE, "boolean");
						active.put(REQUIRED, false);
						active.put(CASEEXSACT, false);

						attributesArray.put(active);

					} else if (EXTERNALID.equals(missingAttributeName)) {
						JSONObject externalId = new JSONObject();

						externalId.put(SCHEMA, SCHEMAVALUE);
						externalId.put(NAME, EXTERNALID);
						externalId.put(READONLY, false);
						externalId.put(TYPE, STRING);
						externalId.put(REQUIRED, false);
						externalId.put(CASEEXSACT, true);

						attributesArray.put(externalId);

					} else if (TIMEZONE.equals(missingAttributeName)) {
						JSONObject timezone = new JSONObject();

						timezone.put(SCHEMA, SCHEMAVALUE);
						timezone.put(NAME, TIMEZONE);
						timezone.put(READONLY, false);
						timezone.put(TYPE, STRING);
						timezone.put(REQUIRED, false);
						timezone.put(CASEEXSACT, false);

						attributesArray.put(timezone);

					} else if (DISPLAYNAME.equals(missingAttributeName)) {
						JSONObject displayName = new JSONObject();

						displayName.put(SCHEMA, SCHEMAVALUE);
						displayName.put(NAME, DISPLAYNAME);
						displayName.put(READONLY, false);
						displayName.put(TYPE, STRING);
						displayName.put(REQUIRED, false);
						displayName.put(CASEEXSACT, false);

						attributesArray.put(displayName);

					} else if (PROFILEURL.equals(missingAttributeName)) {
						JSONObject profileUrl = new JSONObject();

						profileUrl.put(SCHEMA, SCHEMAVALUE);
						profileUrl.put(NAME, PROFILEURL);
						profileUrl.put(READONLY, false);
						profileUrl.put(TYPE, STRING);
						profileUrl.put(REQUIRED, false);
						profileUrl.put(CASEEXSACT, false);

						attributesArray.put(profileUrl);

					} else if (NICKNAME.equals(missingAttributeName)) {
						JSONObject nickName = new JSONObject();

						nickName.put(SCHEMA, SCHEMAVALUE);
						nickName.put(NAME, NICKNAME);
						nickName.put(READONLY, false);
						nickName.put(TYPE, STRING);
						nickName.put(REQUIRED, true);
						nickName.put(CASEEXSACT, false);

						attributesArray.put(nickName);

					} else if (TITLE.equals(missingAttributeName)) {
						JSONObject title = new JSONObject();

						title.put(SCHEMA, SCHEMAVALUE);
						title.put(NAME, TITLE);
						title.put(READONLY, false);
						title.put(TYPE, STRING);
						title.put(REQUIRED, false);
						title.put(CASEEXSACT, false);

						attributesArray.put(title);
					} else if (SCHEMAS.equals(missingAttributeName)) {
						JSONObject schemas = new JSONObject();
						JSONArray subAttributeArray = new JSONArray();

						JSONObject valueBlank = new JSONObject();

						schemas.put(SCHEMA, SCHEMAVALUE);
						schemas.put(NAME, SCHEMAS);
						schemas.put(READONLY, false);
						schemas.put(TYPE, "complex");
						schemas.put(MULTIVALUED, true);
						schemas.put(REQUIRED, false);
						schemas.put(CASEEXSACT, false);

						valueBlank.put(NAME, "blank");
						valueBlank.put(READONLY, true);
						valueBlank.put(REQUIRED, false);
						valueBlank.put(MULTIVALUED, false);

						subAttributeArray.put(valueBlank);

						schemas.put(SUBATTRIBUTES, subAttributeArray);

						attributesArray.put(schemas);
					}

				}

				jsonObject.put(ATTRIBUTES, attributesArray);
			}

		}
		return jsonObject;

	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {

		if (!attributeMapList.isEmpty()) {

			for (int position = 0; position < attributeMapList.size(); position++) {
				Map<String, Map<String, Object>> resources = attributeMapList.get(position);

				if (resources.containsKey(USERNAME) && !resources.containsKey(EMAILSDEFAULTPRIMARY)
						&& !resources.containsKey(EMAILSDEFAULTVALUE)) {

					resources.put(EMAILSDEFAULTPRIMARY, null);
					resources.put(EMAILSDEFAULTVALUE, null);

					attributeMapList.remove(position);
					attributeMapList.add(position, resources);
				}

			}
		}
		return attributeMapList;

	}

	@Override
	public Set<Attribute> addAttributesToInject(Set<Attribute> injectedAttributeSet) {
		Attribute schemaAttribute = AttributeBuilder.build("schemas.default.blank", SCHEMAVALUE);
		injectedAttributeSet.add(schemaAttribute);
		return injectedAttributeSet;
	}

	@Override
	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoint, ScimConnectorConfiguration conf) {

		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> authorizationData = ServiceAccessManager.logIntoService(conf);

		HttpPost loginInstance = null;

		for (String data : authorizationData.keySet()) {
			if (AUTHHEADER.equals(data)) {

				authHeader = (Header) authorizationData.get(data);

			} else if (URI.equals(data)) {

				scimBaseUri = (String) authorizationData.get(data);
			} else if (LOGININSTANCE.equals(data)) {

				loginInstance = (HttpPost) authorizationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		ServiceAccessManager.logIntoService(conf);
		HttpClient httpClient = HttpClientBuilder.create().build();
		String queuedUid;
		queuedUid = ((Uid) uid).getUidValue();

		String uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).append(SLASH)
				.append(queuedUid).toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(PRETTYPRINTHEADER);

		String responseString = null;
		HttpResponse response;
		try {
			response = httpClient.execute(httpGet);

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Status code: {0}", statusCode);
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(responseString);

						LOGGER.info("Json object returned from service provider: {0}", jsonObject.toString(1));

						if (jsonObject.has(GROUPS)) {
							int amountOfResources = jsonObject.getJSONArray(GROUPS).length();

							for (int position = 0; position < amountOfResources; position++) {
								JSONObject minResourceJson = new JSONObject();
								minResourceJson = jsonObject.getJSONArray(GROUPS).getJSONObject(position);
								if (minResourceJson.has(VALUE)) {

									String groupUid = minResourceJson.getString(VALUE);
									if (groupUid != null && !groupUid.isEmpty()) {

										StringBuilder groupUri = new StringBuilder(scimBaseUri).append(SLASH)
												.append(membershipResourceEndpoint).append(SLASH).append(groupUid);

										LOGGER.info("The uri to which we are sending the queri {0}", groupUri);

										HttpGet httpGetR = new HttpGet(groupUri.toString());
										httpGetR.addHeader(authHeader);
										httpGetR.addHeader(PRETTYPRINTHEADER);

										HttpResponse resourceResponse = httpClient.execute(httpGetR);

										if (statusCode == 200) {
											responseString = EntityUtils.toString(resourceResponse.getEntity());
											JSONObject fullResourceJson = new JSONObject(responseString);

											LOGGER.info(
													"The {0}. resource json object which was returned by the service provider: {1}",
													position + 1, fullResourceJson.toString(1));

											ConnectorObject connectorObject = buildConnectorObject(fullResourceJson,
													membershipResourceEndpoint);
											resultHandler.handle(connectorObject);

										} else {

											ErrorHandler.onNoSuccess(resourceResponse, groupUri.toString());
										}

									}
								} else {
									LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

									throw new ConnectorException(
											"No uid present in fetched object while processing query result");

								}
							}
						} else {

							LOGGER.error("Resource object not present in provider response to the query");

							throw new ConnectorException(
									"No uid present in fetched object while processing query result");
						}

					} catch (JSONException jsonException) {
						if (queuedUid == null) {
							queuedUid = "the full resource representation";
						}
						LOGGER.error(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException.getLocalizedMessage(), queuedUid);
						LOGGER.info(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException, queuedUid);
						throw new ConnectorException(
								"An exception has occurred while setting the variable \"jsonObject\".", jsonException);
					}

				} else {

					LOGGER.warn("Service provider response is empty, responce returned on query: {0}", uri);
				}
			} else {
				ErrorHandler.onNoSuccess(response, uri);
			}

		} catch (IOException e) {

			if (queuedUid == null) {
				queuedUid = "the full resource representation";
			}

			LOGGER.error(
					"An error occurred while processing the query http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e.getLocalizedMessage(), queuedUid);
			LOGGER.info(
					"An error occurred while processing the query http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e, queuedUid);
			throw new ConnectorIOException("An error occurred while processing the query http response.", e);
		} finally {
			ServiceAccessManager.logOut(loginInstance);
		}

	}

	@Override
	public List<String> excludeFromAssembly(List<String> excludedAttributes) {
		excludedAttributes.add(META);
		excludedAttributes.add("schemas");
		excludedAttributes.add(PHOTOS);

		return excludedAttributes;
	}

	@Override
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		return null;
	}

	// TODO check if method not obsolete.
	/**
	 * 
	 * Extends the "AttributeInfoBuilder" parameter which was provided with an
	 * attribute which this method builds.
	 * 
	 * @param builder
	 * @param attributeName
	 * @param infoBuilder
	 * @return
	 */

	private ObjectClassInfoBuilder buildMissingAttributes(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {

		if (EMAILSDEFAULTVALUE.equals(attributeName)) {
			infoBuilder.setMultiValued(true);
			infoBuilder.setRequired(true);
			infoBuilder.setType(String.class);
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			infoBuilder.setMultiValued(false);
			infoBuilder.setRequired(true);
			infoBuilder.setType(Boolean.class);
			builder.addAttributeInfo(infoBuilder.build());
		}

		return builder;

	}

	@Override
	public ObjectClassInfoBuilder schemaObjectInjection(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {

		if (ACTIVE.equals(attributeName)) {
			builder = builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
		} else {
			builder = buildMissingAttributes(builder, attributeName, infoBuilder);
		}

		return builder;
	}

	@Override
	public List<String> populateDictionary(WorkaroundFlags flag) {
		List<String> dictionary = new ArrayList<String>();

		if (WorkaroundFlags.PARSERFLAG.getValue().equals(flag.getValue())) {
			dictionary.add(SUBATTRIBUTES);
			dictionary.add("subattributes");
		} else if (WorkaroundFlags.BUILDERFLAG.getValue().equals(flag.getValue())) {

			dictionary.add(ACTIVE);
			dictionary.add(EMAILSDEFAULTVALUE);
			dictionary.add(EMAILSDEFAULTPRIMARY);
		} else {

			LOGGER.warn("No such flag defined: {0}", flag);
		}

		return dictionary;
	}

	@Override
	public Boolean checkFilter(Filter filter, String endpointName) {

		LOGGER.info("Checking filter if contains all values handling is needed ");

		if (endpointName.equals("Groups")) {
			if (filter instanceof EqualsFilter || filter instanceof ContainsAllValuesFilter) {

				Attribute filterAttr = ((AttributeFilter) filter).getAttribute();

				String attributeName = filterAttr.getName();

				LOGGER.info("The attribute: {0}", attributeName);
				LOGGER.info("The attribute value: {0}", filterAttr);

				if ("members.default.value".equals(attributeName)) {
					LOGGER.warn("Processing trough group object class \"equals\" filter workaround.");
					return true;
				} else {

					return false;
				}
			}

		}
		return false;
	}

	@Override
	public void handleCAVGroupQuery(JSONObject jsonObject, String resourceEndPoint, ResultsHandler handler,
			String scimBaseUri, Header authHeader) throws ClientProtocolException, IOException {
		String responseString = null;
		HttpClient httpClient = HttpClientBuilder.create().build();

		if (jsonObject.has(GROUPS)) {
			int amountOfResources = jsonObject.getJSONArray(GROUPS).length();

			for (int position = 0; position < amountOfResources; position++) {
				JSONObject minResourceJson = new JSONObject();
				minResourceJson = jsonObject.getJSONArray(GROUPS).getJSONObject(position);
				if (minResourceJson.has(VALUE)) {

					String groupUid = minResourceJson.getString(VALUE);
					if (groupUid != null && !groupUid.isEmpty()) {

						StringBuilder groupUri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint)
								.append(SLASH).append(groupUid);

						LOGGER.info("The uri to which we are sending the queri {0}", groupUri);

						HttpGet httpGetR = new HttpGet(groupUri.toString());
						httpGetR.addHeader(authHeader);
						httpGetR.addHeader(PRETTYPRINTHEADER);

						HttpResponse resourceResponse = httpClient.execute(httpGetR);
						int statusCode = resourceResponse.getStatusLine().getStatusCode();

						if (statusCode == 200) {
							responseString = EntityUtils.toString(resourceResponse.getEntity());
							JSONObject fullResourceJson = new JSONObject(responseString);

							LOGGER.info("The {0}. resource json object which was returned by the service provider: {1}",
									position + 1, fullResourceJson.toString(1));

							ConnectorObject connectorObject = buildConnectorObject(fullResourceJson, resourceEndPoint);
							handler.handle(connectorObject);

						} else {

							ErrorHandler.onNoSuccess(resourceResponse, groupUri.toString());
						}

					}
				} else {
					LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

					throw new ConnectorException("No uid present in fetched object while processing query result");
				}
			}
		} else {

			LOGGER.error("Resource object not present in provider response to the query");

			throw new ConnectorException("No uid present in fetched object while processing query result");
		}

	}

}
