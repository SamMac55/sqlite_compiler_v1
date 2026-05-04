import sqlite3
import csv
import sys
import subprocess

#what is the database we are using? provided by Driver
db_file = sys.argv[1]

# connect to the database
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# get the output from the compiler made in driver and read is
with open("output/output.sql") as f:
    sql = f.read()

# split the statements by the ; (may change in future)
statements = sql.split(";")

#counter used for output file names
counter = 1

for stmt in statements:
    stmt = stmt.strip()
    if not stmt:
        continue

    try:
        #if the statement is a command, execute it using subprocess
        if stmt.lower() in (".tables", ".fullschema"):
            result = subprocess.run(
                ["sqlite3", db_file],
                input=stmt,
                capture_output=True,
                text=True
            )
            #and add it to an ouptut file
            with open(f"output/output_{counter}.csv", "w", newline="") as f:
                f.write(result.stdout)
        #else if it is a select statemnt
        elif stmt.lower().startswith("select"):
            # execute it and get all of the rows + headers
            cursor.execute(stmt)
            rows = cursor.fetchall()
            headers = [d[0] for d in cursor.description]
            #put the output in a csv file
            with open(f"output/output_{counter}.csv", "w", newline="") as f:
                writer = csv.writer(f)
                writer.writerow(headers)
                writer.writerows(rows)
        #otherwise (insert,delete,create,update) execute the statement (no need for output file)
        else:
            cursor.execute(stmt)
            conn.commit()

    except Exception as e:
        print(f"[statement {counter}] ERROR: {e}") #handle errors from sqlite
    #increase the counter to make a new output file later
    counter += 1
#close connection
conn.close()