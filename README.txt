# Packages and dependencies
For this phase of the project, no packages or dependencies are required to run the program and see the output.
In the future, sqlite will have to be installed to check the statements against the databases.

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

2. Verify:
sqlite3 --version

# How to build and run (WSL/Linux)
Start by running:
antlr4 liteQL.g4 -no-listener -visitor
CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.

**this should generate the visitor/lexer/parser that is needed for Driver.java and PrettyPrintVisitor.java**

Then run: 
javac -cp $CP *.java
to compile all of the java files

Next use any of the io examples and run it like this:
java -cp $CP Driver < io/FILENAME.txt > out.sql

See the output in the out.sql file! Right now it just pretty prints all of the commands and uses imagined databases.
