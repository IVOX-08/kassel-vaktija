# One-off: insert onboarding-permissions strings after onb_start in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STR = {
    "onb_perms_title": {
        "values": "Dozvole", "values-de": "Berechtigungen", "values-en": "Permissions",
        "values-ar": "الأذونات", "values-tr": "İzinler", "values-sq": "Lejet",
        "values-ur": "اجازتیں", "values-ru": "Разрешения",
    },
    "onb_perms_body": {
        "values": "Da bi vas aplikacija na vrijeme obavijestila o namazu i vijestima, omogućite sljedeće dozvole.",
        "values-de": "Damit dich die App pünktlich an Gebet und Nachrichten erinnern kann, erlaube bitte die folgenden Berechtigungen.",
        "values-en": "So the app can remind you of prayers and announcements on time, please allow the following.",
        "values-ar": "حتى يتمكن التطبيق من تذكيرك بالصلاة والإشعارات في وقتها، يرجى السماح بالأذونات التالية.",
        "values-tr": "Uygulamanın namaz ve duyuruları zamanında hatırlatabilmesi için lütfen aşağıdaki izinleri verin.",
        "values-sq": "Që aplikacioni t'ju kujtojë namazin dhe njoftimet në kohë, ju lutemi lejoni sa vijon.",
        "values-ur": "تاکہ ایپ آپ کو نماز اور اطلاعات کی بروقت یاد دہانی کرا سکے، براہِ کرم درج ذیل اجازتیں دیں۔",
        "values-ru": "Чтобы приложение вовремя напоминало о намазе и сообщениях, разрешите следующее.",
    },
    "onb_allow": {
        "values": "Dozvoli", "values-de": "Erlauben", "values-en": "Allow",
        "values-ar": "السماح", "values-tr": "İzin ver", "values-sq": "Lejo",
        "values-ur": "اجازت دیں", "values-ru": "Разрешить",
    },
}

ANCHOR = 'name="onb_start"'
locales = ["values", "values-de", "values-en", "values-ar", "values-tr", "values-sq", "values-ur", "values-ru"]

for folder in locales:
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("onb_perms_title" in l for l in lines):
        print(f"{folder}: already present, skipped")
        continue
    out, inserted = [], False
    for line in lines:
        out.append(line)
        if ANCHOR in line and not inserted:
            for name, vals in STR.items():
                out.append(f'    <string name="{name}">{vals[folder]}</string>\n')
            inserted = True
    if not inserted:
        raise SystemExit(f"ANCHOR NOT FOUND in {path}")
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"{folder}: ok")
