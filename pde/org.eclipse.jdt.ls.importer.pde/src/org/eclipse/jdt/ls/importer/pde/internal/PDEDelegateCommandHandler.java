package org.eclipse.jdt.ls.importer.pde.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.launching.IPDELauncherConstants;

public class PDEDelegateCommandHandler implements IDelegateCommandHandler {
	public static final String RESOLVE_LAUNCH_ARGUMENTS = "java.pde.resolveLaunchArguments";
	public static final String RELOAD_TARGET_PLATFORM = "java.pde.reloadTargetPlatform";
	public static final String RESOLVE_JUNIT_ARGUMENTS = "java.pde.resolveJUnitArguments";

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
			case RESOLVE_JUNIT_ARGUMENTS:
				String testFileUri = (String) arguments.get(0);
				String method = null;
				if (arguments.size() > 1) {
					method = (String) arguments.get(1);
				}
				return resolveJunitArguments(testFileUri, method, monitor);
			default:
				break;
		}

		throw new UnsupportedOperationException(String.format("PDE plugin doesn't support the command '%s'.", commandId));
	}

	private static Object resolveLaunchArguments(String launchFileUri) throws Exception {
		try {
			File file = Paths.get(new URI(launchFileUri)).toFile();
			String fileName = getSimpleName(file);
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

	private static String getSimpleName(File file) {
		IPath path = new Path(file.getName());
		String fileName = file.getName();
		if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equals(path.getFileExtension())) {
			fileName = path.removeFileExtension().toString();
		} else if (ILaunchConfiguration.LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION.equals(path.getFileExtension())) {
			fileName = path.removeFileExtension().toString();
		}

		return fileName;
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

	private static Object resolveJunitArguments(String testFileUri, String method, IProgressMonitor monitor) throws Exception {
		File file = Paths.get(new URI(testFileUri)).toFile();
		String simpleName = getSimpleName(file);
		String jreVersion = System.getProperty("java.specification.version");
		if (StringUtils.isBlank(jreVersion)) {
			jreVersion = "1.8";
		}
		String jreContainer = "JavaSE-" + jreVersion;
		if (file.isFile()) {
			ICompilationUnit cu = JDTUtils.resolveCompilationUnit(testFileUri);
			if (cu == null || cu.findPrimaryType() == null) {
				throw new Exception("No test class found in the file " + testFileUri);
			}

			IType type = cu.findPrimaryType();
			String testMainType = type.getFullyQualifiedName();
			String testProject = type.getJavaProject().getProject().getName();
			TestInfo testInfo = new TestInfo();
			testInfo.testKind = "org.eclipse.jdt.junit.loader.junit4";
			testInfo.testMainType = testMainType;
			testInfo.testName = StringUtils.isBlank(method) ? "" : method;
			testInfo.testProject = testProject;
			testInfo.jreContainer = jreContainer;
			if (!StringUtils.isBlank(method)) {
				simpleName += "." + method;
			}
			ILaunchConfiguration configuration = new JunitLaunchConfiguration(simpleName, testInfo);
			JUnitLaunchConfigurationDelegate delegate = new JUnitLaunchConfigurationDelegate();
			return delegate.getJUnitLaunchArguments(configuration, "run", monitor);
		} else if (file.isDirectory()) {
			IContainer[] targetContainers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(new URI(testFileUri));
			if (targetContainers == null || targetContainers.length == 0) {
				throw new Exception("No test cases found in the folder " + simpleName);
			}

			// For multi-module scenario, findContainersForLocationURI API may return a container array, need put the result from the nearest project in front.
			Arrays.sort(targetContainers, (Comparator<IContainer>) (IContainer a, IContainer b) -> {
				return a.getFullPath().toPortableString().length() - b.getFullPath().toPortableString().length();
			});

			IJavaElement targetElement = null;
			for (IContainer container : targetContainers) {
				targetElement = JavaCore.create(container);
				if (targetElement != null) {
					break;
				}
			}

			if (targetElement == null) {
				throw new Exception("The folder " + simpleName + " is not testable. Only package, project root and source folder are testable.");
			}

			TestInfo testInfo = new TestInfo();
			testInfo.testKind = "org.eclipse.jdt.junit.loader.junit4";
			testInfo.testContainer = StringEscapeUtils.escapeXml(targetElement.getHandleIdentifier());
			testInfo.jreContainer = jreContainer;
			testInfo.testProject = targetElement.getJavaProject().getProject().getName();
			ILaunchConfiguration configuration = new JunitLaunchConfiguration(simpleName, testInfo);
			JUnitLaunchConfigurationDelegate delegate = new JUnitLaunchConfigurationDelegate();
			return delegate.getJUnitLaunchArguments(configuration, "run", monitor);
		}

		throw new Exception("The resource is not testable.");
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
