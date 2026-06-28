package com.unam.photocleaner.presentation;

import com.unam.photocleaner.data.local.MediaStoreDataSource;
import com.unam.photocleaner.domain.usecase.GroupPhotosUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<MediaStoreDataSource> mediaStoreProvider;

  private final Provider<GroupPhotosUseCase> groupPhotosProvider;

  public MainViewModel_Factory(Provider<MediaStoreDataSource> mediaStoreProvider,
      Provider<GroupPhotosUseCase> groupPhotosProvider) {
    this.mediaStoreProvider = mediaStoreProvider;
    this.groupPhotosProvider = groupPhotosProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(mediaStoreProvider.get(), groupPhotosProvider.get());
  }

  public static MainViewModel_Factory create(Provider<MediaStoreDataSource> mediaStoreProvider,
      Provider<GroupPhotosUseCase> groupPhotosProvider) {
    return new MainViewModel_Factory(mediaStoreProvider, groupPhotosProvider);
  }

  public static MainViewModel newInstance(MediaStoreDataSource mediaStore,
      GroupPhotosUseCase groupPhotos) {
    return new MainViewModel(mediaStore, groupPhotos);
  }
}
