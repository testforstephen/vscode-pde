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

  ![reloadTargetPlatform](https://user-images.githubusercontent.com/14052197/69771683-a9e96c80-11c8-11ea-9c51-750a94b8dd35.png)

- ### Debug PDE application  
  When you right click an Eclipse _*.launch_ file, it will show "**Debug PDE application**" context menu. This command will calculate the launch parameters of the PDE application first, then persist them into _launch.json_, and finally trigger Java Debugger to launch your PDE application. <b>Notice</b>: to support this feature, you need leverage Eclipse to generate a debug configuration for your PDE application first, and then export the debug config as a _.launch_ file.  

  ![debugPDEApplication](https://user-images.githubusercontent.com/14052197/69771753-dac9a180-11c8-11ea-97a2-bde1eca10032.png)

- ### Run/Debug JUnit Plug-in Test  
  This extension registered menus to the **Test Explorer**, and allow you to **Run/Debug a JUnit Plug-in Test**.  

  ![runTestMenu](https://user-images.githubusercontent.com/14052197/69772120-0e58fb80-11ca-11ea-865c-007b524ccae8.png)

  ![runTestExplorer](https://user-images.githubusercontent.com/14052197/69771935-7eb34d00-11c9-11ea-89f4-c2082f7b5938.png)


## Thanks  
This extension is orginally developed by [Gorkem Ercan (@gorkem)](https://github.com/gorkem) and [Martin Aeschlimann (@aeschli)](https://github.com/aeschli)