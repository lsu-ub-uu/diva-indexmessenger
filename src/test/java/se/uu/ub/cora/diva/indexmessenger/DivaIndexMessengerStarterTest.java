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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.indexmessenger.CoraCredentials;
import se.uu.ub.cora.indexmessenger.IndexMessengerListener;
import se.uu.ub.cora.indexmessenger.log.LoggerFactorySpy;
import se.uu.ub.cora.indexmessenger.log.LoggerSpy;
import se.uu.ub.cora.javaclient.cora.CoraClientFactoryImp;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.JmsMessageRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;

public class DivaIndexMessengerStarterTest {

	private LoggerFactorySpy loggerFactorySpy = new LoggerFactorySpy();
	private MessagingFactorySpy messagingFactorySpy;
	private String testedClassName = "DivaIndexMessengerStarter";
	private String args[];

	@BeforeMethod
	public void setUp() {
		loggerFactorySpy.resetLogs(testedClassName);
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		messagingFactorySpy = new MessagingFactorySpy();
		MessagingProvider.setMessagingFactory(messagingFactorySpy);

		args = new String[] { "args-dev-diva-drafts", "args-61617", "args-fedora.apim.*",
				"args-admin", "args-admin", "args-someAppTokenVerifierUrl", "args-someBaseUrl",
				"args-userIdForCora", "args-appTokenForCora" };
	}

