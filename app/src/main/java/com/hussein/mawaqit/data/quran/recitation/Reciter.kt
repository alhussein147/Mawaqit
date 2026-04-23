package com.hussein.mawaqit.data.quran.recitation


enum class Reciter(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val englishSmallName: String,
) {
    ABU_BAKR(2, "أبو بكر الشاطري", "Abu Bakr Al Shatri", "shatri"),
    NASSER(3, "ناصر القطامي", "Nasser Al Qatami", "qtm"),
    YASSER(4, "ياسر الدوسري", "Yasser Al Dosari", "yasser"),

}

enum class FullSurahReciter(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val url: String,
) {
    Husr(
        id = 1,
        nameArabic = "محمود خليل الحصري",
        nameEnglish = "Mamhoud Khalel alHusry",
        url = "https://server13.mp3quran.net/husr/Almusshaf-Al-Mojawwad"
    ),Basit(
        id =2,
        nameArabic = "عبدالباسط عبد الصمد",
        nameEnglish = "Abdul Basit Abdul Samad",
        url = "https://server7.mp3quran.net/basit"
    )
}