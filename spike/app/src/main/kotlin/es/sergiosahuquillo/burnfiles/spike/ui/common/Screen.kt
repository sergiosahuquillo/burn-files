package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.log.TestKind

/** Screens reachable from Home (contracts/ui-navigation.md). */
enum class Screen(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val test: TestKind?
) {
    ACCESS(R.string.screen_access, R.string.screen_access_desc, R.drawable.ic_access, TestKind.ACCESS),
    BENCHMARK(R.string.screen_benchmark, R.string.screen_benchmark_desc, R.drawable.ic_benchmark, TestKind.BENCHMARK),
    UPDATE(R.string.screen_update, R.string.screen_update_desc, R.drawable.ic_update, TestKind.UPDATE),
    USB(R.string.screen_usb, R.string.screen_usb_desc, R.drawable.ic_usb, TestKind.USB),
    RESULTS(R.string.screen_results, R.string.screen_results_desc, R.drawable.ic_results, null)
    ;

    companion object {
        /** TV initial focus: first screen without results; Access if every test has some. */
        fun initialFor(tested: Set<TestKind>): Screen =
            entries.firstOrNull { it.test != null && it.test !in tested } ?: ACCESS
    }
}
