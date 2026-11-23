package eu.kanade.presentation.more.settings.screen.novel

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import raito.domain.novel.NovelPreferences
import raito.novel.content.NovelRegexReplacement
import raito.novel.content.NovelRegexReplacements
import raito.novel.content.NovelRegexRules
import raito.novel.content.NovelStoredToggleList

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class NovelRegexRulesViewModel(
    novelPreferences: NovelPreferences,
) : NovelToggleListViewModel<NovelRegexReplacement>(
    NovelStoredToggleList(novelPreferences.readerRegexReplacements(), NovelRegexRules),
) {

    /**
     * What the rule would do to [input], run through the same kernel the reader uses, so the answer
     * here cannot disagree with what a chapter gets. Null where the pattern will not compile, with
     * the reason, which is what the dialog shows instead of an output.
     */
    fun preview(rule: NovelRegexReplacement, input: String): Result<String> = runCatching {
        NovelRegexReplacements.compile(rule).applyTo(input)
    }
}
