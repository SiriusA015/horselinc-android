package com.horselinc.views.listeners

import com.horselinc.models.data.HLHorseModel

interface HLHorseOwnerItemListener {
    fun onChangePercentage (position: Int, percentage: Float) {}
    fun onClickDelete (position: Int) {}
}

interface HLHorseRegistrationItemListener {
    fun onChangeName (position: Int, name: String) {}
    fun onChangeNumber (position: Int, number: String) {}
    fun onClickDelete (position: Int) {}
}

interface HLEditHorseItemListener {
    fun onClickDelete (position: Int, horse: HLHorseModel) {}
}

interface HLProviderHorseItemListener {
    fun onClickHorse (position: Int, horse: HLHorseModel) {}
}