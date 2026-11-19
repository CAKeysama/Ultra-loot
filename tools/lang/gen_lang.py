"""Builds assets/ultraloot/lang/{en_us,pt_br}.json from the lang_*.py tables. Run: python tools/lang/gen_lang.py"""
import json, os, sys, importlib.util
here = os.path.dirname(os.path.abspath(__file__))
L = {}
for name in sorted(os.listdir(here)):
    if name.startswith("lang_") and name.endswith(".py"):
        spec = importlib.util.spec_from_file_location(name[:-3], os.path.join(here, name))
        mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
        L.update(mod.L)
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, "..", "..", "src", "main", "resources", "assets", "ultraloot", "lang")
os.makedirs(out, exist_ok=True)
for idx, lang in ((0, "en_us"), (1, "pt_br")):
    data = {k: v[idx] for k, v in sorted(L.items())}
    with open(os.path.join(out, lang + ".json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2); f.write("\n")
print(len(L), "keys")
