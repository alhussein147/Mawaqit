package com.hussein.mawaqit.presentation.radio

// TODO: hoist to a remote api, these doesn't need to be hardcoded and they might change
// migrate to local db
enum class RadioChannel(
    val id: Int,
    val displayName: String,
    val streamUrl: String,
    val imageUrl: String
) {
    AbuBakrAlShatri(
        id = 1,
        displayName = "اذاعة ابو بكر الشاطري",
        streamUrl = "https://backup.qurango.net/radio/shaik_abu_bakr_al_shatri",
        imageUrl = "https://i1.sndcdn.com/artworks-000663801097-wb0y31-t200x200.jpg"
    ),
    AhmadKhaderAlTarabulsi(
        id = 2,
        displayName = "اذاعة احمد خضر الطرابلسي",
        streamUrl = "https://backup.qurango.net/radio/ahmad_khader_altarabulsi",
        imageUrl = "https://i.pinimg.com/564x/d3/c2/9c/d3c29cc03198c3c15d380af048b2d68b.jpg"
    ),
    IbrahimAlAkhdar(
        id = 3,
        displayName = "اذاعة ابراهيم الاخضر",
        streamUrl = "https://backup.qurango.net/radio/ibrahim_alakdar",
        imageUrl = "https://static.suratmp3.com/pics/reciters/thumbs/44_600_600.jpg"
    ),
    KhalidAlJileel(
        id = 4,
        displayName = "اذاعة خالد الجليل",
        streamUrl = "https://backup.qurango.net/radio/khalid_aljileel",
        imageUrl = "https://i1.sndcdn.com/avatars-ubX3f7yLm5eGyphJ-A4ysyA-t500x500.jpg"
    ),
    SalahAlHashim(
        id = 5,
        displayName = "اذاعة صلاح الهاشم",
        streamUrl = "https://backup.qurango.net/radio/salah_alhashim",
        imageUrl = "https://i.pinimg.com/564x/e9/22/1b/e9221b5ffd484937dc70c3eabe350c6f.jpg"
    ),
    SalahBukhatir(
        id = 6,
        displayName = "اذاعة صلاح بو خاطر",
        streamUrl = "https://backup.qurango.net/radio/slaah_bukhatir",
        imageUrl = "https://pbs.twimg.com/profile_images/1306502829251624960/uHKIJQpq_200x200.jpg"
    ),
    AbdulbasitAbdulsamad(
        id = 7,
        displayName = "اذاعة عبدالباسط عبدالصمد",
        streamUrl = "https://backup.qurango.net/radio/abdulbasit_abdulsamad_mojawwad",
        imageUrl = "https://cdns-images.dzcdn.net/images/talk/06b711ac6da4cde0eb698e244f5e27b8/300x300.jpg"
    ),
    AbdulAzizSuhaim(
        id = 8,
        displayName = "اذاعة عبد العزيز سحيم",
        streamUrl = "https://backup.qurango.net/radio/a_sheim",
        imageUrl = "https://i.pinimg.com/564x/a7/37/47/a73747375897de4897da372a0fd921a0.jpg"
    ),
    FaresAbbad(
        id = 9,
        displayName = "اذاعة فارس عباد",
        streamUrl = "https://backup.qurango.net/radio/fares_abbad",
        imageUrl = "https://static.suratmp3.com/pics/reciters/thumbs/15_600_600.jpg"
    ),
    MaherAlMuaiqly(
        id = 10,
        displayName = "اذاعة ماهر المعيقلي",
        streamUrl = "https://backup.qurango.net/radio/maher",
        imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts113/v4/4b/80/58/4b80582d-78ca-a466-0341-0869bc611745/mza_5280524847349008894.jpg/250x250bb.jpg"
    ),
    MohammedSiddiqAlMinshawi(
        id = 11,
        displayName = "اذاعة محمد صديق المنشاوي",
        streamUrl = "https://backup.qurango.net/radio/mohammed_siddiq_alminshawi_mojawwad",
        imageUrl = "https://i1.sndcdn.com/artworks-000284633237-7gdg9t-t200x200.jpg"
    ),
    MahmoudKhalilAlHussary(
        id = 12,
        displayName = "اذاعة محمود خليل الحصري",
        streamUrl = "https://backup.qurango.net/radio/mahmoud_khalil_alhussary_mojawwad",
        imageUrl = "https://watanimg.elwatannews.com/image_archive/original_lower_quality/18194265071637693809.jpg"
    ),
    MahmoudAliAlBanna(
        id = 13,
        displayName = "اذاعة محمود علي البنا",
        streamUrl = "https://backup.qurango.net/radio/mahmoud_ali__albanna_mojawwad",
        imageUrl = "https://i.pinimg.com/200x/29/67/b3/2967b3fbc1ce1f5a70874288d34317bf.jpg"
    ),
    MisharyAlAfasi(
        id = 14,
        displayName = "اذاعة مشاري العفاسي",
        streamUrl = "https://backup.qurango.net/radio/mishary_alafasi",
        imageUrl = "https://i1.sndcdn.com/artworks-000019055020-yr9cjc-t200x200.jpg"
    ),
    NasserAlQatami(
        id = 15,
        displayName = "اذاعة ناصر القطامي",
        streamUrl = "https://backup.qurango.net/radio/nasser_alqatami",
        imageUrl = "https://i1.sndcdn.com/artworks-000096282703-s9wldh-t200x200.jpg"
    ),
    NabilAlRifay(
        id = 16,
        displayName = "اذاعة نبيل الرفاعي",
        streamUrl = "https://backup.qurango.net/radio/nabil_al_rifay",
        imageUrl = "https://i1.sndcdn.com/artworks-000161140408-wh6nhw-t200x200.jpg"
    ),
    HaithamAlJadani(
        id = 17,
        displayName = "اذاعة هيثم الجدعاني",
        streamUrl = "https://backup.qurango.net/radio/hitham_aljadani",
        imageUrl = "https://ar.islamway.net/uploads/authors/3948.jpg"
    ),
    YasserAlDosari(
        id = 18,
        displayName = "اذاعة ياسر الدوسري",
        streamUrl = "https://backup.qurango.net/radio/yasser_aldosari",
        imageUrl = "https://www.almowaten.net/wp-content/uploads/2022/06/%D9%8A%D8%A7%D8%B3%D8%B1-%D8%A7%D9%84%D8%AF%D9%88%D8%B3%D8%B1%D9%8A.jpg"
    ),
    QuranRadioCairo(
        id = 19,
        displayName = "اذاعة القران الكريم من القاهرة",
        streamUrl = "https://n0e.radiojar.com/8s5u5tpdtwzuv?rj-ttl=5&rj-tok=AAABjW7yROAA0TUU8cXhXIAi6g",
        imageUrl = "https://apkdownmod.com/thumbnail?src=images/appsicon/2020/08/app-image-5f42ba68a61b1.jpg"
    ),
    SunnahRadio(
        id = 20,
        displayName = "اذاعة السنة النبوية",
        streamUrl = "https://n01.radiojar.com/x0vs2vzy6k0uv?rj-ttl=5&rj-tok=AAABjW751GcA4NgCI8-5DCpCHQ",
        imageUrl = "https://i.pinimg.com/564x/55/16/ab/5516abd3744c3d0b0a7b28bedd5474c0.jpg"
    ),
    KhashiaRecitations(
        id = 21,
        displayName = "اذاعة تلاوات خاشعة",
        streamUrl = "https://backup.qurango.net/radio/salma",
        imageUrl = "https://pbs.twimg.com/profile_images/1396812808659079169/5ft2haLD_400x400.jpg"
    ),
    Ruqyah(
        id = 22,
        displayName = "اذاعة الرقية الشرعية",
        streamUrl = "https://backup.qurango.net/radio/roqiah",
        imageUrl = "https://i1.sndcdn.com/artworks-zygACgAd2NKwuohE-UF2Piw-t500x500.jpg"
    ),
    EidTakbeerat(
        id = 23,
        displayName = "اذاعة تكبيرات العيد",
        streamUrl = "https://backup.qurango.net/radio/eid",
        imageUrl = "https://i.pinimg.com/736x/3c/b3/fc/3cb3fc494b9f8332a7b7b3256e3d9822.jpg"
    ),
    MukhtasarTafsir(
        id = 24,
        displayName = "المختصر في تفسير القرآن الكريم",
        streamUrl = "https://backup.qurango.net/radio/mukhtasartafsir",
        imageUrl = "https://areejquran.net/wp-content/uploads/2015/12/unnamed.jpg"
    );
}
