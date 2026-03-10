/*******************************************************************************
 * Copyright (c) 2019 Microsoft Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Microsoft Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.importer.pde.internal;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.internal.core.LaunchConfigurationInfo;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JunitLaunchConfiguration extends LaunchConfiguration {
	private LaunchConfigurationInfo launchInfo;

	public JunitLaunchConfiguration(String launchName, TestInfo testInfo) throws CoreException {
		super(launchName, null, false);
		this.launchInfo = new JUnitLaunchConfigurationInfo(testInfo);
	}

	@Override
	protected LaunchConfigurationInfo getInfo() throws CoreException {
		return this.launchInfo;
	}
}

class JUnitLaunchConfigurationInfo extends LaunchConfigurationInfo {
	public JUnitLaunchConfigurationInfo(TestInfo testInfo) throws CoreException {
		try {
			StrSubstitutor sub = new StrSubstitutor(testInfo.toValueMap());
			String launchXml = sub.replace(testInfo.useUIThread ? JunitLaunchTemplate.TEMPLATE : JunitLaunchTemplate.HEADLESS_TEMPLATE);
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			StringReader reader = new StringReader(launchXml);
			InputSource source = new InputSource(reader);
			Element root = parser.parse(source).getDocumentElement();
			initializeFromXML(root);
		} catch (ParserConfigurationException | SAXException | IOException | CoreException e) {
			// do nothing
			throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.ls.importer.pde", "Failed to load JUnit launch configuration", e));
		}
	}
}

class TestInfo {
	public String testContainer = "";
	public String testName = "";
	public String testKind = "org.eclipse.jdt.junit.loader.junit4";
	public String testMainType = "";
	public String testProject = "";
	public String jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER";
	public String testBundle = "";
	public String vmArgs = "";
	public boolean useUIThread = false;

	public Map<String, String> toValueMap() {
		Map<String, String> valueMap = new HashMap<>();
		valueMap.put("testContainer", testContainer);
		valueMap.put("testName", testName);
		valueMap.put("testKind", testKind);
		valueMap.put("testMainType", testMainType);
		valueMap.put("jreContainer", jreContainer);
		valueMap.put("testProject", testProject);
		valueMap.put("testBundle", testBundle);
		valueMap.put("useUIThread", String.valueOf(useUIThread));
		
		if (Platform.OS_MACOSX.equals(Platform.getOS())) {
			valueMap.put("vmArgs", "-XstartOnFirstThread " + vmArgs);
		} else {
			valueMap.put("vmArgs", vmArgs);
		}
		return valueMap;
	}
}
