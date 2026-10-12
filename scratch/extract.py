import json

path = r"C:\Users\arunp\.gemini\antigravity-ide\brain\cb944a79-384d-4165-9756-cb84231f3f45\.system_generated\logs\transcript_full.jsonl"
with open(path, "r", encoding="utf-8") as f:
    for line in f:
        if '"step_index":2219' in line:
            obj = json.loads(line)
            with open(r"e:\MultiPhysics\scratch\task_prompt.txt", "w", encoding="utf-8") as out:
                out.write(obj.get("content", ""))
            print("Extracted successfully!")
            break
