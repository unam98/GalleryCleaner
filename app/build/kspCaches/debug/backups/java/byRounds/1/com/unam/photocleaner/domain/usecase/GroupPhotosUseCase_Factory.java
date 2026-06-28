package com.unam.photocleaner.domain.usecase;

import android.content.Context;
import com.unam.photocleaner.data.local.db.PhotoHashDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class GroupPhotosUseCase_Factory implements Factory<GroupPhotosUseCase> {
  private final Provider<Context> contextProvider;

  private final Provider<PhotoHashDao> photoHashDaoProvider;

  public GroupPhotosUseCase_Factory(Provider<Context> contextProvider,
      Provider<PhotoHashDao> photoHashDaoProvider) {
    this.contextProvider = contextProvider;
    this.photoHashDaoProvider = photoHashDaoProvider;
  }

  @Override
  public GroupPhotosUseCase get() {
    return newInstance(contextProvider.get(), photoHashDaoProvider.get());
  }

  public static GroupPhotosUseCase_Factory create(Provider<Context> contextProvider,
      Provider<PhotoHashDao> photoHashDaoProvider) {
    return new GroupPhotosUseCase_Factory(contextProvider, photoHashDaoProvider);
  }

  public static GroupPhotosUseCase newInstance(Context context, PhotoHashDao photoHashDao) {
    return new GroupPhotosUseCase(context, photoHashDao);
  }
}
