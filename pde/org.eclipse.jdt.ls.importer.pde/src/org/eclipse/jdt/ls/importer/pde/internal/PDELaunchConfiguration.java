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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.internal.core.LaunchConfigurationInfo;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PDELaunchConfiguration extends LaunchConfiguration {
	private LaunchConfigurationInfo launchInfo;

	public PDELaunchConfiguration(String launchName, File launchFile) throws CoreException {
		super(launchName, null, false);
		this.launchInfo = new PDELaunchConfigurationInfo(launchFile);
	}

	@Override
	protected LaunchConfigurationInfo getInfo() throws CoreException {
		return this.launchInfo;
	}
}

class PDELaunchConfigurationInfo extends LaunchConfigurationInfo {
	public PDELaunchConfigurationInfo(File launchFile) throws CoreException {
		try {
			String launchXml = new String(Files.readAllBytes(launchFile.toPath()));
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			StringReader reader = new StringReader(launchXml);
			InputSource source = new InputSource(reader);
			Element root = parser.parse(source).getDocumentElement();
			initializeFromXML(root);
		} catch (ParserConfigurationException | SAXException | IOException | CoreException e) {
			// do nothing
			throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.ls.importer.pde", "Failed to parse launch file", e));
		}
	}
}
