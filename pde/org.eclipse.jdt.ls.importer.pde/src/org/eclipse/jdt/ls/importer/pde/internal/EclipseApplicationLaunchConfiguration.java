/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Originally copied from org.eclipse.pde.launching.EclipseApplicationLaunchConfiguration
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.jdt.ls.importer.pde.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.launching.AbstractPDELaunchConfiguration;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Version;

public class EclipseApplicationLaunchConfiguration extends AbstractPDELaunchConfiguration {

	// used to generate the dev classpath entries
	// key is bundle ID, value is a model
	private Map<String, IPluginModelBase> fAllBundles;

	// key is a model, value is startLevel:autoStart
	private Map<IPluginModelBase, String> fModels;

	/**
	 * To avoid duplicating variable substitution (and duplicate prompts) this
	 * variable will store the substituted workspace location.
	 */
	private String fWorkspaceLocation;

	public EclipseApplicationLaunchConfiguration() {
	}

	public String getWorkspaceLocation() {
		return fWorkspaceLocation;
	}

	@Override
	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList<String> programArgs = new ArrayList<>();

		// If a product is specified, then add it to the program args
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			String product = configuration.getAttribute(IPDELauncherConstants.PRODUCT, ""); //$NON-NLS-1$
			if (product.length() > 0) {
				programArgs.add("-product"); //$NON-NLS-1$
				programArgs.add(product);
			} else { // TODO product w/o an application and product... how to handle gracefully?
				programArgs.add("-application"); //$NON-NLS-1$
				programArgs.add(configuration.getAttribute(IPDELauncherConstants.APPLICATION, "")); //$NON-NLS-1$
			}
		} else {
			// specify the application to launch
			programArgs.add("-application"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication()));
		}

		// specify the workspace location for the runtime workbench
		if (fWorkspaceLocation == null) {
			fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		}
		if (fWorkspaceLocation.length() > 0) {
			programArgs.add("-data"); //$NON-NLS-1$
			programArgs.add(fWorkspaceLocation);
		}

		//		boolean showSplash = false;
		String productID = LaunchConfigurationHelper.getProductID(configuration);
		Properties prop = LaunchConfigurationHelper.createConfigIniFile(configuration, productID, fAllBundles, fModels, getConfigDir(configuration));
//		showSplash = prop.containsKey("osgi.splashPath") || prop.containsKey("splashLocation"); //$NON-NLS-1$ //$NON-NLS-2$
		TargetPlatformHelper.checkPluginPropertiesConsistency(fAllBundles, getConfigDir(configuration));
		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

		// add the output folder names
		programArgs.add("-dev"); //$NON-NLS-1$
		programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", fAllBundles)); //$NON-NLS-1$
		// necessary for PDE to know how to load plugins when target platform = host platform
		// see PluginPathFinder.getPluginPaths() and PluginPathFinder.isDevLaunchMode()
		IPluginModelBase base = fAllBundles.get(PDECore.PLUGIN_ID);
		if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) < 0) {
			programArgs.add("-pdelaunch"); //$NON-NLS-1$
		}

		String[] args = super.getProgramArguments(configuration);
		Collections.addAll(programArgs, args);

