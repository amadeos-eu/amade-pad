package eu.amadeos.apps.pad.service;

import com.google.inject.Inject;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.repository.DocumentRepository;

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

    public CompletableFuture<TextDocument> open(Path path) {
        Objects.requireNonNull(path);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.read(path);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> save(Path path, TextDocument document) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(document);
        return CompletableFuture.runAsync(() -> {
            try {
                repository.write(path, document);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}
