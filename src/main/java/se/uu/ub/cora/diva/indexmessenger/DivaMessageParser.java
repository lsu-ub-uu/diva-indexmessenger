/*
 * Copyright 2019 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.diva.indexmessenger;

import java.util.Map;

import se.uu.ub.cora.indexmessenger.IndexMessageException;
import se.uu.ub.cora.indexmessenger.parser.MessageParser;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;

public class DivaMessageParser implements MessageParser {
	private static final String TEXT_TO_IDENTIFY_MESSAGES_WHICH_DOES_TRIGGER_INDEXING = ""
			+ "<category term=\"MODEL_NOREF\" scheme=\"fedora-types:dsID\" label=\"xsd:string\"></category>";
	private Logger logger = LoggerProvider.getLoggerForClass(DivaMessageParser.class);
	private String parsedRecordId;
	private boolean workOrderShouldBeCreated = false;
	private String parsedType;

	@Override
	public void parseHeadersAndMessage(Map<String, String> headers, String message) {
		try {
			tryToParseMessage(headers, message);
		} catch (IndexMessageException exception) {
			handleError(exception);
		}
	}

	private void tryToParseMessage(Map<String, String> headers, String message) {
		logger.logInfoUsingMessage("------------------------------------------------------------");
		logger.logInfoUsingMessage("HEADERS: " + headers);
		logger.logInfoUsingMessage("");
		logger.logInfoUsingMessage("MESSAGE: " + message);
		logger.logInfoUsingMessage("------------------------------------------------------------");
		extractRecordIdFromHeaders(headers);
		if (shouldWorkOrderBeCreatedForMessage(headers)) {
			parsedType = "person";
			workOrderShouldBeCreated = true;
		}
	}

	private void extractRecordIdFromHeaders(Map<String, String> headers) {
		parsedRecordId = headers.get("pid");
		if (parsedRecordId == null)
			throw IndexMessageException.withMessage("No pid found in header");
	}

	private boolean shouldWorkOrderBeCreatedForMessage(Map<String, String> headers) {
		String methodName = headers.get("methodName");
		String typePartOfId = extractTypePartOfId();
		return "modifyDatastreamByReference".equals(methodName)
				&& "authority-person".equals(typePartOfId);
		// return message.contains(TEXT_TO_IDENTIFY_MESSAGES_WHICH_DOES_TRIGGER_INDEXING);
	}

	private String extractTypePartOfId() {
		return parsedRecordId.substring(0, parsedRecordId.indexOf(":"));
	}

	private void handleError(IndexMessageException e) {
		logger.logErrorUsingMessage(e.getMessage());
	}

	@Override
	public String getParsedId() {
		return parsedRecordId;
	}

	@Override
	public String getParsedType() {
		return parsedType;
	}

	@Override
	public boolean shouldWorkOrderBeCreatedForMessage() {
		return workOrderShouldBeCreated;
	}

}
