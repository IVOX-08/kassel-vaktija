# One-off: insert about_dev_email after about_dev_promo in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STRINGS = {
    "values":    "ili pišite e-mail:",
    "values-de": "oder E-Mail schreiben:",
    "values-en": "or write an email:",
    "values-ar": "أو راسلنا عبر البريد الإلكتروني:",
    "values-tr": "veya e-posta yazın:",
    "values-sq": "ose shkruani një email:",
    "values-ur": "یا ای میل کریں:",
    "values-ru": "или напишите на эл. почту:",
}

ANCHOR = 'name="about_dev_promo"'

for folder, label in STRINGS.items():
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("about_dev_email" in l for l in lines):
        print(f"{folder}: already present, skipped")
        continue
    out, inserted = [], False
    for line in lines:
        out.append(line)
        if ANCHOR in line and not inserted:
            out.append(f'    <string name="about_dev_email">{label}</string>\n')
            inserted = True
    if not inserted:
        raise SystemExit(f"ANCHOR NOT FOUND in {path}")
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"{folder}: ok")
