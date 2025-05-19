plugins {
    id("com.android.application") version "8.9.3" apply false
    kotlin("android") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}