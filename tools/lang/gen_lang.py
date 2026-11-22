"""Builds assets/ultraloot/lang/{en_us,pt_br}.json from the lang_*.py tables. Run: python tools/lang/gen_lang.py"""
import os, sys, importlib.util
here = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(here, ".."))
from gen_common import Output  # noqa: E402  (writes only changed files; --check)
L = {}
EXTRA = []  # modules with write_extra(out, lang_dir): extra generated assets (e.g. the guide book structure)
for name in sorted(os.listdir(here)):
    if name.startswith("lang_") and name.endswith(".py"):
        spec = importlib.util.spec_from_file_location(name[:-3], os.path.join(here, name))
        mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
        L.update(mod.L)
        if hasattr(mod, "write_extra"):
            EXTRA.append(mod)
args = [a for a in sys.argv[1:] if not a.startswith("--")]
out = args[0] if args else os.path.join(here, "..", "..", "src", "main", "resources", "assets", "ultraloot", "lang")
OUT = Output()
for idx, lang in ((0, "en_us"), (1, "pt_br")):
    data = {k: v[idx] for k, v in sorted(L.items())}
    OUT.json(os.path.join(out, lang + ".json"), data, ensure_ascii=False)
for mod in EXTRA:
    mod.write_extra(OUT, out)
print(len(L), "keys")
OUT.report("lang", out)
