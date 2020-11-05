package me.alexkovrigin.astrotool.isolines

import com.google.gson.*
import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.geom.*
import java.io.PrintWriter
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 *
 * provides container for isolines,
 * high-level geometry operations,
 * isoline indexing
 */
class IsolineContainer : HashSet<IIsoline> {
    var factory: GeometryFactory
        private set

    constructor(gf: GeometryFactory, isolines: Collection<IIsoline>) : super(isolines.size) {
        factory = gf
        isolines.forEach{ e: IIsoline -> add(e) }
    }

    constructor(gf: GeometryFactory) : super() {
        factory = gf
    }

    constructor(other: IsolineContainer) : super(other.size) {
        factory = other.factory
        other.forEach { x -> add(Isoline(x)) }
    }

    /**
     * @return Bounding box of all isolines (eg. usage: fitting view to whole map)
     */
    val envelope: Envelope
        get() {
            val envelope = Envelope()
            this@IsolineContainer.forEach{
                envelope.expandToInclude(it.geometry.envelopeInternal)
            }
            return envelope
        }

    /**
     * Get convex hull of the map
     */
    fun convexHull(): ConvexHull {
        val pointsList = flatMap {
            it.geometry.coordinates.asList()
        }.toTypedArray()
        return ConvexHull(pointsList, factory)
    }

    val isolinesAsGeometry: ArrayList<Geometry>
        get() {
            val ret = ArrayList<Geometry>(size)
            ret.addAll(stream().map(IIsoline::geometry).collect(Collectors.toList()))
            return ret
        }

    fun toGeometry(gf: GeometryFactory): Geometry = gf.buildGeometry(isolinesAsGeometry)

    fun serialize(path: String) {
        val gsonBuilder = GsonBuilder().setPrettyPrinting()
        gsonBuilder.registerTypeAdapter(LineString::class.java, LineStringAdapter(factory))
        val result = gsonBuilder.create().toJson(this)
        try {
            val writer = PrintWriter(path, "UTF-8")
            writer.println(result)
            writer.close()
        } catch (ex: Exception) {
            println("could not serialize Isoline Container, reason: " + ex.message)
        }
    }

    fun findInCircle(center: Coordinate?, radius: Double): List<IIsoline> {
        val p = factory.createPoint(center)
        return stream().filter(
            Predicate { il: IIsoline -> il.geometry.isWithinDistance(p, radius) }
        ).collect(Collectors.toList())
    }

    fun getIntersecting(ls: LineString): List<IIsoline> {
        return filter { iso -> iso.lineString.intersects(ls) }
    }

    fun filterToIsolineContainer(predicate: (IIsoline) -> Boolean): IsolineContainer =
        IsolineContainer(factory, this.filter(predicate))

    class LineStringAdapter(var gf: GeometryFactory) : JsonSerializer<LineString>,
        JsonDeserializer<LineString> {
        override fun serialize(
            src: LineString, typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val obj = JsonObject()
            val coordinates = src.coordinates
            val xArray = JsonArray()
            val yArray = JsonArray()
            for (i in coordinates.indices) {
                xArray.add(JsonPrimitive(coordinates[i].x))
                yArray.add(JsonPrimitive(coordinates[i].y))
            }
            obj.add("xs", xArray)
            obj.add("ys", yArray)
            return obj
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LineString {
            val jObject = json as JsonObject
            val xsJson = jObject["xs"].asJsonArray
            val ysJson = jObject["ys"].asJsonArray
            val xIt: Iterator<JsonElement> = xsJson.iterator()
            val yIt: Iterator<JsonElement> = ysJson.iterator()
            val coordinates = java.util.ArrayList<Coordinate>()
            while (xIt.hasNext() && yIt.hasNext()) {
                coordinates.add(Coordinate(xIt.next().asDouble, yIt.next().asDouble))
            }
            return gf.createLineString(coordinates.toTypedArray())
        }

    }

    private class IsolineAdapter internal constructor(var gf: GeometryFactory) : JsonDeserializer<Isoline> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Isoline {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(LineString::class.java, LineStringAdapter(gf))
            val g = gsonBuilder.create()
            val jObj = json.asJsonObject
            val ls = g.fromJson(jObj["lineString"], LineString::class.java)
            val type = jObj["type"].asInt
            val slopeSide = jObj["slopeSide"].asString
            val id = jObj["id"].asInt
            val edgeToEdge = jObj["isEdgeToEdge"].asBoolean
            val height = jObj["height"].asDouble
            val newIsoline = Isoline(type, ls.coordinateSequence, gf)
            newIsoline.height = height
            newIsoline.isEdgeToEdge = edgeToEdge
            newIsoline.id = id
            return newIsoline
        }

    }

    private class IsolineContainerAdapter internal constructor(var gf: GeometryFactory) :
        JsonDeserializer<IsolineContainer> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): IsolineContainer {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(Isoline::class.java, IsolineAdapter(gf))
            val g = gsonBuilder.create()
            val isolineJsonArr = json.asJsonArray
            val isolines = ArrayList<IIsoline>()
            for (e in isolineJsonArr) {
                val i = g.fromJson(e, Isoline::class.java)
                isolines.add(i)
            }
            return IsolineContainer(gf, isolines)
        }

    }

    companion object {
        @Throws(Exception::class)
        fun deserialize(path: String): IsolineContainer? {
            val encoded = Files.readAllBytes(Paths.get(path))
            val jsonStr = String(encoded, StandardCharsets.UTF_8)
            val gf = GeometryFactory()
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(IsolineContainer::class.java, IsolineContainerAdapter(gf))
            val g = gsonBuilder.create()
            return try {
                g.fromJson(jsonStr, IsolineContainer::class.java)
            } catch (ex: Exception) {
                println(ex)
                null
            }
        }
    }
}