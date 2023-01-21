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

public class JunitLaunchTemplate {

	//@formatter:off
	public static final String TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
			"<launchConfiguration type=\"org.eclipse.pde.ui.JunitLaunchConfig\">\n" +
			"    <booleanAttribute key=\"append.args\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"askclear\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"automaticAdd\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"automaticValidate\" value=\"true\"/>\n" +
			"    <stringAttribute key=\"bootstrap\" value=\"\"/>\n" +
			"    <stringAttribute key=\"checked\" value=\"[NONE]\"/>\n" +
			"    <booleanAttribute key=\"clearConfig\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"clearws\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"clearwslog\" value=\"false\"/>\n" +
			"    <stringAttribute key=\"configLocation\" value=\"${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/pde-junit\"/>\n" +
			"    <booleanAttribute key=\"default\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"includeOptional\" value=\"true\"/>\n" +
			"    <stringAttribute key=\"location\" value=\"${workspace_loc}/../junit-workspace\"/>\n" +
			"    <listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\">\n" +
			"    </listAttribute>\n" +
			"    <listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\">\n" +
			"    </listAttribute>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.junit.CONTAINER\" value=\"${testContainer}\"/>\n" +
			"    <booleanAttribute key=\"org.eclipse.jdt.junit.KEEPRUNNING_ATTR\" value=\"false\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.junit.TESTNAME\" value=\"${testName}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.junit.TEST_KIND\" value=\"${testKind}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.JRE_CONTAINER\" value=\"${jreContainer}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"${testMainType}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\" value=\"-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\" value=\"${testProject}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.SOURCE_PATH_PROVIDER\" value=\"org.eclipse.pde.ui.workbenchClasspathProvider\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.VM_ARGUMENTS\" value=\"-ea\"/>\n" +
			"    <stringAttribute key=\"pde.version\" value=\"3.3\"/>\n" +
			"    <stringAttribute key=\"product\" value=\"org.eclipse.sdk.ide\"/>\n" +
			"    <booleanAttribute key=\"run_in_ui_thread\" value=\"${useUIThread}\"/>\n" +
			"    <booleanAttribute key=\"show_selected_only\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"tracing\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"useCustomFeatures\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"useDefaultConfig\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"useDefaultConfigArea\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"useProduct\" value=\"true\"/>\n" +
			"</launchConfiguration>\n" +
			"";
	//@formatter:on

	//@formatter:off
	public static final String HEADLESS_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
			"<launchConfiguration type=\"org.eclipse.pde.ui.JunitLaunchConfig\">\n" +
			"    <booleanAttribute key=\"append.args\" value=\"true\"/>\n" +
			// "    <stringAttribute key=\"application\" value=\"org.eclipse.pde.junit.runtime.coretestapplication\"/>\n" +
			"    <booleanAttribute key=\"askclear\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"automaticAdd\" value=\"true\"/>\n" +
			// "    <booleanAttribute key=\"automaticIncludeRequirements\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"automaticValidate\" value=\"false\"/>\n" +
			"    <stringAttribute key=\"bootstrap\" value=\"\"/>\n" +
			"    <stringAttribute key=\"checked\" value=\"[NONE]\"/>\n" +
			"    <booleanAttribute key=\"clearConfig\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"clearws\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"clearwslog\" value=\"false\"/>\n" +
			"    <stringAttribute key=\"configLocation\" value=\"${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/pde-junit\"/>\n" +
			"    <booleanAttribute key=\"default\" value=\"true\"/>\n" +
			// "    <setAttribute key=\"deselected_workspace_bundles\"/>\n" +
			"    <booleanAttribute key=\"includeOptional\" value=\"true\"/>\n" +
			"    <stringAttribute key=\"location\" value=\"${workspace_loc}/../junit-workspace\"/>\n" +
			"    <listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\">\n" +
			"    </listAttribute>\n" +
			"    <listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\">\n" +
			"    </listAttribute>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.junit.CONTAINER\" value=\"${testContainer}\"/>\n" +
			"    <booleanAttribute key=\"org.eclipse.jdt.junit.KEEPRUNNING_ATTR\" value=\"false\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.junit.TESTNAME\" value=\"${testName}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.junit.TEST_KIND\" value=\"${testKind}\"/>\n" +
			"    <booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_ATTR_USE_ARGFILE\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_SHOW_CODEDETAILS_IN_EXCEPTION_MESSAGES\" value=\"true\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.JRE_CONTAINER\" value=\"${jreContainer}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"${testMainType}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\" value=\"-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\" value=\"${testProject}\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.SOURCE_PATH_PROVIDER\" value=\"org.eclipse.pde.ui.workbenchClasspathProvider\"/>\n" +
			"    <stringAttribute key=\"org.eclipse.jdt.launching.VM_ARGUMENTS\" value=\"-ea\"/>\n" +
			"    <stringAttribute key=\"pde.version\" value=\"3.3\"/>\n" +
			"    <stringAttribute key=\"product\" value=\"org.eclipse.sdk.ide\"/>\n" +
			"    <booleanAttribute key=\"run_in_ui_thread\" value=\"false\"/>\n" +
			// "    <setAttribute key=\"selected_target_bundles\"/>\n" +
			// "    <setAttribute key=\"selected_workspace_bundles\">\n" +
			// "        <setEntry value=\"${testBundle}@default:default\"/>\n" +
			// "    </setAttribute>\n" +
			"    <booleanAttribute key=\"show_selected_only\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"tracing\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"useCustomFeatures\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"useDefaultConfig\" value=\"true\"/>\n" +
			"    <booleanAttribute key=\"useDefaultConfigArea\" value=\"false\"/>\n" +
			"    <booleanAttribute key=\"useProduct\" value=\"false\"/>\n" +
			"</launchConfiguration>\n" +
			"";
	//@formatter:on
}
