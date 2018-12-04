## JDT LS extension for importing Eclipse plugin projects

This projects hosts an extension to [JDT.LS](https://github.com/eclipse/eclipse.jdt.ls) and 
an accompanying vscode extension that is capable of importing Eclipse PDE projects and set up
the correct target platforms. 

### Building the vscode extension
```bash
cd vscode-pde
npm install
npm run build-server
vsce package 
```
