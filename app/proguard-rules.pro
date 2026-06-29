# TensorFlow Lite (ML Kit 내부 의존)
-keep class org.tensorflow.** { *; }

# Room: 엔티티 필드명은 리플렉션으로 참조되므로 보존
-keep class com.unam.gallerycleaner.data.local.db.**Entity { *; }

# WorkManager: 클래스 이름으로 인스턴스화되므로 보존
-keepnames class * extends androidx.work.ListenableWorker

# Hilt ViewModel: 리플렉션으로 생성되므로 이름 보존
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Parcelable: @Parcelize 생성 CREATOR 보존 (proguard-android-optimize.txt 보완)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 도메인 모델: SavedStateHandle / Parcelable 직렬화에 필요
-keep class com.unam.gallerycleaner.domain.model.** implements android.os.Parcelable { *; }
-keep class com.unam.gallerycleaner.presentation.UiState { *; }
-keep class com.unam.gallerycleaner.presentation.UiState$* { *; }

# Kotlin: sealed class / data class 리플렉션 안정성
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
