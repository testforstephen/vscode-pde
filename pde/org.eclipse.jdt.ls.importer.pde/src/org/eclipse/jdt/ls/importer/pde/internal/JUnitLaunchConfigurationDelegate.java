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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

public class JUnitLaunchConfigurationDelegate extends org.eclipse.pde.launching.JUnitLaunchConfigurationDelegate {

	public JUnitLaunchArguments getJUnitLaunchArguments(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		ILaunch launch = new Launch(configuration, mode, null);
		showCommandLine(configuration, mode, launch, monitor);

		String mainTypeName = verifyMainTypeName(configuration);

		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) {
			workingDirName = workingDir.getAbsolutePath();
		}

		// Environment variables
		// String[] envp = getEnvironment(configuration);

		ArrayList<String> vmArguments = new ArrayList<>();
		ArrayList<String> programArguments = new ArrayList<>();
		collectExecutionArguments(configuration, vmArguments, programArguments);
		vmArguments.addAll(Arrays.asList(DebugPlugin.parseArguments(getVMArguments(configuration, mode))));
		IJavaProject javaProject = getJavaProject(configuration);
		if (JavaRuntime.isModularProject(javaProject)) {
			vmArguments.add("--add-modules=ALL-MODULE-PATH"); //$NON-NLS-1$
		}

		// VM-specific attributes
		// Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

		// Classpath and modulepath
		String[][] classpathAndModulepath = getClasspathAndModulepath(configuration);
		String[] classpath = classpathAndModulepath[0];
		String[] modulepath = classpathAndModulepath[1];

		//		if (TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(getTestRunnerKind(configuration).getId())) {
		//			if (!configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_DONT_ADD_MISSING_JUNIT5_DEPENDENCY, false)) {
		//				if (!Arrays.stream(classpath).anyMatch(s -> s.contains("junit-platform-launcher") || s.contains("org.junit.platform.launcher"))) { //$NON-NLS-1$ //$NON-NLS-2$
		//					try {
		//						JUnitRuntimeClasspathEntry x = new JUnitRuntimeClasspathEntry("org.junit.platform.launcher", null); //$NON-NLS-1$
		//						String entryString = new ClasspathLocalizer(Platform.inDevelopmentMode()).entryString(x);
		//						int length = classpath.length;
		//						System.arraycopy(classpath, 0, classpath = new String[length + 1], 0, length);
		//						classpath[length] = entryString;
		//					} catch (IOException | URISyntaxException e) {
		//						throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		//					}
		//				}
		//			}
		//		}

		JUnitLaunchArguments launchArguments = new JUnitLaunchArguments();
		launchArguments.mainClass = mainTypeName;
		launchArguments.projectName = javaProject.getProject().getName();
		launchArguments.cwd = workingDirName;
		launchArguments.classpath = classpath;
		launchArguments.modulepath = modulepath;
		launchArguments.vmArguments = vmArguments.toArray(new String[vmArguments.size()]);
		launchArguments.programArguments = programArguments.toArray(new String[programArguments.size()]);
		launchArguments.environment = EclipseApplicationLaunchConfiguration.getEnvironmentVariable(configuration);
		launchArguments.port = launch.getAttribute(JUnitLaunchConfigurationConstants.ATTR_PORT);

		return launchArguments;
	}

	public static class JUnitLaunchArguments {
		String mainClass;
		String projectName;
		String cwd;
		String[] classpath;
		String[] modulepath;
		String[] vmArguments;
		String[] programArguments;
		Map<String, String> environment;
		String port;
	}
}
