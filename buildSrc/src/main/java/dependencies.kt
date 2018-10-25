object Versions {
    val kotlin = "1.2.51"
    val room = "2.0.0-rc01"
    val retrofit = "2.4.0"
    val dagger2 = "2.16"
}

object Libs {
    val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    val ktx = "androidx.core:core-ktx:1.0.0"
    val appcompat = "androidx.appcompat:appcompat:1.0.0"
    val design = "com.google.android.material:material:1.0.0"
    val recyclerview = "androidx.recyclerview:recyclerview:1.0.0"
    val constraint_layout = "androidx.constraintlayout:constraintlayout:1.1.2"
    val lifecycle = "androidx.lifecycle:lifecycle-extensions:2.0.0-rc01"

    val room = "androidx.room:room-runtime:${Versions.room}"
    val roomCompiler = "androidx.room:room-compiler:${Versions.room}"

    val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    val retrofitRx = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    val retrofitMoshi = "com.squareup.retrofit2:converter-moshi:${Versions.retrofit}"

    val dagger2 = "com.google.dagger:dagger:${Versions.dagger2}"
    val dagger2Compiler = "com.google.dagger:dagger-compiler:${Versions.dagger2}"
    val daggerAnnotations = "org.glassfish:javax.annotation:3.1.1"

    val picasso = "com.squareup.picasso:picasso:2.5.2"
}