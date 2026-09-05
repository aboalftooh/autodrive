package com.autodrive.app.core.database.converters

import androidx.room.TypeConverter
import java.math.BigDecimal

/** يخزن Decimal كنص لتجنب تقريب SQLite REAL. */
class BigDecimalConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = value.toBigDecimal()
}
