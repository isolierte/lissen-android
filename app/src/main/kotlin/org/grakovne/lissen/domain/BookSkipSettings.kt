package org.grakovne.lissen.domain

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class BookSkipSettings(
  val enabled: Boolean = true,
  val introSkipSeconds: Int? = null,
  val outroSkipSeconds: Int? = null,
)
