import zipfile
import os

zip_path = r"C:\Users\arunp\.gemini\antigravity-ide\brain\cb944a79-384d-4165-9756-cb84231f3f45\MultiPhysics_Current.zip"
repo_path = r"e:\MultiPhysics"

zf = zipfile.ZipFile(zip_path, "r")
zip_names = set(zf.namelist())
prefix = ""
for n in zip_names:
    if n.endswith("pom.xml"):
        prefix = n[:-7]
        break

modified_in_permitted = []
modified_outside = []

for root, dirs, files in os.walk(repo_path):
    if any(k in root for k in [".git", "target", "scratch", ".system_generated", ".agents"]):
        continue
    for file in files:
        full_path = os.path.join(root, file)
        rel = os.path.relpath(full_path, repo_path).replace("\\", "/")
        zip_entry = prefix + rel
        if zip_entry in zip_names:
            with open(full_path, "rb") as f:
                cur_data = f.read()
            zip_data = zf.read(zip_entry)
            if cur_data != zip_data:
                if rel.startswith("src/main/java/ui/") or rel.startswith("src/main/resources/"):
                    modified_in_permitted.append(rel)
                else:
                    modified_outside.append(rel)
        else:
            if rel.startswith("src/main/java/ui/") or rel.startswith("src/main/resources/"):
                modified_in_permitted.append("(NEW) " + rel)
            else:
                modified_outside.append("(NEW) " + rel)

zf.close()

print(f"Total modified files in permitted dirs: {len(modified_in_permitted)}")
for m in sorted(modified_in_permitted):
    print("  ", m)

print(f"\nTotal modified files OUTSIDE permitted dirs: {len(modified_outside)}")
for o in sorted(modified_outside):
    print("  ", o)
