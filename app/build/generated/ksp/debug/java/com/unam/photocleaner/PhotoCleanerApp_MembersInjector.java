package com.unam.photocleaner;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class PhotoCleanerApp_MembersInjector implements MembersInjector<PhotoCleanerApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public PhotoCleanerApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<PhotoCleanerApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new PhotoCleanerApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(PhotoCleanerApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.unam.photocleaner.PhotoCleanerApp.workerFactory")
  public static void injectWorkerFactory(PhotoCleanerApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