//		if (!programArgs.contains("-nosplash") && showSplash) { //$NON-NLS-1$
//			if (TargetPlatformHelper.getTargetVersion() >= 3.1) {
//				programArgs.add(0, "-launcher"); //$NON-NLS-1$
//
//				IPath path = null;
//				if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
//					path = new Path(TargetPlatform.getLocation()).append("Eclipse.app/Contents/MacOS/eclipse"); //$NON-NLS-1$
//				} else {
//					path = new Path(TargetPlatform.getLocation()).append("eclipse"); //$NON-NLS-1$
//					if (TargetPlatform.getOS().equals("win32")) { //$NON-NLS-1$
//						path = path.addFileExtension("exe"); //$NON-NLS-1$
//					}
//				}
//
//				programArgs.add(1, path.toOSString()); //This could be the branded launcher if we want (also this does not bring much)
//				programArgs.add(2, "-name"); //$NON-NLS-1$
//				programArgs.add(3, "Eclipse"); //This should be the name of the product //$NON-NLS-1$
//				programArgs.add(4, "-showsplash"); //$NON-NLS-1$
//				programArgs.add(5, "600"); //$NON-NLS-1$
//			} else {
//				programArgs.add(0, "-showsplash"); //$NON-NLS-1$
//				programArgs.add(1, computeShowsplashArgument());
//			}
		//		}
		return programArgs.toArray(new String[programArgs.size()]);
	}

	private String computeShowsplashArgument() {
		IPath eclipseHome = new Path(TargetPlatform.getLocation());
		IPath fullPath = eclipseHome.append("eclipse"); //$NON-NLS-1$
		return fullPath.toOSString() + " -showsplash 600"; //$NON-NLS-1$
	}

	@Override
	protected File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null) {
			fConfigDir = LaunchConfigurationHelper.getConfigurationArea(config);
		}
		return fConfigDir;
	}

	/**
	 * Clears the workspace prior to launching if the workspace exists and the
	 * option to clear it is turned on. Also clears the configuration area if that
	 * option is chosen.
	 *
	 * @param configuration
	 *            the launch configuration
	 * @param monitor
	 *            the progress monitor
	 * @throws CoreException
	 *             if unable to retrieve launch attribute values
	 * @since 3.3
	 */
	@Override
	protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		if (fWorkspaceLocation == null) {
			fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 50);

		// Clear workspace and prompt, if necessary
		if (!LauncherUtils.clearWorkspace(configuration, fWorkspaceLocation, subMon.split(25))) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		subMon.setWorkRemaining(25);
		if (subMon.isCanceled()) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false)) {
			CoreUtility.deleteContent(getConfigDir(configuration), subMon.split(25));
		}

		subMon.setWorkRemaining(0);
	}

	@Override
	public void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		//		fWorkspaceLocation = null;

		fModels = BundleLauncherHelper.getMergedBundleMap(configuration, false);
		fAllBundles = new HashMap<>(fModels.size());
		Iterator<IPluginModelBase> iter = fModels.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = iter.next();
			fAllBundles.put(model.getPluginBase().getId(), model);
		}
		validateConfigIni(configuration);
		//		super.preLaunchCheck(configuration, launch, monitor);
	}

	private void validateConfigIni(ILaunchConfiguration configuration) throws CoreException {
		if (!configuration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
			String templateLoc = configuration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, ""); //$NON-NLS-1$
			IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
			templateLoc = mgr.performStringSubstitution(templateLoc);

			File templateFile = new File(templateLoc);
			if (!templateFile.exists()) {
				if (!LauncherUtils.generateConfigIni()) {
					throw new CoreException(Status.CANCEL_STATUS);
					// with the way the launcher works, if a config.ini file is not found one will be generated automatically.
					// This check was to warn the user a config.ini needs to be generated. - bug 161265, comment #7
				}
			}
		}
	}

	@Override
	public String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String[] vmArgs = super.getVMArguments(configuration);
		IPluginModelBase base = fAllBundles.get(PDECore.PLUGIN_ID);
		if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) >= 0) { //$NON-NLS-1$
			// necessary for PDE to know how to load plugins when target platform = host platform
			// see PluginPathFinder.getPluginPaths() and PluginPathFinder.isDevLaunchMode()
			String[] result = new String[vmArgs.length + 1];
			System.arraycopy(vmArgs, 0, result, 0, vmArgs.length);
			result[vmArgs.length] = "-Declipse.pde.launch=true"; //$NON-NLS-1$
			return result;
		}
		return vmArgs;
	}

	public static Map<String, String> getEnvironmentVariable(ILaunchConfiguration configuration) throws CoreException {
		Map<String, String> configEnv = configuration.getAttribute(LaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null);
		if (configEnv == null) {
			return null;
		}
		Map<String, String> env = new HashMap<>();

		// Add variables from config
		boolean win32 = Platform.getOS().equals(Constants.OS_WIN32);
		String key = null;
		String value = null;
		Object nativeValue = null;
		String nativeKey = null;
		for (Entry<String, String> entry : configEnv.entrySet()) {
			key = entry.getKey();
			value = entry.getValue();
			// translate any string substitution variables
			if (value != null) {
				value = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
			}
			boolean added = false;
			if (win32) {
				// First, check if the key is an exact match for an existing key.
				nativeValue = env.get(key);
				if (nativeValue != null) {
					// If an exact match is found, just replace the value
					env.put(key, value);
				} else {
					// Win32 variables are case-insensitive. If an exact match isn't found, iterate to
					// check for a case-insensitive match. We maintain the key's case (see bug 86725),
					// but do a case-insensitive comparison (for example, "pAtH" will still override "PATH").
					for (Entry<String, String> nativeEntry : env.entrySet()) {
						nativeKey = (nativeEntry).getKey();
						if (nativeKey.equalsIgnoreCase(key)) {
							nativeEntry.setValue(value);
							added = true;
							break;
						}
					}
				}
			}
			if (!added) {
				env.put(key, value);
			}
		}

		return configEnv;
	}

}
