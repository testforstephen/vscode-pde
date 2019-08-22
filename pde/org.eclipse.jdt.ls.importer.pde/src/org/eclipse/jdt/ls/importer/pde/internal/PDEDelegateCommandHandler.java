package org.eclipse.jdt.ls.importer.pde.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.launching.IPDELauncherConstants;

public class PDEDelegateCommandHandler implements IDelegateCommandHandler {
	public static final String RESOLVE_LAUNCH_ARGUMENTS = "java.pde.resolveLaunchArguments";
	public static final String RELOAD_TARGET_PLATFORM = "java.pde.reloadTargetPlatform";

	@Override
	public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor monitor) throws Exception {
		switch (commandId) {
			case RESOLVE_LAUNCH_ARGUMENTS:
				String launchFileUri = (String) arguments.get(0);
				return resolveLaunchArguments(launchFileUri);
			case RELOAD_TARGET_PLATFORM:
				String targetUri = (String) arguments.get(0);
				reloadTargetPlatform(targetUri, monitor);
				return null;
			default:
				break;
		}

		throw new UnsupportedOperationException(String.format("PDE plugin doesn't support the command '%s'.", commandId));
	}

	private static Object resolveLaunchArguments(String launchFileUri) throws Exception {
		try {
			File file = Paths.get(new URI(launchFileUri)).toFile();
			IPath path = new Path(file.getName());
			String fileName = file.getName();
			if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equals(path.getFileExtension())) {
				fileName = path.removeFileExtension().toString();
			} else if (ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION.equals(path.getFileExtension())) {
				fileName = path.removeFileExtension().toString();
			}

			ILaunchConfiguration configuration = new PDELaunchConfiguration(fileName, file);
			String configType = configuration.getType().getIdentifier();
			if (!configType.equals(IPDELauncherConstants.ECLIPSE_APPLICATION_LAUNCH_CONFIGURATION_TYPE)) {
				throw new UnsupportedOperationException(String.format("PDE plugin doesn't support the launch type '%s'.", configType));
			}

			EclipseApplicationLaunchConfiguration pdeLaunchConfiguration = new EclipseApplicationLaunchConfiguration();
			pdeLaunchConfiguration.preLaunchCheck(configuration, (ILaunch) null, new NullProgressMonitor());
			LaunchArguments launchArguments = new LaunchArguments();
			launchArguments.setVmArguments(pdeLaunchConfiguration.getVMArguments(configuration));
			launchArguments.setProgramArguments(pdeLaunchConfiguration.getProgramArguments(configuration));
			launchArguments.setEnvironment(pdeLaunchConfiguration.getEnvironmentVariable(configuration));
			launchArguments.setClasspath(pdeLaunchConfiguration.getClasspath(configuration));
			launchArguments.setWorkspaceLocation(pdeLaunchConfiguration.getWorkspaceLocation());
			return launchArguments;
		} catch (URISyntaxException | CoreException e) {
			throw new Exception("Failed to parse the launch configuration", e);
		}
	}

	private static void reloadTargetPlatform(String targetUri, IProgressMonitor monitor) throws Exception {
		// set target platform
		ITargetPlatformService service = PDEImporterActivator.acquireService(ITargetPlatformService.class);

		// increase the connection timeouts for slow connections
		ensureMimimalTimeout("sun.net.client.defaultConnectTimeout", 10000);
		ensureMimimalTimeout("sun.net.client.defaultReadTimeout", 600000);

		try {
			ITargetHandle targetHandle = service.getTarget(new URI(targetUri));
			ITargetDefinition targetDefinition = targetHandle.getTargetDefinition();
			LoadTargetDefinitionJob.load(targetDefinition);
		} catch (URISyntaxException | CoreException e) {
			throw new Exception("Failed to reload target platform", e);
		}
	}

	private static void ensureMimimalTimeout(String property, int min) {
		String current = System.getProperty(property);
		if (parseInt(current, 0) < min) {
			System.setProperty(property, String.valueOf(min));
		}
	}

	private static int parseInt(String value, int dflt) {
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return dflt;
	}

	private static class LaunchArguments {
		String[] vmArguments;
		String[] programArguments;
		Map<String, String> environment;
		String workspaceLocation;
		String[] classpath;

		public LaunchArguments() {
		}

		public void setVmArguments(String[] vmArguments) {
			this.vmArguments = vmArguments;
		}

		public void setProgramArguments(String[] programArguments) {
			this.programArguments = programArguments;
		}

		public void setEnvironment(Map<String, String> environment) {
			this.environment = environment;
		}

		public void setWorkspaceLocation(String workspaceLocation) {
			this.workspaceLocation = workspaceLocation;
		}

		public void setClasspath(String[] classpath) {
			this.classpath = classpath;
		}
    }
}
