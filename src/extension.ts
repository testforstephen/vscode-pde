
import * as compareVersions from "compare-versions";
import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";

const RECENTLY_USED_PDE_LAUNCH_FILE = "recentlyUsedPdeLaunchFile";
const RECENTLY_USED_TARGET_FILE = "recentlyUsedTargetFile";
export function activate(context: vscode.ExtensionContext) {
    validateUpstreamJavaExtension();

    vscode.commands.registerCommand("java.pde.debug", (uri: vscode.Uri) => {
        launchPDEApplication(context, uri);
    });

    vscode.commands.registerCommand("java.pde.reload", async (uri: vscode.Uri) => {
        if (!uri) {
            // If no target platform file is specified, then use the recently used target file instead.
            uri = getUriCache(context, RECENTLY_USED_TARGET_FILE);
            if (!uri || !fs.existsSync(uri.fsPath)) {
                const targetPaths = await findTargets();
                if (targetPaths.length === 1) {
                    uri = vscode.Uri.file(targetPaths[0]);
                } else if (targetPaths.length > 1) {
                    const items = targetPaths.map((target) => {
                        return {
                            label: path.basename(target),
                            description: target,
                        };
                    });
                    const picked = await vscode.window.showQuickPick(items, {
                        placeHolder: "Please select a target definition file",
                    });

                    if (picked) {
                        uri = vscode.Uri.file(picked.description);
                    }
                }
            }

            if (!uri) {
                vscode.window.showErrorMessage("Please specify a pde target definition file first.");
                return;
            }
        }

        updateUriCache(context, RECENTLY_USED_TARGET_FILE, uri);
        vscode.commands.executeCommand("java.execute.workspaceCommand", "java.pde.reloadTargetPlatform", uri.toString());
    });

    vscode.commands.registerCommand("java.pde.runUnitTest", (node) => {
        launchJunitPluginTest(node, true);
    });

    vscode.commands.registerCommand("java.pde.debugUnitTest", (node) => {
        launchJunitPluginTest(node, false);
    });
}

function validateUpstreamJavaExtension() {
    const javaExtension = vscode.extensions.getExtension("redhat.java");
    if (javaExtension && compareVersions(javaExtension.packageJSON.version, "0.66.0") < 0) {
        vscode.window.showErrorMessage(`The latest PDE extension requires the [Language Support for Java](command:extension.open?%22redhat.java%22) extension 0.66.0 or later (current: ${javaExtension.packageJSON.version}). `
            + `Please update the upstream Java extension to the latest version and try again.`);
    }
}

async function findTargets(): Promise<string[]> {
    const result: string[] = [];
    const configs: vscode.Uri[] = await vscode.workspace.findFiles("javaConfig.json");
    if (configs != null) {
        for (const config of configs) {
            try {
                const configPath = config.fsPath;
                const rootPath = path.dirname(configPath);
                if (fs.existsSync(configPath)) {
                    const javaConfig = JSON.parse(fs.readFileSync(configPath).toString());
                    if (javaConfig.targetPlatform && fs.existsSync(path.join(rootPath, javaConfig.targetPlatform))) {
                        result.push(path.join(rootPath, javaConfig.targetPlatform).normalize());
                    }
                }
            } catch {
                // do nothing
            }
        }
    }

    return result;
}

async function launchPDEApplication(context: vscode.ExtensionContext, uri: vscode.Uri) {
    if (!uri) {
        // If no pde launch configuration file is specified, then use the recently used launch file instead.
        uri = getUriCache(context, RECENTLY_USED_PDE_LAUNCH_FILE);
        if (!uri || !fs.existsSync(uri.fsPath)) {
            vscode.window.showErrorMessage("Please specify a pde launch configuration file first.");
            return;
        }
    }

    const workspaceFolder: vscode.WorkspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
    if (!workspaceFolder) {
        vscode.window.showErrorMessage("No workspace folder found.");
        return;
    }

    updateUriCache(context, RECENTLY_USED_PDE_LAUNCH_FILE, uri);
    const launchArguments = <LaunchArguments> await vscode.commands.executeCommand("java.execute.workspaceCommand", "java.pde.resolveLaunchArguments", uri.toString());
    let projectName;
    const javaConfigFile = path.join(workspaceFolder.uri.fsPath, "javaConfig.json");
    if (fs.existsSync(javaConfigFile)) {
        const javaConfig = JSON.parse(fs.readFileSync(javaConfigFile).toString());
        if (javaConfig && javaConfig.projects && javaConfig.projects.length) {
            projectName = javaConfig.projects[0] || "";
            const segments = projectName.split(/[\\/]/);
            projectName = segments[segments.length - 1];
        }
    }

    const insider = vscode.version.endsWith("-insider") ? "-insider" : "";
    const launchConfiguration = {
        type: "java",
        name: path.basename(uri.fsPath) + insider,
        request: "launch",
        mainClass: "org.eclipse.equinox.launcher.Main",
        projectName,
        classPaths: launchArguments.classpath,
        args: launchArguments.programArguments,
        vmArgs: launchArguments.vmArguments,
        env: launchArguments.environment
    };

    await persistLaunchConfig(launchConfiguration, workspaceFolder.uri);
    await vscode.debug.startDebugging(workspaceFolder, launchConfiguration);
}

function getUriCache(context: vscode.ExtensionContext, key: string): vscode.Uri {
    const cache = context.workspaceState.get<string>(key);
    try {
        if (cache) {
            return vscode.Uri.parse(cache);
        }
    } catch {
        // do nothing
    }

    return undefined;
}

