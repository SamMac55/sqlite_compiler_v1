import subprocess
import sys

#what is the database file? (given to us by Driver)
db_file = sys.argv[1]

# run the command against the database (it is a command not a statement so we use subprocess)
result = subprocess.run(
    ["sqlite3", db_file, ".fullschema"],
    capture_output=True,
    text=True
)
#make the schema that will be parsed
with open("data/schema.txt", "w") as f:
    f.write(result.stdout)