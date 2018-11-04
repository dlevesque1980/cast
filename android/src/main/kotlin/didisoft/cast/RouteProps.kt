package didisoft.cast

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RouteProps(
        val connectionState: Int,
        val id: String
)
