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

	@Override
	public void parseHeadersAndMessage(Map<String, String> headers, String message) {
		try {
			tryToParseMessage(headers, message);
		} catch (IndexMessageException exception) {
			handleError(exception);
		}
	}

	private void tryToParseMessage(Map<String, String> headers, String message) {
		if (shouldWorkOrderBeCreatedForMessage(message)) {
			extractRecordIdFromHeaders(headers);
			workOrderShouldBeCreated = true;
		}
	}

	private boolean shouldWorkOrderBeCreatedForMessage(String message) {
		return message.contains(TEXT_TO_IDENTIFY_MESSAGES_WHICH_DOES_TRIGGER_INDEXING);
	}

	private void extractRecordIdFromHeaders(Map<String, String> headers) {
		parsedRecordId = headers.get("pid");
		if (parsedRecordId == null)
			throw IndexMessageException.withMessage("No pid found in header");
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
		// return top level diva type for all publication types when it is decided what it should be
		// called

		return "currentlyUnknownParent";
	}

	@Override
	public boolean shouldWorkOrderBeCreatedForMessage() {
		return workOrderShouldBeCreated;
	}
}
