package com.horselinc.models.data

import com.horselinc.HLSortOrder

/**
 *  Filter Model for Horse Search
 */
data class HLHorseFilterModel (
    var trainer: HLHorseManagerModel? = null,
    var owner: HLHorseManagerModel? = null,
    var manager: HLHorseManagerModel? = null,
    var sortFieldName: String? = null,
    var sortOrder: String = HLSortOrder.ASC
) {
    val isNullData: Boolean
        get() {
            return (trainer == null
                    && owner == null
                    && manager == null
                    && sortFieldName == null)
        }
}


/**
 *  Filter Model for Service Request Search
 */
data class HLServiceRequestFilterModel (
    var startDate: Long? = null,
    var endDate: Long? = null,
    var sort: HLSortModel? = null
)


data class HLSortModel (
    var name: String? = null,
    var order: String? = null,
    var index: Int = -1
) {
    companion object {
        val descriptions = arrayListOf(
            "Barn Name (ascending)",
            "Barn Name (descending)",
            "Creation Date (ascending)",
            "Creation Date (descending)"
        )
    }

    var description: String = ""
        set(value) {
            field = value

            val index = descriptions.indexOfFirst { it == description}
            this.index = index
            when (index) {
                0 -> {
                    name = "barnName"
                    order = HLSortOrder.ASC
                }
                1 -> {
                    name = "barnName"
                    order = HLSortOrder.DESC
                }
                2 -> {
                    name = "createdAt"
                    order = HLSortOrder.ASC
                }
                3 -> {
                    name = "createdAt"
                    order = HLSortOrder.DESC
                }
            }
        }

        get() {
            if (index < 0) {
                return ""
            }
            return descriptions[index]
        }
}