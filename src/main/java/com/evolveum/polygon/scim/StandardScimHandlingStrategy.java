package com.evolveum.polygon.scim;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.json.JSONObject;

public class StandardScimHandlingStrategy implements HandlingStrategy {

	@Override
	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uid specialGroupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri,
			Header authHeader) {
		// TODO Auto-generated method stub
		return null;
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
	public ObjectClassInfoBuilder schemaBuilderProcedure(String attributeName,
			Map<String, Map<String, Object>> attributeMap, ObjectClassInfoBuilder builder,
			SchemaObjectBuilderGeneric schemaBuilder) {
		// TODO Auto-generated method stub
		return null;
	}

}
