package org.ole.planet.myplanet.ui.dashboard

import android.os.Bundle
import android.text.Html
import kotlinx.android.synthetic.main.activity_disclaimer.tv_disclaimer
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.base.BaseActivity
import org.ole.planet.myplanet.utilities.Constants

class DisclaimerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disclaimer)
        initActionBar()
        tv_disclaimer.text = Html.fromHtml(Constants.DISCLAIMER)
    }
}
