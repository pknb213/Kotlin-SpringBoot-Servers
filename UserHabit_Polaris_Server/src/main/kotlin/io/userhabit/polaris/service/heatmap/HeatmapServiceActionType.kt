package io.userhabit.polaris.service.heatmap

import io.userhabit.polaris.EventType



class HeatmapServiceActionType {

    companion object {
        // TODO : to enum ?
        const val TAP = "tap"
        const val DOUBLETAP = "double_tap"
        const val LONGTAP = "long_tap"
        const val SWIPE = "swipe"
        const val RESPONSE = "response"
        const val NORESPONSE = "no_response"
        val eventType = mapOf(
            TAP to listOf(EventType.REACT_TAP),
            DOUBLETAP to listOf(EventType.REACT_DOUBLE_TAP),
            LONGTAP to listOf(EventType.REACT_LONG_TAP),
            SWIPE to listOf(EventType.REACT_SWIPE),
            RESPONSE to listOf(
                EventType.REACT_TAP,
                EventType.REACT_LONG_TAP,
                EventType.REACT_DOUBLE_TAP,
                EventType.REACT_SWIPE
            ),
            NORESPONSE to listOf(
                EventType.NOACT_TAP,
                EventType.NOACT_LONG_TAP,
                EventType.NOACT_DOUBLE_TAP,
                EventType.NOACT_SWIPE
            ),
        )

    }

}
