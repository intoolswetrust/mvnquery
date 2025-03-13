package com.github.kwart.mvnquery;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.index.Indexer;
import org.apache.maven.index.updater.IndexUpdater;

@Singleton
@Named
public final class MvnIndexerContext {

    protected final Indexer indexer;
    protected final IndexUpdater indexUpdater;

    @Inject
    public MvnIndexerContext(Indexer indexer, IndexUpdater indexUpdater) {
        this.indexer = indexer;
        this.indexUpdater = indexUpdater;
    }

}
