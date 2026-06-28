package com.unam.photocleaner.di;

import com.unam.photocleaner.data.local.db.AppDatabase;
import com.unam.photocleaner.data.local.db.PhotoHashDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvidePhotoHashDaoFactory implements Factory<PhotoHashDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvidePhotoHashDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PhotoHashDao get() {
    return providePhotoHashDao(dbProvider.get());
  }

  public static AppModule_ProvidePhotoHashDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvidePhotoHashDaoFactory(dbProvider);
  }

  public static PhotoHashDao providePhotoHashDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePhotoHashDao(db));
  }
}
