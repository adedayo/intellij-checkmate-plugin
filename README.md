# CheckMate Security Analysis Plugin for IntelliJ

CheckMate (https://github.com/adedayo/checkmate) is a static security analysis tool. This repository
contains code for the IntelliJ plugin wrapper that allows CheckMate to be used within IntelliJ IDEs.

## Installation
The plugin is published in the JetBrain IntelliJ plugin marketplace and can be installed
in the usual manner. 

![Plugin Install](docs/images/install.png)
 
 ## Usage
 
 The IntelliJ plugin is implemented as a set of code inspection and annotators. 
 To invoke an inspection press  Ctrl+Alt+Shift+I  (Cmd+Alt+Shift+I on macos) and search for its description. 
 
 ![Inspection](docs/images/inspection.png)
 
 Alternatively, you can find them under the menu _Analyze → CheckMate Security → Find Secrets_
 
 Select the scope of search and run the scan. You will get annotated results similar to the following
 
 ![Scan results](docs/images/results.png)
 
 Each result comes with diagnostic information such as
 ![A diagnostic](docs/images/diagnostics.png)
 
 Each issue found also provides diagnostics that could be used to ignore the issue or file for future scans, 
 or to annotate the issue as a "TODO" for a future fix whithout suppressing it from the results.
 ![Quick fix](docs/images/quick_fix.png) 