package com.starman.common


object Enums {

  object DayOfWeek extends Enumeration {
    type DayOfWeek = Value
    val Sunday = Value(0, "Sunday")
    val Monday = Value(1, "Monday")
    val Tuesday = Value(2, "Tuesday")
    val Wednesday = Value(3, "Wednesday")
    val Thursday = Value(4, "Thursday")
    val Friday = Value(5, "Friday")
    val Saturday = Value(6, "Saturday")

  }
}
