/*******************************************************************************
 * Copyright (c) 2017 Microsoft Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Microsoft Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.importer.pde.internal;

import static org.eclipse.core.resources.IProjectDescription.DESCRIPTION_FILE_NAME;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.ls.core.internal.AbstractProjectImporter;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class PDEProjectImporter extends AbstractProjectImporter {

	public static final String CONFIG_FILENAME = "javaConfig.json";

	private File getPluginWorkspaceFile() {
		return new File(rootFolder, CONFIG_FILENAME);
	}

	@Override
	public boolean applies(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		return getPluginWorkspaceFile().exists();
	}

	@Override
	public void importToWorkspace(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		try {
			String targetPlatform = null;
			String eclipseInstallation = null;
			List<String> projects = new ArrayList<>();

			FileReader fileReader = new FileReader(getPluginWorkspaceFile());
			try {
				JsonReader reader = new Gson().newJsonReader(fileReader);
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("targetPlatform")) {
						targetPlatform = reader.nextString();
					} else if (name.equals("projects")) {
						reader.beginArray();
						while (reader.hasNext()) {
							projects.add(reader.nextString());
						}
						reader.endArray();
					} else if (name.equals("eclipseInstallation")) {
						eclipseInstallation = reader.nextString();
					} else {
						reader.skipValue();
					}
				}
			} finally {
				fileReader.close();
			}

			SubMonitor subMonitor = SubMonitor.convert(monitor, projects.size() + 50);

			ITargetDefinition targetDefinition = null;
			if (targetPlatform != null && eclipseInstallation == null) {
				targetDefinition = getTargetDefinitionFromDotTarget(targetPlatform, subMonitor);
			} else if (eclipseInstallation != null && targetPlatform == null) {
				targetDefinition = getTargetDefinitionFromEclipseInstallation(eclipseInstallation, subMonitor);
			} else if (targetPlatform != null && eclipseInstallation != null) {
				targetDefinition = getTargetDefinitionFromComposite(targetPlatform, eclipseInstallation, subMonitor);
			}

			if (targetDefinition != null) {
				new LoadTargetDefinitionJob(targetDefinition).runInWorkspace(subMonitor.split(50));
	
				// import projects
				EclipseProjectImporter importer = new EclipseProjectImporter();
				for (String project : projects) {
					File projectFolder = new File(rootFolder, project);
					if (projectFolder.exists() && new File(projectFolder, DESCRIPTION_FILE_NAME).exists()) {
						importer.importDir(projectFolder.toPath(), subMonitor.split(1));
					} else {
						PDEImporterActivator.logError("Project " + projectFolder.toPath() + " does not exist. Ignoring.");
					}
				}
			}

		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEImporterActivator.PLUGIN_ID, "Problems reading " + CONFIG_FILENAME, e);
			throw new CoreException(status);
		}
	}

	private ITargetDefinition getTargetDefinitionFromDotTarget(String targetPlatform, SubMonitor monitor) throws CoreException {
		// set target platform
		ITargetPlatformService service = PDEImporterActivator.acquireService(ITargetPlatformService.class);
		monitor.setTaskName("Loading target platform...");

		// increase the connection timeouts for slow connections
		ensureMimimalTimeout("sun.net.client.defaultConnectTimeout", 10000);
		ensureMimimalTimeout("sun.net.client.defaultReadTimeout", 600000);

		URI projectFolderURI = new File(rootFolder, targetPlatform).getAbsoluteFile().toURI();
		ITargetHandle targetHandle = service.getTarget(projectFolderURI);
		return targetHandle.getTargetDefinition();
	}

	private ITargetDefinition getTargetDefinitionFromEclipseInstallation(String eclipseInstallation, SubMonitor monitor) throws CoreException {
		// set target platform
		ITargetPlatformService service = PDEImporterActivator.acquireService(ITargetPlatformService.class);
		monitor.setTaskName("Loading target platform...");

		// increase the connection timeouts for slow connections
		ensureMimimalTimeout("sun.net.client.defaultConnectTimeout", 10000);
		ensureMimimalTimeout("sun.net.client.defaultReadTimeout", 600000);

		Path eclipseInstallationPath = Path.of(eclipseInstallation);
		if (!eclipseInstallationPath.isAbsolute()) {
			eclipseInstallationPath = Path.of(rootFolder.toString()).resolve(eclipseInstallationPath).normalize().toAbsolutePath();
		}

		ITargetDefinition targetDefinition = service.newTarget();
		ITargetLocation container = new ProfileBundleContainer(eclipseInstallationPath.toString(), eclipseInstallationPath.resolve("configuration").toString());
		targetDefinition.setTargetLocations(new ITargetLocation[] { container });
		targetDefinition.setName("Specified Eclipse installation");

		// initialize environment with default settings
		targetDefinition.setArch(Platform.getOSArch());
		targetDefinition.setOS(Platform.getOS());
		targetDefinition.setWS(Platform.getWS());
		targetDefinition.setNL(Platform.getNL());

		return targetDefinition;
	}

	private ITargetDefinition getTargetDefinitionFromComposite(String targetPlatform, String eclipseInstallation, SubMonitor monitor) throws CoreException {
		ITargetDefinition targetDefinition = getTargetDefinitionFromDotTarget(targetPlatform, monitor);

		ITargetLocation[] oldLocations = targetDefinition.getTargetLocations();
		ITargetLocation[] newLocations = new ITargetLocation[oldLocations.length + 1];
		System.arraycopy(oldLocations, 0, newLocations, 0, oldLocations.length);

		Path eclipseInstallationPath = Path.of(eclipseInstallation);
		if (!eclipseInstallationPath.isAbsolute()) {
			eclipseInstallationPath = Path.of(rootFolder.toString()).resolve(eclipseInstallationPath).normalize().toAbsolutePath();
		}

		ITargetLocation eclipseContainer = new ProfileBundleContainer(eclipseInstallationPath.toString(), eclipseInstallationPath.resolve("configuration").toString());
		newLocations[oldLocations.length] = eclipseContainer;

		return targetDefinition;
	}

	private void ensureMimimalTimeout(String property, int min) {
		String current = System.getProperty(property);
		if (parseInt(current, 0) < min) {
			System.setProperty(property, String.valueOf(min));
		}
	}

	private int parseInt(String value, int dflt) {
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return dflt;
	}

	@Override
	public void reset() {
	}

}
