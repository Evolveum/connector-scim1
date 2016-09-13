package com.evolveum.polygon.test.scim;

import java.util.ArrayList;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.spi.SearchResultsHandler;

final class TestSearchResultsHandler implements SearchResultsHandler {

	private static final ArrayList<ConnectorObject> result = new ArrayList<>();

	private static final Log LOGGER = Log.getLog(TestSearchResultsHandler.class);

	public TestSearchResultsHandler() {

		result.clear();
	}

	@Override
	public boolean handle(ConnectorObject connectorObject) {
		result.add(connectorObject);
		return true;
	}

	@Override
	public void handleResult(SearchResult result) {
		LOGGER.info("Im handling {0}", result.getRemainingPagedResults());

	}

	public ArrayList<ConnectorObject> getResult() {

		return TestSearchResultsHandler.result;
	}
}