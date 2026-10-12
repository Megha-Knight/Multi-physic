import json

path = r"C:\Users\arunp\.gemini\antigravity-ide\brain\cb944a79-384d-4165-9756-cb84231f3f45\.system_generated\logs\transcript.jsonl"
steps = []
with open(path, "r", encoding="utf-8") as f:
    for line in f:
        try:
            obj = json.loads(line)
            if obj.get("step_index", 0) >= 2295:
                steps.append({
                    "step": obj["step_index"],
                    "type": obj.get("type"),
                    "content": str(obj.get("content", ""))[:300]
                })
        except Exception:
            pass

for s in steps[-30:]:
    print(f"Step {s['step']} [{s['type']}]: {s['content'][:200]}")
