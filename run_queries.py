import sqlite3
import csv
import sys
import subprocess

db_file = sys.argv[1]

conn = sqlite3.connect(db_file)
cursor = conn.cursor()

with open("output/output.sql") as f:
    sql = f.read()

statements = sql.split(";")

counter = 1

for stmt in statements:
    stmt = stmt.strip()
    if not stmt:
        continue

    try:
        if stmt.lower() in (".tables", ".fullschema"):
            result = subprocess.run(
                ["sqlite3", db_file],
                input=stmt,
                capture_output=True,
                text=True
            )
            with open(f"output/output_{counter}.csv", "w", newline="") as f:
                f.write(result.stdout)
        elif stmt.lower().startswith("select"):
            cursor.execute(stmt)
            rows = cursor.fetchall()
            headers = [d[0] for d in cursor.description]

            with open(f"output/output_{counter}.csv", "w", newline="") as f:
                writer = csv.writer(f)
                writer.writerow(headers)
                writer.writerows(rows)
        else:
            cursor.execute(stmt)
            conn.commit()

    except Exception as e:
        print(f"[statement {counter}] ERROR: {e}")

    counter += 1

conn.close()