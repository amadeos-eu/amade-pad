package org.datazuul.amadeos.pad.service;

import com.google.inject.Inject;
import org.datazuul.amadeos.pad.repository.DocumentRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

public final class DocumentService {
    private final DocumentRepository repository;
    private final ExecutorService executor;

    @Inject
    public DocumentService(DocumentRepository repository, ExecutorService executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public CompletableFuture<String> open(Path path) {
        Objects.requireNonNull(path);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.read(path);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> save(Path path, String content) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(content);
        return CompletableFuture.runAsync(() -> {
            try {
                repository.write(path, content);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}
