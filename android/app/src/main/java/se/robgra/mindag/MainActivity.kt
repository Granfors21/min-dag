package se.robgra.mindag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = AppState(this, { System.currentTimeMillis() })
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (state.route) {
                    Route.CHILD, Route.WELCOME -> moveTaskToBack(true)
                    Route.ROUTINE_EDIT -> state.route = Route.PARENT
                    Route.CHECKS -> state.route = if (state.settings.onboarded) Route.PARENT else Route.CHECKS
                    Route.PIN_SETUP -> state.route = if (state.settings.pin.isEmpty()) Route.WELCOME else if (state.recovering) Route.PIN_SETUP else Route.PARENT
                    Route.RECOVERY_SHOW -> {}
                    Route.RECOVERY_ENTER -> state.route = Route.PIN
                    else -> state.route = Route.CHILD
                }
            }
        })
        setContent { MinDagApp(state) }
    }

    override fun onResume() {
        super.onResume()
        state.reload()
        // Föräldraläget stängs när appen lämnas, så barnet inte hittar det öppet.
        if (state.route in setOf(Route.PARENT, Route.ROUTINE_EDIT, Route.PIN) && state.settings.onboarded) state.route = Route.CHILD
        Alarms.syncAll(this)
    }
}
