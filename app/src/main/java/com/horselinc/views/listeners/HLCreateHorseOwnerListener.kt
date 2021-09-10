package com.horselinc.views.listeners

import com.horselinc.models.data.HLHorseManagerModel

interface HLCreateHorseOwnerListener {
    fun onCreatedNewHorseOwner (horseManager: HLHorseManagerModel) {}
}