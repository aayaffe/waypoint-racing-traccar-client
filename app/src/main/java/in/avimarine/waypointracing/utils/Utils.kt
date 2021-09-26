package `in`.avimarine.waypointracing.utils

class Utils {
    companion object{
        fun timeDiffInSeconds(first: Long, second: Long): Double{
            return ((second - first) / 1000).toDouble()
        }
    }
}