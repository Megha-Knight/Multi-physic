import zipfile
import os
import shutil

repo_dir = r"e:\MultiPhysics"
out_zip1 = r"C:\Users\arunp\.gemini\antigravity-ide\brain\cb944a79-384d-4165-9756-cb84231f3f45\MultiPhysics_Phase2_Repaired.zip"
out_zip2 = r"e:\MultiPhysics\MultiPhysics_Phase2_Repaired.zip"

excluded_dirs = {".git", "target", ".system_generated", "tmp"}
excluded_extensions = {".class"}

def should_include(rel_path):
    parts = rel_path.replace("\\", "/").split("/")
    if any(p in excluded_dirs for p in parts):
        return False
    if rel_path.endswith((".zip", ".tar.gz", ".bak")):
        return False
    if parts[0].startswith("hs_err_") or parts[0].startswith("replay_"):
        return False
    _, ext = os.path.splitext(rel_path)
    if ext in excluded_extensions:
        return False
    return True

print(f"Creating {out_zip1}...")
count = 0
with zipfile.ZipFile(out_zip1, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(repo_dir):
        # filter dirs in place
        dirs[:] = [d for d in dirs if d not in excluded_dirs]
        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, repo_dir).replace("\\", "/")
            if should_include(rel_path):
                zf.write(full_path, rel_path)
                count += 1

print(f"Added {count} files to {out_zip1}.")
shutil.copyfile(out_zip1, out_zip2)
print(f"Copied deliverable to {out_zip2}.")
print(f"Archive size: {os.path.getsize(out_zip1):,} bytes")
