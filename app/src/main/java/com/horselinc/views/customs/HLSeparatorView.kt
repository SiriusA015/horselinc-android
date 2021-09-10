package com.horselinc.views.customs

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.horselinc.R


/** Created by jcooperation0137 on 2019-08-31.
 */
class HLSeparatorView(context: Context): LinearLayout(context) {
    var containerView = LayoutInflater.from(context).inflate(R.layout.layout_seperator, this)
}