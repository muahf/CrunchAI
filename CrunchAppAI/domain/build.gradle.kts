plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jlleitschuh.ktlint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

        implementation(libs.javax.inject)

        testImplementation(libs.junit)
        testImplementation(libs.kotlinx.coroutines.test)

}