	@Test
	public void testConstructorIsPrivate() throws Exception {
		Constructor<DivaIndexMessengerStarter> constructor = DivaIndexMessengerStarter.class
				.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void testMainMethod() {
		// String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);
		assertInfoMessagesForStartup();
		assertNoFatalErrorMessages();
	}

	private void assertInfoMessagesForStartup() {
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 0),
				"DivaIndexMessengerStarter starting...");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 1),
				"Sending indexOrders to: args-someBaseUrl using appToken from: args-someAppTokenVerifierUrl");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 2),
				"Will listen for index messages from: args-dev-diva-drafts using port: args-61617");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 3),
				"DivaIndexMessengerStarter started");
		assertEquals(loggerFactorySpy.getNoOfInfoLogMessagesUsingClassname(testedClassName), 4);
	}

	private void assertNoFatalErrorMessages() {
		LoggerSpy loggerSpy = loggerFactorySpy.createdLoggers.get(testedClassName);
		assertNotNull(loggerSpy);
	}

	// @Test
	// public void testMainMethodWithoutPropertiesFileNameShouldUseDefaultFilename() {
	// String args[] = new String[] {};
	// DivaIndexMessengerStarter.main(args);
	// assertNoFatalErrorMessages();
	// }

	@Test
	public void testMainMethodCoraClientFactorySetUpCorrectly() throws Exception {
		// String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = DivaIndexMessengerStarter.indexMessengerListener;
		CoraClientFactoryImp coraClientFactory = (CoraClientFactoryImp) messageListener
				.getCoraClientFactory();

		// assert same as in divaindexer.properties
		assertEquals(coraClientFactory.getAppTokenVerifierUrl(), "args-someAppTokenVerifierUrl");
		assertEquals(coraClientFactory.getBaseUrl(), "args-someBaseUrl");
	}

	@Test
	public void testMainMethodMessageParserFactorySetUpCorrectly() throws Exception {
		// String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = DivaIndexMessengerStarter.indexMessengerListener;
		assertTrue(messageListener.getMessageParserFactory() instanceof DivaMessageParserFactory);
	}

	@Test
	public void testMainMethodMessagingRoutingInfoSetUpCorrectly()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		// String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = DivaIndexMessengerStarter.indexMessengerListener;
		JmsMessageRoutingInfo messagingRoutingInfo = (JmsMessageRoutingInfo) messageListener
				.getMessageRoutingInfo();
		// // assert same as in divaindexer.properties
		assertNotNull(messagingRoutingInfo);
		assertEquals(messagingRoutingInfo.hostname, "args-dev-diva-drafts");
		assertEquals(messagingRoutingInfo.port, "args-61617");
		assertEquals(messagingRoutingInfo.routingKey, "args-fedora.apim.*");
		assertEquals(messagingRoutingInfo.username, "args-admin");
		assertEquals(messagingRoutingInfo.password, "args-admin");

	}

	@Test
	public void testMainMethodCoraCredentialsSetUpCorrectly()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		// String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = DivaIndexMessengerStarter.indexMessengerListener;
		CoraCredentials credentials = messageListener.getCredentials();

		// assert same as in divaindexer.properties
		assertEquals(credentials.userId, "args-userIdForCora");
		assertEquals(credentials.appToken, "args-appTokenForCora");
	}

	// @Test
	// public void testErrorHandling() throws Exception {
	// String args[] = new String[] { "notEnoughParameters" };
	//
	// DivaIndexMessengerStarter.main(args);
	//
	// assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 1);
	// assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
	// "Unable to start DivaIndexMessengerStarter, number of arguments should be 9.");
	// }

	// @Test
	// public void testErrorHandlingNoAppTokenVerifierUrl() throws Exception {
	//
	// String fileName = "propertiesForTestingMissingParameterApptokenUrl.properties";
	// String propertyName = "appTokenVerifierUrl";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }
	//
	// private void testPropertiesErrorWhenPropertyIsMissing(String fileName, String propertyName) {
	// String args[] = new String[] { fileName };
	//
	// DivaIndexMessengerStarter.main(args);
	// assertCorrectErrorForMissingProperty(propertyName);
	// }
	//
	// @Test
	// public void testErrorHandlingNoBaseUrl() throws Exception {
	// String fileName = "propertiesForTestingMissingParameterBaseUrl.properties";
	// String propertyName = "baseUrl";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }
	//
	// @Test
	// public void testPropertiesErrorWhenHostnameIsMissing() {
	// String propertyName = "messaging.hostname";
	// String fileName = "propertiesForTestingMissingParameterHostname.properties";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }
	//
	// @Test
	// public void testPropertiesErrorWhenPortIsMissing() {
	// String fileName = "propertiesForTestingMissingParameterPort.properties";
	// String propertyName = "messaging.port";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }
	//
	// @Test
	// public void testPropertiesErrorWhenRoutingKeyIsMissing() {
	// String propertyName = "messaging.routingKey";
	// String fileName = "propertiesForTestingMissingParameterRoutingKey.properties";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }
	//
	// @Test
	// public void testPropertiesErrorWhenVirtualHostIsMissing() {
	// String propertyName = "messaging.username";
	// String fileName = "propertiesForTestingMissingParameterUsername.properties";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }
	//
	// @Test
	// public void testPropertiesErrorWhenExchangeIsMissing() {
	// String propertyName = "messaging.password";
	// String fileName = "propertiesForTestingMissingParameterPassword.properties";
	// testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	// }

	private void assertCorrectErrorForMissingProperty(String propertyName) {
		assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 1);
		Exception exception = loggerFactorySpy.getFatalLogErrorUsingClassNameAndNo(testedClassName,
				0);
		assertTrue(exception instanceof RuntimeException);
		assertEquals(exception.getMessage(),
				"Property with name " + propertyName + " not found in properties");
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"Unable to start DivaIndexMessengerStarter ");
	}

	@Test
	public void testMainMethodMessagingRoutingInfoSetUpCorrectlyFromFile()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = DivaIndexMessengerStarter.indexMessengerListener;
		JmsMessageRoutingInfo messagingRoutingInfo = (JmsMessageRoutingInfo) messageListener
				.getMessageRoutingInfo();
		// // assert same as in divaindexer.properties
		assertNotNull(messagingRoutingInfo);
		assertEquals(messagingRoutingInfo.hostname, "dev-diva-drafts");
		assertEquals(messagingRoutingInfo.port, "61617");
		assertEquals(messagingRoutingInfo.routingKey, "fedora.apim.*");
		assertEquals(messagingRoutingInfo.username, "admin");
		assertEquals(messagingRoutingInfo.password, "admin");

	}

	@Test
	public void testMainMethodCoraCredentialsSetUpCorrectlyFromFile()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		String args[] = new String[] { "divaIndexer.properties" };
		DivaIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = DivaIndexMessengerStarter.indexMessengerListener;
		CoraCredentials credentials = messageListener.getCredentials();

		// assert same as in divaindexer.properties
		assertEquals(credentials.userId, "userIdForCora");
		assertEquals(credentials.appToken, "appTokenForCora");
	}

}
