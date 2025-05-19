package ru.hse.miem.cryptotrendreader

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.yandex.div.DivDataTag
import com.yandex.div.core.Div2Context
import com.yandex.div.core.DivActionHandler
import com.yandex.div.core.DivConfiguration
import com.yandex.div.core.DivViewFacade
import com.yandex.div.core.expression.variables.DivVariableController
import com.yandex.div.core.view2.Div2View
import com.yandex.div.data.DivParsingEnvironment
import com.yandex.div.json.ParsingErrorLogger
import com.yandex.div.json.expressions.ExpressionResolver
import com.yandex.div2.DivAction
import com.yandex.div2.DivData
import org.json.JSONObject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.hse.miem.cryptotrendreader.core.CoilDivImageLoader
import ru.hse.miem.cryptotrendreader.core.DivAssetReader
import ru.hse.miem.cryptotrendreader.ui.CryptoViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var divContext: Div2Context
    private val variableController = DivVariableController()
    private val cryptoViewModel: CryptoViewModel by viewModel()
    private var div2View: Div2View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val handler = object : DivActionHandler() {
            override fun handleAction(action: DivAction, view: DivViewFacade, resolver: ExpressionResolver): Boolean {
                val id = action.logId.evaluate(resolver)
                if (id == "load_instrument_button") {
                    val input = variableController.get("instrument_input")?.getValue()?.toString()
                    if (input.isNullOrBlank()) cryptoViewModel.startObservation() else cryptoViewModel.updateInstrument(input)
                    return true
                }
                return super.handleAction(action, view, resolver)
            }
        }

        val config = DivConfiguration.Builder(CoilDivImageLoader(this))
            .divVariableController(variableController)
            .actionHandler(handler)
            .build()

        divContext = Div2Context(
            baseContext = this,
            configuration = config,
            lifecycleOwner = this
        )

        cryptoViewModel.setupVariables(variableController)
        renderDiv("templates/main_screen")
        cryptoViewModel.startObservation()
    }

    private fun renderDiv(path: String) {
        val container = findViewById<LinearLayout>(R.id.container)
        container.removeAllViews()
        val json = DivAssetReader(divContext).read("$path.json")
        val templates = json.optJSONObject("templates")
        val card = json.getJSONObject("card")
        div2View = DivViewFactory(divContext, templates).createView(card)
        container.addView(div2View)
        div2View?.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    }
}

class DivViewFactory(
    private val context: Div2Context,
    private val templatesJson: JSONObject?
) {
    private val env = DivParsingEnvironment(ParsingErrorLogger.LOG).apply {
        templatesJson?.let(::parseTemplates)
    }

    fun createView(card: JSONObject): Div2View {
        val data = DivData(env, card)
        return Div2View(context).apply { setData(data, DivDataTag(data.logId)) }
    }
}
