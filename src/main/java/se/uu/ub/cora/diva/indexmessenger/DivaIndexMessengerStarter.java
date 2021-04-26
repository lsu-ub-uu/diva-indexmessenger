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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import se.uu.ub.cora.indexmessenger.CoraCredentials;
import se.uu.ub.cora.indexmessenger.IndexMessengerListener;
import se.uu.ub.cora.indexmessenger.parser.MessageParserFactory;
import se.uu.ub.cora.javaclient.cora.CoraClientFactory;
import se.uu.ub.cora.javaclient.cora.CoraClientFactoryImp;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.JmsMessageRoutingInfo;

public class DivaIndexMessengerStarter {

	protected static IndexMessengerListener indexMessengerListener;

	private static Logger logger = LoggerProvider
			.getLoggerForClass(DivaIndexMessengerStarter.class);

	private DivaIndexMessengerStarter() {
	}

	public static void main(String[] args) {
		logger.logInfoUsingMessage("DivaIndexMessengerStarter starting...");
		tryToCreateIndexMessengerListener(args);
	}

	private static void tryToCreateIndexMessengerListener(String[] args) {
		try {
			Properties properties = loadProperties(args);
			createIndexMessengerListener(properties);
			logger.logInfoUsingMessage("DivaIndexMessengerStarter started");
		} catch (Exception ex) {
			logger.logFatalUsingMessageAndException("Unable to start DivaIndexMessengerStarter ",
					ex);
		}
	}

	private static Properties loadProperties(String[] args) throws IOException {
		if (propertiesShouldBeReadFromFile(args)) {
			String propertiesFileName = getFilenameFromArgsOrDefault(args);
			return readPropertiesFromFile(propertiesFileName);
		} else if (propertiesProvidedAsArguments(args)) {
			return loadProperitesFromArgs(args);
		}
		throw new RuntimeException("Number of arguments should be 9.");

	}

	private static boolean propertiesShouldBeReadFromFile(String[] args) {
		return args.length == 0 || fileNameProvidedAsArgument(args);
	}

	private static boolean fileNameProvidedAsArgument(String[] args) {
		return args.length == 1;
	}

	private static boolean propertiesProvidedAsArguments(String[] args) {
		return args.length == 9;
	}

	private static Properties readPropertiesFromFile(String propertiesFileName) throws IOException {
		try (InputStream input = DivaIndexMessengerStarter.class.getClassLoader()
				.getResourceAsStream(propertiesFileName)) {
			return loadProperitesFromFile(input);
		}
	}

	private static String getFilenameFromArgsOrDefault(String[] args) {
		if (args.length > 0) {
			return args[0];
		}
		return "divaIndexer.properties";
	}

	private static Properties loadProperitesFromArgs(String[] args) {
		Properties properties = new Properties();
		properties.put("messaging.hostname", args[0]);
		properties.put("messaging.port", args[1]);
		properties.put("messaging.routingKey", args[2]);
		properties.put("messaging.username", args[3]);
		properties.put("messaging.password", args[4]);
		properties.put("appTokenVerifierUrl", args[5]);
		properties.put("baseUrl", args[6]);
		properties.put("cora.userId", args[7]);
		properties.put("cora.appToken", args[8]);

		return properties;
	}

	private static Properties loadProperitesFromFile(InputStream input) throws IOException {
		Properties properties = new Properties();

		properties.load(input);
		return properties;
	}

	private static void createIndexMessengerListener(Properties properties) {
		CoraClientFactory coraClientFactory = createCoraClientFactoryFromProperties(properties);
		MessageParserFactory messageParserFactory = new DivaMessageParserFactory();
		JmsMessageRoutingInfo routingInfo = createMessageRoutingInfoFromProperties(properties);
		CoraCredentials credentials = createCoraCredentialsFromProperties(properties);

		String logM = "Will listen for index messages from: {0} using port: {1}";
		String formattedLogMessage = MessageFormat.format(logM, routingInfo.hostname,
				routingInfo.port);
		logger.logInfoUsingMessage(formattedLogMessage);

		indexMessengerListener = new IndexMessengerListener(coraClientFactory, messageParserFactory,
				routingInfo, credentials);

	}

	private static CoraClientFactory createCoraClientFactoryFromProperties(Properties properties) {
		String baseUrl = extractPropertyThrowErrorIfNotFound(properties, "baseUrl");
		String appTokenVerifierUrl = extractPropertyThrowErrorIfNotFound(properties,
				"appTokenVerifierUrl");
		String logM2 = "Sending indexOrders to: {0} using appToken from: {1}";
		String formattedLogMessage2 = MessageFormat.format(logM2, baseUrl, appTokenVerifierUrl);
		logger.logInfoUsingMessage(formattedLogMessage2);
		return CoraClientFactoryImp.usingAppTokenVerifierUrlAndBaseUrl(appTokenVerifierUrl,
				baseUrl);
	}

	private static String extractPropertyThrowErrorIfNotFound(Properties properties,
			String propertyName) {
		throwErrorIfPropertyNameIsMissing(properties, propertyName);
		return properties.getProperty(propertyName);
	}

	private static void throwErrorIfPropertyNameIsMissing(Properties properties,
			String propertyName) {
		if (!properties.containsKey(propertyName)) {
			throw new RuntimeException(
					"Property with name " + propertyName + " not found in properties");
		}
	}

	private static JmsMessageRoutingInfo createMessageRoutingInfoFromProperties(
			Properties properties) {
		String hostname = extractPropertyThrowErrorIfNotFound(properties, "messaging.hostname");
		String port = extractPropertyThrowErrorIfNotFound(properties, "messaging.port");
		String routingKey = extractPropertyThrowErrorIfNotFound(properties, "messaging.routingKey");
		String username = extractPropertyThrowErrorIfNotFound(properties, "messaging.username");
		String password = extractPropertyThrowErrorIfNotFound(properties, "messaging.password");
		return new JmsMessageRoutingInfo(hostname, port, routingKey, username, password);
	}

	private static CoraCredentials createCoraCredentialsFromProperties(Properties properties) {
		String userId = properties.getProperty("cora.userId");
		String apptoken = properties.getProperty("cora.appToken");
		return new CoraCredentials(userId, apptoken);
	}
}
