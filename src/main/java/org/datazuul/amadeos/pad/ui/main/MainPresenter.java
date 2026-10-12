package org.datazuul.amadeos.pad.ui.main;

import com.google.inject.Inject;
import org.datazuul.amadeos.pad.service.DocumentService;
import org.datazuul.amadeos.pad.ui.common.EdtExecutor;
import org.datazuul.amadeos.pad.ui.editor.EditorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Coordinates document workflows on the EDT; only immutable file snapshots leave it.
 */
public final class MainPresenter {
    private static final Logger LOG = LoggerFactory.getLogger(MainPresenter.class);
    private final MainView view;
    private final DocumentService documents;
    private final ExecutorService executor;
    private final EdtExecutor edt;
    private final Map<EditorView, CompletableFuture<Boolean>> saves = new IdentityHashMap<>();
    private final Set<EditorView> closing = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Path> opening = new HashSet<>();
    private boolean exiting;
    private boolean closed;
    private boolean wordWrap;
    private boolean lineNumbers = true;

    @Inject
    public MainPresenter(MainView view, DocumentService documents, ExecutorService executor, EdtExecutor edt) {
        this.view = view;
        this.documents = documents;
        this.executor = executor;
        this.edt = edt;
    }

    private boolean available() {
        EdtExecutor.requireEdt();
        return !exiting && !closed;
    }

    public void newDocument() {
        if (available()) addEditor();
    }

    private EditorView addEditor() {
        EditorView editor = view.addEditor();
        editor.setWordWrap(wordWrap);
        editor.setLineNumbersVisible(lineNumbers);
        return editor;
    }

    public void chooseOpen() {
        if (!available()) return;
        Path path = view.chooseOpenPath();
        if (path != null) open(path);
    }

    public CompletableFuture<Void> open(Path path) {
        if (!available()) return CompletableFuture.completedFuture(null);
        Path normalized = path.toAbsolutePath().normalize();
        for (EditorView editor : view.editors()) {
            if (normalized.equals(editor.state().path())) {
                view.selectEditor(editor);
                return CompletableFuture.completedFuture(null);
            }
        }
        if (!opening.add(normalized)) return CompletableFuture.completedFuture(null);
        return documents.open(normalized).handleAsync((content, error) -> {
            if (error != null) {
                report("Failed to open file", error);
                if (view.editors().isEmpty()) addEditor();
            } else {
                addEditor().load(normalized, content);
            }
            opening.remove(normalized);
            return null;
        }, edt);
    }

    public CompletableFuture<Boolean> saveCurrent(boolean saveAs) {
        if (!available()) return CompletableFuture.completedFuture(false);
        EditorView editor = view.activeEditor();
        return editor == null ? CompletableFuture.completedFuture(false) : save(editor, saveAs);
    }

    private CompletableFuture<Boolean> save(EditorView editor, boolean saveAs) {
        if (saves.containsKey(editor)) return saves.get(editor);
        Path path = editor.state().path();
        if (saveAs || path == null) path = view.chooseSavePath(path);
        if (path == null) return CompletableFuture.completedFuture(false);
        Path target = path.toAbsolutePath().normalize();
        String content = editor.text();
        long revision = editor.state().revision();
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        saves.put(editor, result);
        documents.save(target, content).handleAsync((unused, error) -> {
            if (error != null) {
                report("Failed to save file", error);
                return false;
            }
            editor.saved(target, revision);
            return true;
        }, edt).whenCompleteAsync((success, error) -> {
            saves.remove(editor);
            if (error != null) result.completeExceptionally(error);
            else result.complete(success);
        }, edt);
        return result;
    }

    public CompletableFuture<Boolean> closeActiveTab() {
        if (!available()) return CompletableFuture.completedFuture(false);
        EditorView editor = view.activeEditor();
        if (editor == null) return CompletableFuture.completedFuture(true);
        return close(editor).thenApplyAsync(success -> {
            if (success && view.editors().isEmpty()) addEditor();
            return success;
        }, edt);
    }

    private CompletableFuture<Boolean> close(EditorView editor) {
        if (!closing.add(editor)) return CompletableFuture.completedFuture(false);
        CompletableFuture<Boolean> ready;
        if (saves.containsKey(editor)) {
            ready = saves.get(editor);
        } else if (editor.state().isModified()) {
            MainView.SaveChoice choice = view.confirmClose(editor);
            if (choice == MainView.SaveChoice.DISCARD) {
                closing.remove(editor);
                view.removeEditor(editor);
                return CompletableFuture.completedFuture(true);
            }
            ready = switch (choice) {
                case SAVE -> save(editor, false);
                case DISCARD -> CompletableFuture.completedFuture(true);
                case CANCEL -> CompletableFuture.completedFuture(false);
            };
        } else {
            ready = CompletableFuture.completedFuture(true);
        }
        return ready.thenApplyAsync(success -> {
            closing.remove(editor);
            if (!success) return false;
            if (editor.state().isModified()) {
                view.showError("The document changed while saving. Save the new changes before closing.");
                return false;
            }
            view.removeEditor(editor);
            return true;
        }, edt);
    }

    public CompletableFuture<Boolean> exit() {
        if (!available()) return CompletableFuture.completedFuture(false);
        if (!opening.isEmpty() || !saves.isEmpty() || !closing.isEmpty()) {
            view.showError("Please wait for the current file operation to finish before exiting.");
            return CompletableFuture.completedFuture(false);
        }
        exiting = true;
        return closeForExit(List.copyOf(view.editors()), 0).thenApplyAsync(success -> {
            exiting = false;
            if (success) {
                closed = true;
                executor.shutdown();
                view.dispose();
            }
            return success;
        }, edt);
    }

    private CompletableFuture<Boolean> closeForExit(List<EditorView> editors, int index) {
        if (index == editors.size()) return CompletableFuture.completedFuture(true);
        return close(editors.get(index)).thenComposeAsync(success ->
                success ? closeForExit(editors, index + 1) : CompletableFuture.completedFuture(false), edt);
    }

    public void edit(EditorView.Edit action) {
        if (!available()) return;
        EditorView editor = view.activeEditor();
        if (editor != null) editor.edit(action);
    }

    public void setWordWrap(boolean enabled) {
        if (!available()) return;
        wordWrap = enabled;
        view.editors().forEach(editor -> editor.setWordWrap(enabled));
    }

    public void setLineNumbers(boolean enabled) {
        if (!available()) return;
        lineNumbers = enabled;
        view.editors().forEach(editor -> editor.setLineNumbersVisible(enabled));
    }

    public void search(boolean replace) {
        if (available()) view.showSearch(replace);
    }

    public void findNext() {
        if (available()) view.findNext();
    }

    public void about() {
        if (available()) view.showAbout();
    }

    private void report(String message, Throwable error) {
        LOG.error(message, error);
        Throwable cause = error;
        while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        view.showError(message + ": " + cause.getMessage());
    }
}
