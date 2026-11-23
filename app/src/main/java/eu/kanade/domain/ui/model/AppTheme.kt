package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class AppTheme(val titleRes: StringResource?) {
    // Hidden alias of Totoro, kept so a stored DEFAULT (and the old Raito palette) still resolves
    DEFAULT(null),
    MONET(MR.strings.theme_monet),
    DS_AMBER(MR.strings.theme_ds_amber),
    DS_ASUKA(MR.strings.theme_ds_asuka),
    DS_ITSUKA(MR.strings.theme_ds_itsuka),
    DS_KANADE(MR.strings.theme_ds_kanade),
    DS_LIME(MR.strings.theme_ds_lime),
    DS_MAMIMI(MR.strings.theme_ds_mamimi),
    DS_MIKU(MR.strings.theme_ds_miku),
    DS_MION(MR.strings.theme_ds_mion),
    DS_RIKKA(MR.strings.theme_ds_rikka),
    DS_SAKURA(MR.strings.theme_ds_sakura),
    DS_SHANA(MR.strings.theme_ds_shana),
    DS_SKY(MR.strings.theme_ds_sky),
    DS_TOTORO(MR.strings.theme_ds_totoro),

    // Deprecated
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),
}
