package de.igbdsandzakkassel.vaktija.shared

data class Dhikr(
    val arabic: String,
    val transliteration: String,
    /** Meaning per app-language tag (always includes "en" as a fallback). */
    val meaning: Map<String, String>,
)

/** Common dhikr. Arabic + transliteration are standard; meanings native-reviewed via a workflow. */
object DhikrData {
    val ITEMS: List<Dhikr> = listOf(
        Dhikr(
            "سُبْحَانَ اللَّهِ", "Subhan Allah",
            mapOf(
                "en" to "Glory be to Allah", "bs" to "Slava Allahu", "de" to "Gepriesen sei Allah",
                "tr" to "Allah'ı her türlü eksiklikten tenzih ederim",
                "sq" to "I lartësuar qoftë Allahu (i pastër nga çdo e metë)",
                "ur" to "اللہ پاک ہے", "ru" to "Пречист Аллах",
            ),
        ),
        Dhikr(
            "الْحَمْدُ لِلَّهِ", "Alhamdulillah",
            mapOf(
                "en" to "All praise is due to Allah", "bs" to "Hvala Allahu",
                "de" to "Alles Lob gebührt Allah", "tr" to "Hamd, Allah'a mahsustur",
                "sq" to "Falënderimi i takon Allahut", "ur" to "تمام تعریفیں اللہ ہی کے لیے ہیں",
                "ru" to "Хвала Аллаху",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illa Allah",
            mapOf(
                "en" to "There is no god but Allah", "bs" to "Nema boga osim Allaha",
                "de" to "Es gibt keinen Gott außer Allah", "tr" to "Allah'tan başka ilah yoktur",
                "sq" to "Nuk ka zot tjetër që meriton të adhurohet përveç Allahut",
                "ur" to "اللہ کے سوا کوئی معبود نہیں",
                "ru" to "Нет божества, достойного поклонения, кроме Аллаха",
            ),
        ),
        Dhikr(
            "اللَّهُ أَكْبَرُ", "Allahu Akbar",
            mapOf(
                "en" to "Allah is the Greatest", "bs" to "Allah je najveći",
                "de" to "Allah ist der Größte", "tr" to "Allah en büyüktür",
                "sq" to "Allahu është më i Madhi", "ur" to "اللہ سب سے بڑا ہے",
                "ru" to "Аллах Велик",
            ),
        ),
        Dhikr(
            "أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah",
            mapOf(
                "en" to "I seek forgiveness from Allah", "bs" to "Tražim oprost od Allaha",
                "de" to "Ich bitte Allah um Vergebung", "tr" to "Allah'tan bağışlanma dilerim",
                "sq" to "Kërkoj falje nga Allahu", "ur" to "میں اللہ سے بخشش مانگتا ہوں",
                "ru" to "Прошу прощения у Аллаха",
            ),
        ),
        Dhikr(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Subhan Allahi wa bihamdihi",
            mapOf(
                "en" to "Glory and praise be to Allah", "bs" to "Slava Allahu i Njemu hvala",
                "de" to "Gepriesen sei Allah und alles Lob gebührt Ihm",
                "tr" to "Allah'ı hamd ile tesbih ederim",
                "sq" to "I lartësuar qoftë Allahu dhe Atij i takon falënderimi",
                "ur" to "اللہ پاک ہے اور اُسی کی حمد کے ساتھ", "ru" to "Пречист Аллах и хвала Ему",
            ),
        ),
        Dhikr(
            "سُبْحَانَ اللَّهِ الْعَظِيمِ", "Subhan Allah al-Azim",
            mapOf(
                "en" to "Glory be to Allah, the Most Great", "bs" to "Slava Allahu Veličanstvenom",
                "de" to "Gepriesen sei Allah, der Allmächtige",
                "tr" to "Yüce Allah'ı her türlü eksiklikten tenzih ederim",
                "sq" to "I lartësuar qoftë Allahu i Madhërishëm", "ur" to "اللہ پاک ہے، جو سب سے عظیم ہے",
                "ru" to "Пречист Аллах Великий",
            ),
        ),
        Dhikr(
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "La hawla wa la quwwata illa billah",
            mapOf(
                "en" to "There is no might nor power except with Allah",
                "bs" to "Nema snage ni moći osim u Allaha",
                "de" to "Es gibt keine Macht und keine Kraft außer bei Allah",
                "tr" to "Güç ve kuvvet ancak Allah'a aittir",
                "sq" to "Nuk ka forcë e as fuqi përveçse me ndihmën e Allahut",
                "ur" to "اللہ کی مدد کے سوا نہ کوئی طاقت ہے اور نہ قوت",
                "ru" to "Нет силы и мощи ни у кого, кроме Аллаха",
            ),
        ),
        Dhikr(
            "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "Hasbunallahu wa ni'mal-wakil",
            mapOf(
                "en" to "Allah is sufficient for us, and He is the best disposer of affairs",
                "bs" to "Dovoljan nam je Allah i divan li je On Zaštitnik",
                "de" to "Allah genügt uns, und Er ist der beste Sachwalter",
                "tr" to "Allah bize yeter, O ne güzel vekildir",
                "sq" to "Na mjafton Allahu dhe sa Mbrojtës i mrekullueshëm është Ai",
                "ur" to "ہمارے لیے اللہ کافی ہے اور وہی بہترین کارساز ہے",
                "ru" to "Достаточно нам Аллаха, и Он — наилучший Покровитель",
            ),
        ),
        Dhikr(
            "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Allahumma salli ala Muhammad",
            mapOf(
                "en" to "O Allah, send blessings upon Muhammad", "bs" to "Allahu moj, blagoslovi Muhammeda",
                "de" to "O Allah, segne Muhammad", "tr" to "Allah'ım, Muhammed'e salât eyle",
                "sq" to "O Allah, dërgo bekime mbi Muhamedin", "ur" to "اے اللہ! محمد صلی اللہ علیہ وسلم پر درود بھیج",
                "ru" to "О Аллах, благослови Мухаммада",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "La ilaha illa Allahu wahdahu la sharika lah",
            mapOf(
                "en" to "There is no god but Allah, alone, without partner",
                "bs" to "Nema boga osim Allaha, Jedinog, koji nema saučesnika",
                "de" to "Es gibt keinen Gott außer Allah, Er allein, ohne Teilhaber",
                "tr" to "Allah'tan başka ilah yoktur; O tektir, ortağı yoktur",
                "sq" to "Nuk ka zot tjetër që meriton të adhurohet përveç Allahut, Një e të Vetëm, që nuk ka ortak",
                "ur" to "اللہ کے سوا کوئی معبود نہیں، وہ اکیلا ہے، اُس کا کوئی شریک نہیں",
                "ru" to "Нет божества, достойного поклонения, кроме одного лишь Аллаха, у Которого нет сотоварища",
            ),
        ),
        Dhikr(
            "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "Allahumma anta Rabbi la ilaha illa anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya wa abu'u bidhanbi faghfir li fa innahu la yaghfirudh-dhunuba illa anta",
            mapOf(
                "en" to "O Allah, You are my Lord, there is no god but You. You created me and I am Your servant; I keep Your covenant and promise as best I can. I seek refuge in You from the evil I have done. I acknowledge Your favour upon me and I acknowledge my sin, so forgive me, for none forgives sins except You.",
                "bs" to "Allahu moj, Ti si Gospodar moj, nema boga osim Tebe. Ti si me stvorio i ja sam Tvoj rob. Držim se Tvoga zavjeta i obećanja koliko god mogu. Utječem Ti se od zla koje sam počinio. Priznajem Ti blagodati kojima si me obasuo i priznajem grijeh svoj, pa mi oprosti, jer grijehe ne oprašta niko osim Tebe.",
                "de" to "O Allah, Du bist mein Herr. Es gibt keinen Gott außer Dir. Du hast mich erschaffen, und ich bin Dein Diener. Ich halte meinen Bund und mein Versprechen Dir gegenüber ein, so gut ich kann. Ich suche Zuflucht bei Dir vor dem Übel, das ich getan habe. Ich erkenne Deine Gnade an, die Du mir erwiesen hast, und ich bekenne meine Sünde. So vergib mir, denn wahrlich, niemand vergibt die Sünden außer Dir.",
                "tr" to "Allah'ım! Sen benim Rabbimsin, Sen'den başka ibadete layık hiçbir ilah yoktur. Beni Sen yarattın ve ben Sen'in kulunum. Gücüm yettiğince Sana verdiğim ahd ve va'd üzereyim. İşlediğim kötülüklerin şerrinden Sana sığınırım. Üzerimdeki nimetini ikrar eder, günahımı da itiraf ederim. Beni bağışla; çünkü günahları Sen'den başka bağışlayacak hiç kimse yoktur.",
                "sq" to "O Allah, Ti je Zoti im, nuk ka zot tjetër që meriton adhurimin përveç Teje. Ti më krijove dhe unë jam robi Yt; e mbaj besën dhe premtimin Tënd për aq sa kam mundësi. Kërkoj mbrojtjen Tënde nga e keqja që kam bërë. Pranoj e dëshmoj mirësitë e Tua ndaj meje dhe pranoj mëkatin tim, andaj më fal, sepse askush nuk i fal mëkatet përveç Teje.",
                "ur" to "اے اللہ! تو ہی میرا رب ہے، تیرے سوا کوئی معبود نہیں۔ تو نے ہی مجھے پیدا کیا اور میں تیرا بندہ ہوں۔ میں اپنی استطاعت کے مطابق تیرے عہد اور تیرے وعدے پر قائم ہوں۔ میں تیرے کیے ہوئے برے اعمال کے شر سے تیری پناہ مانگتا ہوں۔ میں تیری اُن نعمتوں کا اقرار کرتا ہوں جو تو نے مجھ پر کیں، اور اپنے گناہ کا بھی اقرار کرتا ہوں، پس تو مجھے بخش دے، کیونکہ تیرے سوا گناہوں کو کوئی نہیں بخش سکتا۔",
                "ru" to "О Аллах, Ты — мой Господь, нет божества, достойного поклонения, кроме Тебя. Ты сотворил меня, и я — Твой раб. Я храню верность завету с Тобой и обещанию, данному Тебе, насколько мне это под силу. Прибегаю к Тебе от зла того, что я совершил. Признаю милость Твою ко мне и признаю свой грех. Прости же меня, ибо, поистине, никто не прощает грехи, кроме Тебя.",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            "La ilaha illa Allahu wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa huwa 'ala kulli shay'in qadir",
            mapOf(
                "en" to "There is no god but Allah, alone, without partner. His is the dominion and His is the praise, and He has power over all things.",
                "bs" to "Nema boga osim Allaha, Jedinoga, koji nema sudruga. Njemu pripada sva vlast i Njemu pripada sva hvala, i On sve može.",
                "de" to "Es gibt keinen Gott außer Allah, Er allein, ohne Teilhaber. Sein ist die Herrschaft und Sein ist das Lob, und Er hat Macht über alle Dinge.",
                "tr" to "Allah'tan başka ibadete layık hiçbir ilah yoktur; O tektir, O'nun hiçbir ortağı yoktur. Mülk yalnız O'nundur, hamd yalnız O'na mahsustur ve O'nun her şeye gücü yeter.",
                "sq" to "Nuk ka zot tjetër që meriton adhurimin përveç Allahut, Një e të Vetëm, që nuk ka shok; Atij i takon sundimi dhe Atij i takon lavdërimi, dhe Ai është i Plotfuqishëm mbi çdo gjë.",
                "ur" to "اللہ کے سوا کوئی معبود نہیں، وہ اکیلا ہے، اُس کا کوئی شریک نہیں۔ اُسی کی بادشاہت ہے اور اُسی کے لیے ہر تعریف ہے، اور وہ ہر چیز پر قادر ہے۔",
                "ru" to "Нет божества, достойного поклонения, кроме одного лишь Аллаха, у Которого нет сотоварища. Ему принадлежит власть, и Ему хвала, и Он над всякой вещью властен.",
            ),
        ),
        Dhikr(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
            "Subhanallahi wa bihamdihi, 'adada khalqihi, wa rida nafsihi, wa zinata 'arshihi, wa midada kalimatihi",
            mapOf(
                "en" to "Glory and praise be to Allah, as much as the number of His creation, as much as pleases Him, as much as the weight of His Throne, and as much as the ink of His words.",
                "bs" to "Slava i hvala Allahu, koliko je stvorenja Njegovih, koliko je Njemu po volji, koliko je težak Arš Njegov i koliko je mastila za riječi Njegove.",
                "de" to "Gepriesen sei Allah und Ihm gebührt das Lob — so viel wie die Zahl Seiner Geschöpfe, so viel wie es Seinem Wohlgefallen entspricht, so viel wie das Gewicht Seines Thrones und so viel wie die Tinte Seiner Worte.",
                "tr" to "Yarattıklarının sayısınca, kendi razı olduğunca, Arş'ının ağırlığınca ve kelimelerinin mürekkebi miktarınca Allah'ı hamd ile tesbih ederim.",
                "sq" to "I lartësuar qoftë Allahu dhe Atij i qoftë lavdërimi: sa numri i krijesave të Tij, sa kënaqësia e Tij, sa rëndesa e Arshit të Tij dhe sa boja e fjalëve të Tij.",
                "ur" to "اللہ پاک ہے اور میں اُس کی حمد بیان کرتا ہوں، اُس کی مخلوق کی تعداد کے برابر، اُس کی ذات کی رضا کے برابر، اُس کے عرش کے وزن کے برابر، اور اُس کے کلمات کی سیاہی کے برابر۔",
                "ru" to "Преславен Аллах и хвала Ему — числом, равным числу Его творений, и настолько, насколько это угодно Ему, и равным весу Его Трона, и равным количеству чернил, которыми записаны Его слова.",
            ),
        ),
        Dhikr(
            "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
            "Astaghfirullahal-'Azim alladhi la ilaha illa huwal-Hayyul-Qayyum wa atubu ilayh",
            mapOf(
                "en" to "I seek forgiveness from Allah the Most Great, besides whom there is no god, the Ever-Living, the Sustainer, and I turn to Him in repentance.",
                "bs" to "Molim oprost od Allaha Veličanstvenog, osim kojeg drugog boga nema, Živog i Vječnog, i Njemu se kajem.",
                "de" to "Ich bitte Allah, den Gewaltigen, um Vergebung — Ihn, außer dem es keinen Gott gibt, den Lebendigen, den Beständigen — und ich kehre reuevoll zu Ihm zurück.",
                "tr" to "Kendisinden başka ibadete layık hiçbir ilah bulunmayan, diri (Hayy) ve her şeyi ayakta tutan (Kayyum) Yüce Allah'tan bağışlanma diler ve O'na tövbe ederim.",
                "sq" to "Kërkoj falje nga Allahu i Madhërishëm, përveç të Cilit nuk ka zot tjetër, i Përjetshmi, Mbajtësi i çdo gjëje, dhe tek Ai kthehem me pendim.",
                "ur" to "میں اللہ بزرگ و برتر سے بخشش مانگتا ہوں، جس کے سوا کوئی معبود نہیں، جو ہمیشہ زندہ رہنے والا اور سب کو سنبھالنے والا ہے، اور میں اُسی کی طرف توبہ کرتا ہوں۔",
                "ru" to "Прошу прощения у Аллаха Великого, кроме Которого нет божества, достойного поклонения, Живого, Вседержителя, и приношу Ему своё покаяние.",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            "La ilaha illa anta subhanaka inni kuntu minaz-zalimin",
            mapOf(
                "en" to "There is no god but You; glory be to You! Indeed, I was among the wrongdoers.",
                "bs" to "Nema boga osim Tebe, slava Tebi! Zaista sam ja bio od onih koji su sami sebi nepravdu učinili.",
                "de" to "Es gibt keinen Gott außer Dir. Gepriesen seist Du! Wahrlich, ich gehörte zu den Ungerechten.",
                "tr" to "Sen'den başka ibadete layık hiçbir ilah yoktur. Sen'i tüm noksanlıklardan tenzih ederim. Şüphesiz ben zalimlerden oldum.",
                "sq" to "Nuk ka zot tjetër përveç Teje; i lartësuar qofsh Ti! Vërtet, unë isha prej atyre që i bënë padrejtësi vetes.",
                "ur" to "تیرے سوا کوئی معبود نہیں، تو پاک ہے، بے شک میں ہی ظالموں میں سے تھا۔",
                "ru" to "Нет божества, достойного поклонения, кроме Тебя! Преславен Ты! Поистине, я был одним из несправедливых.",
            ),
        ),
        Dhikr(
            "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar",
            mapOf(
                "en" to "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
                "bs" to "Gospodaru naš, podari nam dobro na ovome svijetu i dobro na onome svijetu, i sačuvaj nas od patnje u Vatri.",
                "de" to "Unser Herr, gib uns im Diesseits Gutes und im Jenseits Gutes, und bewahre uns vor der Strafe des Feuers.",
                "tr" to "Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi cehennem azabından koru.",
                "sq" to "Zoti ynë, na jep të mira në këtë botë dhe të mira në botën tjetër, dhe na ruaj nga dënimi i zjarrit.",
                "ur" to "اے ہمارے رب! ہمیں دنیا میں بھی بھلائی عطا فرما اور آخرت میں بھی بھلائی عطا فرما، اور ہمیں آگ کے عذاب سے بچا۔",
                "ru" to "Господь наш! Даруй нам благо в этом мире и благо в мире будущем и защити нас от мучений Огня.",
            ),
        ),
        Dhikr(
            "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            "Hasbiyallahu la ilaha illa huwa, 'alayhi tawakkaltu wa huwa Rabbul-'arshil-'azim",
            mapOf(
                "en" to "Allah is sufficient for me; there is no god but Him. In Him I put my trust, and He is the Lord of the Mighty Throne.",
                "bs" to "Dovoljan mi je Allah, nema boga osim Njega; na Njega se oslanjam, a On je Gospodar Arša veličanstvenog.",
                "de" to "Allah genügt mir. Es gibt keinen Gott außer Ihm. Auf Ihn vertraue ich, und Er ist der Herr des gewaltigen Thrones.",
                "tr" to "Allah bana yeter; O'ndan başka ibadete layık hiçbir ilah yoktur. Ben yalnız O'na güvenip dayandım ve O, büyük Arş'ın Rabbidir.",
                "sq" to "Allahu më mjafton mua; nuk ka zot tjetër përveç Tij. Tek Ai jam mbështetur dhe Ai është Zoti i Arshit të madh.",
                "ur" to "مجھے اللہ ہی کافی ہے، اُس کے سوا کوئی معبود نہیں، اُسی پر میں نے بھروسہ کیا، اور وہی عظیم عرش کا رب ہے۔",
                "ru" to "Достаточно мне Аллаха, нет божества, достойного поклонения, кроме Него. На Него я уповаю, и Он — Господь великого Трона.",
            ),
        ),
        Dhikr(
            "رَضِيتُ بِاللَّهِ رَبًّا وَبِالْإِسْلَامِ دِينًا وَبِمُحَمَّدٍ نَبِيًّا",
            "Raditu billahi Rabban, wa bil-islami dinan, wa bi-Muhammadin nabiyyan",
            mapOf(
                "en" to "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad as my Prophet.",
                "bs" to "Zadovoljan sam Allahom kao Gospodarom, islamom kao vjerom i Muhammedom kao vjerovjesnikom.",
                "de" to "Ich bin zufrieden mit Allah als Herrn, mit dem Islam als Religion und mit Muhammad als Prophet.",
                "tr" to "Rab olarak Allah'tan, din olarak İslam'dan ve peygamber olarak Muhammed'den razı oldum.",
                "sq" to "Jam i kënaqur me Allahun si Zot, me Islamin si fe dhe me Muhamedin si Pejgamber.",
                "ur" to "میں راضی ہوا اللہ کے رب ہونے پر، اسلام کے دین ہونے پر، اور محمد صلی اللہ علیہ وسلم کے نبی ہونے پر۔",
                "ru" to "Я доволен Аллахом как Господом, Исламом — как религией и Мухаммадом — как Пророком.",
            ),
        ),
        Dhikr(
            "اللَّهُمَّ أَعِنِّي عَلَىٰ ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            "Allahumma a'inni 'ala dhikrika wa shukrika wa husni 'ibadatik",
            mapOf(
                "en" to "O Allah, help me to remember You, to thank You, and to worship You well.",
                "bs" to "Allahu moj, pomozi mi da Te spominjem, da Ti zahvaljujem i da Te lijepo obožavam.",
                "de" to "O Allah, hilf mir, Deiner zu gedenken, Dir zu danken und Dich auf schöne Weise anzubeten.",
                "tr" to "Allah'ım! Seni anmak, Sana şükretmek ve Sana en güzel şekilde ibadet etmek hususunda bana yardım eyle.",
                "sq" to "O Allah, më ndihmo që të të përmend Ty, të të falënderoj Ty dhe të të adhuroj Ty në mënyrën më të mirë.",
                "ur" to "اے اللہ! اپنے ذکر، اپنے شکر اور اپنی بہترین عبادت پر میری مدد فرما۔",
                "ru" to "О Аллах, помоги мне поминать Тебя, благодарить Тебя и достойно поклоняться Тебе.",
            ),
        ),
        Dhikr(
            "سُبْحَانَ رَبِّيَ الْعَظِيمِ",
            "Subhana Rabbiyal-'Azim",
            mapOf(
                "en" to "Glory be to my Lord, the Most Great.",
                "bs" to "Slava Gospodaru mome Veličanstvenom.",
                "de" to "Gepriesen sei mein Herr, der Gewaltige.",
                "tr" to "Yüce olan Rabbimi tüm noksanlıklardan tenzih ederim.",
                "sq" to "I lartësuar qoftë Zoti im i Madhërishëm.",
                "ur" to "پاک ہے میرا رب جو بہت عظیم ہے۔",
                "ru" to "Преславен Господь мой Великий.",
            ),
        ),
        Dhikr(
            "سُبْحَانَ رَبِّيَ الْأَعْلَىٰ",
            "Subhana Rabbiyal-A'la",
            mapOf(
                "en" to "Glory be to my Lord, the Most High.",
                "bs" to "Slava Gospodaru mome Uzvišenom.",
                "de" to "Gepriesen sei mein Herr, der Höchste.",
                "tr" to "En yüce olan Rabbimi tüm noksanlıklardan tenzih ederim.",
                "sq" to "I lartësuar qoftë Zoti im më i Larti.",
                "ur" to "پاک ہے میرا رب جو سب سے بلند و برتر ہے۔",
                "ru" to "Преславен Господь мой Всевышний.",
            ),
        ),
        Dhikr(
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "Bismillahir-Rahmanir-Rahim",
            mapOf(
                "en" to "In the name of Allah, the Most Gracious, the Most Merciful.",
                "bs" to "U ime Allaha, Milostivog, Samilosnog.",
                "de" to "Im Namen Allahs, des Allerbarmers, des Barmherzigen.",
                "tr" to "Rahman ve Rahim olan Allah'ın adıyla.",
                "sq" to "Me emrin e Allahut, të Gjithëmëshirshmit, Mëshirëplotit.",
                "ur" to "شروع اللہ کے نام سے جو بے حد مہربان، نہایت رحم والا ہے۔",
                "ru" to "Во имя Аллаха, Милостивого, Милосердного.",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا اللَّهُ مُحَمَّدٌ رَسُولُ اللَّهِ",
            "La ilaha illa Allah, Muhammadun Rasulullah",
            mapOf(
                "en" to "There is no god but Allah; Muhammad is the Messenger of Allah.",
                "bs" to "Nema boga osim Allaha, Muhammed je Allahov poslanik.",
                "de" to "Es gibt keinen Gott außer Allah; Muhammad ist der Gesandte Allahs.",
                "tr" to "Allah'tan başka ibadete layık hiçbir ilah yoktur; Muhammed Allah'ın Resulüdür.",
                "sq" to "Nuk ka zot tjetër që meriton adhurimin përveç Allahut; Muhamedi është i Dërguari i Allahut.",
                "ur" to "اللہ کے سوا کوئی معبود نہیں، محمد صلی اللہ علیہ وسلم اللہ کے رسول ہیں۔",
                "ru" to "Нет божества, достойного поклонения, кроме Аллаха; Мухаммад — посланник Аллаха.",
            ),
        ),
        Dhikr(
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْجَنَّةَ وَأَعُوذُ بِكَ مِنَ النَّارِ",
            "Allahumma inni as'alukal-jannata wa a'udhu bika minan-nar",
            mapOf(
                "en" to "O Allah, I ask You for Paradise and seek refuge in You from the Fire.",
                "bs" to "Allahu moj, molim Te za Džennet i utječem Ti se od Vatre.",
                "de" to "O Allah, ich bitte Dich um das Paradies und suche Zuflucht bei Dir vor dem Feuer.",
                "tr" to "Allah'ım! Senden cenneti ister ve cehennem ateşinden Sana sığınırım.",
                "sq" to "O Allah, unë të kërkoj Ty Xhenetin dhe kërkoj mbrojtjen Tënde nga zjarri.",
                "ur" to "اے اللہ! میں تجھ سے جنت کا سوال کرتا ہوں اور آگ سے تیری پناہ مانگتا ہوں۔",
                "ru" to "О Аллах, поистине, я прошу у Тебя Рай и прибегаю к Тебе от Огня.",
            ),
        ),
    )
}

/** Swift-friendly row: meaning resolved for [lang] (English fallback). */
data class DhikrItem(val arabic: String, val transliteration: String, val meaning: String)

fun dhikrList(lang: String): List<DhikrItem> = DhikrData.ITEMS.map {
    DhikrItem(it.arabic, it.transliteration, it.meaning[lang] ?: it.meaning["en"] ?: "")
}
