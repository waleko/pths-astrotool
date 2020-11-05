package me.alexkovrigin.astrotool.utils

object Constants {
    //Edges
    const val EDGE_WITHIN_THRESHOLD = 1.0
    const val EDGE_CONCAVE_THRESHOLD = 200.0

    //Connections
    const val CONNECTIONS_MIN_ANGLE_DEG = 10.0
    const val CONNECTIONS_MAX_ANGLE_DEG = 100.0
    const val CONNECTIONS_WELD_DIST = 4.0
    const val CONNECTIONS_MAX_DIST = 200.0
    const val CONNECTIONS_NEAR_STEEP_THRESHOLD = 0.5

    // Don't count intersections within this offset from edges
    const val CONNECTIONS_INTERSECTION_OFFSET = 0.1

    //DRAWING
    const val DRAWING_LINE_WIDTH = 0.8
    const val DRAWING_POINT_WIDTH = 2.8
    const val DRAWING_INTERPOLATION_STEP = 0.2

    //Map parsing
    //    public static final int[] isoline_ids = new int[] {1,2,3};
    //    public static final int[] slope_ids = new int[] {1,2,3};
    const val slope_near_dist = 2.0
    const val slope_length = 5.0
    const val tangent_precision = 0.6
    const val map_scale_fix = 1600.0
    const val DESERIALIZATION_BEZIER_STEP = 0.2

    //Nearby detection
    const val NEARBY_TRACE_STEP = 20.0
    const val NEARBY_TRACE_LENGTH = 1000.0
    const val NEARBY_TRACE_OFFSET = 0.1
    const val NEARBY_HILL_THRESHOLD_AREA = 10000.0

    //Interpolation
    //public static final double INTERPOLATION_STEP = 2.5;
    //public static final double INTERPOLATION_FADE_DISTANCE = 10;
    const val INTERPOLATION_FADE_STRENGTH = 3.0
    const val INTERPOLATION_HILL_TANGENT = 1.0
    const val INTERPOLATION_MAX_DISTANCE = 100000000f
}