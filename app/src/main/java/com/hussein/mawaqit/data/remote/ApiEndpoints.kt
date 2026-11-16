package com.hussein.mawaqit.data.remote

object ApiEndpoints {


    object Assets {
        const val AUDIO_SOURCES_URL =
            "https://huggingface.co/datasets/alhussein147/mawaqit/raw/main/audio_sources.json"
        const val INVOCATIONS_URL = "https://huggingface.co/datasets/alhussein147/mawaqit/raw/main/invocations.json"
        const val QURAN_URL = "https://huggingface.co/datasets/alhussein147/mawaqit/raw/main/quran.json"
        const val DATABASE_URL = ""
        const val TAFSIR_SOURCES_URL = "https://huggingface.co/datasets/alhussein147/mawaqit/raw/main/tafsir_sources.json"
        const val MUKHTASAR_TAFSIR_URL = "https://huggingface.co/datasets/alhussein147/mawaqit/raw/main/mukhtasar.json"

        object Fonts {
            const val REGULAR_FONT_URL = "https://huggingface.co/datasets/alhussein147/mawaqit/resolve/main/regular.ttf"
            const val BOLD_FONT_URL = "https://huggingface.co/datasets/alhussein147/mawaqit/resolve/main/bold.ttf"
        }
    }

    object External {
        const val IP_API_URL = "http://ip-api.com/json/"
    }
}
