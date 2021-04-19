/*
 * Copyright 2019, 2021 Uppsala University Library
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
	private static final String TEXT_TO_IDENTIFY_MESSAGES_FOR_DELETE = ""
			+ "<category term=\"D\" scheme=\"fedora-types:state\" label=\"xsd:string\"></category>";
	private Logger logger = LoggerProvider.getLoggerForClass(DivaMessageParser.class);
	private String parsedRecordId;
	private boolean workOrderShouldBeCreated = false;
	private String parsedType;
	private String modificationType;

	@Override
	public void parseHeadersAndMessage(Map<String, String> headers, String message) {
		try {
			tryToParseMessage(headers, message);
		} catch (IndexMessageException exception) {
			handleError(exception);
		}
	}

	// shouldbecreated = true
	// modifyDatastreamByReference
	// modifyObject (delete) - i kombination med TEXT_TO_IDENTIFY
	// purgeObject - inte hanterat
	// addDatastream inte hanterat

	private void tryToParseMessage(Map<String, String> headers, String message) {
		logForTestingPurposes(headers, message);

		throwErrorIfNoPid(headers);

		// check if workorder should be created at all
		// OR set values first, check to be created after??

		setRecordIdFromHeaders(headers);
		setModificationTypeFromMessageAndHeaders(message, headers);
		if (workOrderShouldBeCreatedForMessage(headers, message)) {
			parsedType = "person";
			workOrderShouldBeCreated = true;
		}
	}

	private void logForTestingPurposes(Map<String, String> headers, String message) {
		logger.logInfoUsingMessage("------------------------------------------------------------");
		logger.logInfoUsingMessage("HEADERS: " + headers);
		logger.logInfoUsingMessage("");
		logger.logInfoUsingMessage("MESSAGE: " + message);
		logger.logInfoUsingMessage("------------------------------------------------------------");
	}

	private void setModificationTypeFromMessageAndHeaders(String message,
			Map<String, String> headers) {
		String methodName = headers.get("methodName");
		modificationType = "update";
		if (messageIsFromDelete(message, methodName)) {
			modificationType = "delete";
		}
	}

	private boolean messageIsFromDelete(String message, String methodName) {
		return "modifyObject".equals(methodName)
				&& message.contains(TEXT_TO_IDENTIFY_MESSAGES_FOR_DELETE);
	}

	private void setRecordIdFromHeaders(Map<String, String> headers) {
		parsedRecordId = headers.get("pid");
	}

	private void throwErrorIfNoPid(Map<String, String> headers) {
		if (headers.get("pid") == null) {
			throw IndexMessageException.withMessage("No pid found in header");
		}
	}

	private boolean workOrderShouldBeCreatedForMessage(Map<String, String> headers,
			String message) {
		String methodName = headers.get("methodName");
		String typePartOfId = extractTypePartOfId();

		return methodNameIsCorrect(methodName) && typeIsAuthorityPerson(typePartOfId);
	}

	private String extractTypePartOfId() {
		return parsedRecordId.substring(0, parsedRecordId.indexOf(":"));
	}

	private boolean methodNameIsCorrect(String methodName) {
		return "modifyDatastreamByReference".equals(methodName)
				|| "modifyObject".equals(methodName);
	}

	private boolean typeIsAuthorityPerson(String typePartOfId) {
		return "authority-person".equals(typePartOfId);
	}

	private void handleError(IndexMessageException e) {
		logger.logErrorUsingMessage(e.getMessage());
	}

	@Override
	public String getRecordId() {
		return parsedRecordId;
	}

	@Override
	public String getRecordType() {
		return parsedType;
	}

	@Override
	public boolean shouldWorkOrderBeCreatedForMessage() {
		return workOrderShouldBeCreated;
	}

	@Override
	public String getModificationType() {
		return modificationType;
	}

}
