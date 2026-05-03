# Packages and dependencies
- SQLite must be installed and saved to path
- Python3 must be available, and you need to be able to call a python script using "python3 SCRIPTNAME.py".
If you run python scripts using "python SCRIPTNAME.py", or some other way, the ProcessBuilder will not work as intended.
Run "python3 test.py" to test if your python is configured properly for the program.

# Installing Python3
## Windows
There are two main ways to install Python on Windows:
### Option 1: Microsoft Store
Open the Microsoft Store app.
Search for "Python" and select the latest stable version (e.g., Python 3.12 or 3.13).
Click Get or Install. This version automatically handles updates and adds Python to your system PATH
### Option 2: Official Installer
Download the installer from Python.org.
Run the .exe file.
Crucial Step: Check the box "Add Python to PATH" before clicking Install Now. 

## macOS
Download: Visit Python.org and download the latest macOS 64-bit universal2 installer.
Install: Open the downloaded .pkg file and follow the on-screen prompts.
Alternative: If you use the Homebrew package manager, simply open Terminal and run:
brew install python3. 

## Linux & WSL
Update package lists: sudo apt update.
Install Python 3: sudo apt install python3 python3-pip.

## Verification
After installing, open your terminal and type: 
python --version (Windows)
python3 --version (macOS, Linux, WSL) 

# Installing sqlite (Windows)
1. go to: https://sqlite.org/download.html
    a. for windows download the "sqlite-tools-win-x64-3530000.zip" file and extract the files into a folder
    Note: save this folder path for later steps

2. add the folder path to your PATH variable in environment variables system variables

3. verify by going to command prompt and typing in "sqlite3" you should get something like this: 
        SQLite version 3.51.2 2026-01-09 17:27:48
        Enter ".help" for usage hints.
        Connected to a transient in-memory database.
        Use ".open FILENAME" to reopen on a persistent database.
        sqlite>
    Note: use CTRL^C twice to exit. 

# Installing sqlite (MacOS)
1. Via Terminal: SQLite is usually pre-installed. You can update it or install it using Homebrew:
brew install sqlite

2. Via Web: Download the command-line tool archive for Mac from sqlite.org/download.html and run the executable. 

OR watch this video: https://www.youtube.com/watch?v=4MJSZi4qvIE

# Installing sqlite (Linux/WSL)
1. Use: 
sudo apt update
sudo apt install sqlite3

2. Verify that this works:
sqlite3 --version

# How to build and run (WSL/Linux)
1. Start by running:
antlr4 liteQL.g4 -no-listener -visitor
antlr4 schema_grammar.g4 -no-listener -visitor
CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.

**this should generate the visitor/lexer/parser that is needed for Driver.java, CreateSchemaVisitor.java and TreeBuilderVisitor.java**

**Ensure you are at the root of the project before running**
2. Then run:
javac -cp $CP *.java 
to compile all of the java files, this is important because SelectNode.java may not compile properly if javac -cp $CP Driver.java is run

3. Next use any of the input examples, its corresponding database, and run it like this:
java -cp $CP Driver "data/DATABASENAME.db" < input/FILENAME.txt

**note that each input file has the name of the database in it. the three databases are:
hr.db, animals.db,songs.db, and you can view their Schemas in the corresponding DATABASENAME_script.sql file**

4. See the output in the output/output.sql file and output_#.csv files!
output.sql shows how the statements were compiled
.csv files show how the statements executed against the database that you ran