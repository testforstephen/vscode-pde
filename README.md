Eclipse PDE support for VS Code
===============================

This extension works as a plugin of [Language Support for Java by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java). It provides the ability to import Eclipse PDE projects and set up the correct target platforms.

## Building the VS Code extension
```bash
npm install 
gulp full_build
```

## Requirements
- [Language Support for Java by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java)
- [Debugger for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug)
- [Java Test Runner](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-test)

## Features
- ### Auto import Eclipse PDE projects  
  If you want to enable the PDE extension for your project, it requires you to create a `javaConfig.json` in your workspace root. This config file is used to tell the PDE extension about the locations of the sub projects and target platform file. **Notice**: Both the projects and targetPlatform are the relative path to the workspace root. See the sample below:  
  ```json
  {
    "projects": [
        "./pde/org.eclipse.jdt.ls.importer.pde"
    ],
    "targetPlatform": "./pde/target.target"
  }
  ```

- ### Reload Target Platform  
  When you right click the _*.target_ file, it will show "**Reload Target Platform**" context menu. This command will trigger a job to reload the target platform. The progress of the reload job will be showed in the status bar. Anytime your local _*.target_ file is changed, you must manually run this command to refresh the target platform.  
  ![image](https://user-images.githubusercontent.com/14052197/69699539-2295ed80-1123-11ea-963f-16f3cb88e9ca.png)

- ### Debug PDE application  
  When you right click an Eclipse _*.launch_ file, it will show "**Debug PDE application**" context menu. This command will calculate the launch parameters of the PDE application first, then persist them into _launch.json_, and finally trigger Java Debugger to launch your PDE application. <b>Notice</b>: to support this feature, you need leverage Eclipse to generate a debug configuration for your PDE application first, and then export the debug config as a _.launch_ file.  
  ![image](https://user-images.githubusercontent.com/14052197/69700561-b5d02280-1125-11ea-9832-8490c1c8cc40.png)

- ### Run/Debug JUnit Plug-in Test  
  This extension registered menus to the **Test Explorer**, and allow you to **Run/Debug a JUnit Plug-in Test**.  
  ![image](https://user-images.githubusercontent.com/14052197/69701695-73f4ab80-1128-11ea-9868-047bde08ccb7.png)

  ![image](https://user-images.githubusercontent.com/14052197/69700746-3131d400-1126-11ea-9525-90b70f823edf.png)


## Thanks  
This extension is orginally developed by [Gorkem Ercan (@gorkem)](https://github.com/gorkem) and [Martin Aeschlimann (@aeschli)](https://github.com/aeschli)
