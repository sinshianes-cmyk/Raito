package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import mihon.app.di.appGraph

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_Tachiyomi_Totoro))
                if (isAmoled) add(R.style.ThemeOverlay_Tachiyomi_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = activity.appGraph.uiPreferences
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme.get(), uiPreferences.themeDarkAmoled.get())
            .forEach(activity::setTheme)
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_Tachiyomi_Monet,
    AppTheme.DEFAULT to R.style.Theme_Tachiyomi_Totoro,
    AppTheme.DS_TOTORO to R.style.Theme_Tachiyomi_Totoro,
    AppTheme.DS_ASUKA to R.style.Theme_Tachiyomi_Asuka,
    AppTheme.DS_ITSUKA to R.style.Theme_Tachiyomi_Itsuka,
    AppTheme.DS_KANADE to R.style.Theme_Tachiyomi_Kanade,
    AppTheme.DS_MAMIMI to R.style.Theme_Tachiyomi_Mamimi,
    AppTheme.DS_MIKU to R.style.Theme_Tachiyomi_Miku,
    AppTheme.DS_MION to R.style.Theme_Tachiyomi_Mion,
    AppTheme.DS_RIKKA to R.style.Theme_Tachiyomi_Rikka,
    AppTheme.DS_SAKURA to R.style.Theme_Tachiyomi_Sakura,
    AppTheme.DS_SHANA to R.style.Theme_Tachiyomi_Shana,
    AppTheme.DS_LIME to R.style.Theme_Tachiyomi_Lime,
    AppTheme.DS_AMBER to R.style.Theme_Tachiyomi_Amber,
    AppTheme.DS_SKY to R.style.Theme_Tachiyomi_Sky,
)