function updateUriCache(context: vscode.ExtensionContext, key: string, uri: vscode.Uri) {
    context.workspaceState.update(key, uri.toString());
}

async function launchJunitPluginTest(node, noDebug: boolean) {
    if (!node) {
        vscode.window.showErrorMessage("No test resources are specified.");
        return;
    }

    let uri: vscode.Uri;
    let method: string;
    if (node instanceof vscode.Uri) {
        uri = node;
    } else {
        // Currently even the test explorer can support multi-selection,
        // The node passed in is just a first selected single node element.
        // We check array here in case in the future the behavior is changed
        // upstream.
        if (Array.isArray(node)) {
            node = node[0];
        }
        uri = node.uri;
        const id: string = (node.id as string);
        const methodSeparator: number = id.indexOf('#');
        if (methodSeparator > 0) {
            method = id.slice(methodSeparator + 1);
        }
    }

    const workspaceFolder: vscode.WorkspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
    if (!workspaceFolder) {
        vscode.window.showErrorMessage("No workspace folder found.");
        return;
    }
    // the setting could be folder based, so use the uri to fetch the correct value.
    const useUIThread = await vscode.workspace.getConfiguration("java.pde.test.launch", uri).get("useUIThread");

    const debugSettings: vscode.WorkspaceConfiguration = vscode.workspace.getConfiguration("java.debug.settings");
    const customVmArgs: string[] = Array.isArray(debugSettings.vmArgs) ? debugSettings.vmArgs : parseVmArgs(debugSettings.vmArgs);
    vscode.window.withProgress({ location: vscode.ProgressLocation.Window }, async (p) => {
        p.report({ message: "Launching JUnit Plug-in Test..."});
        try {
            const launchArguments = <JUnitLaunchArguments>await vscode.commands.executeCommand("java.execute.workspaceCommand", "java.pde.resolveJUnitArguments", uri.toString(), useUIThread, method);
            const programArguments = launchArguments.programArguments;
            const launchConfiguration = {
                type: "java",
                name: path.basename(uri.fsPath),
                request: "launch",
                mainClass: launchArguments.mainClass,
                projectName: launchArguments.projectName,
                cwd: verifyWorkingDir(launchArguments.cwd),
                classPaths: launchArguments.classpath,
                modulePaths: launchArguments.modulepath,
                args: programArguments,
                vmArgs: launchArguments.vmArguments.concat(customVmArgs),
                env: launchArguments.environment,
                noDebug,
            };

            if (vscode.extensions.getExtension("vscjava.vscode-java-test") && !(node instanceof vscode.Uri)) {
                const portArgIdx: number = launchConfiguration.args.indexOf('-port');
                launchConfiguration.args.splice(portArgIdx, 2);

                if (noDebug) {
                    await vscode.commands.executeCommand("java.test.explorer.run", node, launchConfiguration);
                } else {
                    await vscode.commands.executeCommand("java.test.explorer.debug", node, launchConfiguration);
                }
            } else {
                // Print junit result to console
                launchConfiguration.args.push('-junitconsole');
                await vscode.debug.startDebugging(workspaceFolder, launchConfiguration);
            } 
        } catch (error) {
            vscode.window.showErrorMessage(error && error.message ? error.message : String(error));
        }
    });

}

function parseVmArgs(vmArgs: string): string[] {
    const argsList: string[] = [];
    let currentArg: string = '';
    let inQuotes: boolean = false;

    for (let i = 0; i < vmArgs.length; i++) {
        const c = vmArgs.charAt(i);
        if (c === '"' || c === "'") {
            inQuotes = !inQuotes;
        } else if (c === ' ' && !inQuotes) {
            if (currentArg.length > 0) {
                argsList.push(currentArg);
                currentArg = '';
            }
        } else {
            currentArg += c;
        }
    }

    if (currentArg.length > 0) {
        argsList.push(currentArg);
    }

    return argsList;
}

function verifyWorkingDir(cwd: string): string {
    if (!cwd) {
        return;
    }

    const uri: vscode.Uri = vscode.Uri.file(cwd);
    if (uri) {
        const workspaceFolder: vscode.WorkspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
        if (workspaceFolder) {
            return cwd;
        }
    }
}

async function persistLaunchConfig(configuration: vscode.DebugConfiguration, workspace: vscode.Uri): Promise<void> {
    const launchConfigurations: vscode.WorkspaceConfiguration = vscode.workspace.getConfiguration("launch", workspace);
    const rawConfigs: vscode.DebugConfiguration[] = launchConfigurations.configurations;
    const oldConfig = rawConfigs.findIndex((value => value.name === configuration.name));
    // Persist the default debug configuration only if the workspace exists.
    if (workspace) {
        // Remove the old debug configuration.
        if (oldConfig >= 0) {
            rawConfigs.splice(oldConfig, 1);
        }
        // Insert the default debug configuration to the beginning of launch.json.
        rawConfigs.splice(0, 0, configuration);
        await launchConfigurations.update("configurations", rawConfigs, vscode.ConfigurationTarget.WorkspaceFolder);
    }
}

interface LaunchArguments {
    vmArguments: string[];
    programArguments: string[];
    environment;
    workspaceLocation: string;
    classpath: string[];
}

interface JUnitLaunchArguments {
    mainClass: string;
    projectName: string;
    cwd: string;
    classpath: string[];
    modulepath: string[];
    vmArguments: string[];
    programArguments: string[];
    environment;
    port: string;
}
