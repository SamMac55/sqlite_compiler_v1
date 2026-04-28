import subprocess
import sys

db_file = sys.argv[1]

result = subprocess.run(
    ["sqlite3", db_file, ".fullschema"],
    capture_output=True,
    text=True
)

with open("data/schema.txt", "w") as f:
    f.write(result.stdout)