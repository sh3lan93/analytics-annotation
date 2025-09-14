
plugins {
    id("java-library-convention")
}

mavenPublishing {
    coordinates(group.toString(), "easy-analytics-annotation", version.toString())
}
