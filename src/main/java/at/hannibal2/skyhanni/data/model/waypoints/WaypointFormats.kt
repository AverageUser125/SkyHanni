package at.hannibal2.skyhanni.data.model.waypoints

object WaypointFormats {

    private val formats: List<WaypointFormat> by lazy {
        listOf(
            ColeweightWaypointFormat(),
            SkytilsWaypointFormat()
        )
    }

    // TODO add Skyblocker waypoint format
    fun load(data: String): Pair<Waypoints<SkyHanniWaypoint>, String>? =
        formats.firstNotNullOfOrNull { format ->
            format.load(data)?.let { it to format.name }
        }

    fun export(waypoints: Waypoints<SkyHanniWaypoint>, name: String): String? =
        formats.firstOrNull { it.name == name }?.export(waypoints)

    fun names(): List<String> = formats.map { it.name }
}
