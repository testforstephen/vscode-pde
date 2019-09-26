
import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";

const RECENTLY_USED_PDE_LAUNCH_FILE = "recentlyUsedPdeLaunchFile";
export function activate(context: vscode.ExtensionContext) {

    vscode.commands.registerCommand("java.pde.debug", (uri: vscode.Uri) => {
        launchPDEApplication(context, uri);
    });

    vscode.commands.registerCommand("java.pde.reload", (uri: vscode.Uri) => {
        if (!uri) {
            vscode.window.showErrorMessage("Please specify a pde target definition file first.");
            return;
        }
        vscode.commands.executeCommand("java.execute.workspaceCommand", "java.pde.reloadTargetPlatform", uri.toString());
    });

    vscode.commands.registerCommand("java.pde.runUnitTest", (node: any) => {
        launchJunitPluginTest(node, true);
    });

    vscode.commands.registerCommand("java.pde.debugUnitTest", (node: any) => {
        launchJunitPluginTest(node, false);
    });
}

async function launchPDEApplication(context: vscode.ExtensionContext, uri: vscode.Uri) {
    if (!uri) {
        // If no pde launch configuration file is specified, then use the recently used launch file instead.
        uri = context.workspaceState.get<vscode.Uri>(RECENTLY_USED_PDE_LAUNCH_FILE);
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

    context.workspaceState.update(RECENTLY_USED_PDE_LAUNCH_FILE, uri);
    const launchArguments = <LaunchArguments> await vscode.commands.executeCommand("java.execute.workspaceCommand", "java.pde.resolveLaunchArguments", uri.toString());
    let projectName;
    const javaConfigFile = path.join(workspaceFolder.uri.fsPath, "javaConfig.json");
    if (fs.existsSync(javaConfigFile)) {
        const javaConfig = JSON.parse(fs.readFileSync(javaConfigFile).toString());
        if (javaConfig && javaConfig.projects && javaConfig.projects.length) {
            projectName = javaConfig.projects[0] || "";
            const segments = projectName.split(/[\\\/]/);
            projectName = segments[segments.length - 1];
        }
    }

    let insider = vscode.version.endsWith("-insider") ? "-insider" : "";
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

async function launchJunitPluginTest(node: any, noDebug: boolean) {
    if (!node) {
        vscode.window.showErrorMessage("No test resources are specified.");
        return;
    }

    let uri: vscode.Uri;
    let method: string;
    if (node instanceof vscode.Uri) {
        uri = node;
    } else {
        uri = vscode.Uri.file(node.fsPath);
        if (node.isMethod) {
            method = node.name;
        }
    }

    const workspaceFolder: vscode.WorkspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
    if (!workspaceFolder) {
        vscode.window.showErrorMessage("No workspace folder found.");
        return;
    }

    vscode.window.withProgress({ location: vscode.ProgressLocation.Window }, (p) => {
        return new Promise(async (resolve, reject) => {
            p.report({ message: "Launching JUnit Plug-in Test..."});
            try {
                const launchArguments = <JUnitLaunchArguments> await vscode.commands.executeCommand("java.execute.workspaceCommand", "java.pde.resolveJUnitArguments", uri.toString(), method);
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
                    vmArgs: launchArguments.vmArguments,
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
            } finally {
                resolve();
            }
        });
    });

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
    environment: any;
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
    environment: any;
    port: string;
}
