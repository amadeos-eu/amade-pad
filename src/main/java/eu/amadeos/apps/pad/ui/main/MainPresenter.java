package eu.amadeos.apps.pad.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.domain.UnencodableTextException;
import eu.amadeos.apps.pad.repository.BinaryFileException;
import eu.amadeos.apps.pad.service.DocumentService;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.apps.pad.ui.editor.EditorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final Messages msg;
    private final LocaleSettings locales;
    private final Map<EditorView, CompletableFuture<Boolean>> saves = new IdentityHashMap<>();
    private final Set<EditorView> closing = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Path> opening = new HashSet<>();
    private boolean exiting;
    private boolean closed;
    private boolean wordWrap;
    private boolean lineNumbers = true;

    @Inject
    public MainPresenter(MainView view, DocumentService documents, ExecutorService executor, EdtExecutor edt,
                         Messages msg, LocaleSettings locales) {
        this.view = view;
        this.documents = documents;
        this.executor = executor;
        this.edt = edt;
        this.msg = msg;
        this.locales = locales;
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

    // --8<-- [start:open]
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
                report("error.open", error);
                if (view.editors().isEmpty()) addEditor();
            } else {
                addEditor().load(normalized, content);
            }
            opening.remove(normalized);
            return null;
        }, edt);
    }
    // --8<-- [end:open]

    public CompletableFuture<Boolean> saveCurrent(boolean saveAs) {
        if (!available()) return CompletableFuture.completedFuture(false);
        EditorView editor = view.activeEditor();
        return editor == null ? CompletableFuture.completedFuture(false) : save(editor, saveAs);
    }

    // --8<-- [start:save]
    private CompletableFuture<Boolean> save(EditorView editor, boolean saveAs) {
        if (saves.containsKey(editor)) return saves.get(editor);
        Path path = editor.state().path();
        if (saveAs || path == null) path = view.chooseSavePath(path);
        if (path == null) return CompletableFuture.completedFuture(false);
        Path target = path.toAbsolutePath().normalize();
        TextDocument snapshot = new TextDocument(editor.text(), editor.state().format());
        long revision = editor.state().revision();
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        saves.put(editor, result);
        write(editor, target, snapshot, revision).whenCompleteAsync((success, error) -> {
            saves.remove(editor);
            if (error != null) result.completeExceptionally(error);
            else result.complete(success);
        }, edt);
        return result;
    }
    // --8<-- [end:save]

    // --8<-- [start:write]
    /**
     * Writes the snapshot; if its charset cannot store the text, offers to save as UTF-8 instead
     * of replacing characters.
     */
    private CompletableFuture<Boolean> write(EditorView editor, Path target, TextDocument snapshot, long revision) {
        return documents.save(target, snapshot).handleAsync((unused, error) -> {
            if (error == null) {
                editor.saved(target, revision, snapshot.format());
                return CompletableFuture.completedFuture(true);
            }
            if (rootCause(error) instanceof UnencodableTextException unencodable
                    && !snapshot.format().isUnicode()
                    && view.confirmSaveAsUtf8(unencodable.charset().name())) {
                return write(editor, target, snapshot.withFormat(snapshot.format().asUtf8()), revision);
            }
            report("error.save", error);
            return CompletableFuture.completedFuture(false);
        }, edt).thenCompose(next -> next);
    }
    // --8<-- [end:write]

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
                view.showError(msg.get("error.changed-while-saving"));
                return false;
            }
            view.removeEditor(editor);
            return true;
        }, edt);
    }

    public CompletableFuture<Boolean> exit() {
        if (!available()) return CompletableFuture.completedFuture(false);
        if (!opening.isEmpty() || !saves.isEmpty() || !closing.isEmpty()) {
            view.showError(msg.get("error.busy"));
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

    // --8<-- [start:language]
    /**
     * Stores the application's own language ("Options > Language"); empty follows the desktop. The
     * language changes at the next start, which the user is told.
     */
    public void setLanguage(Optional<Language> language) {
        if (!available()) return;
        try {
            locales.setApplicationLanguage(language);
        } catch (IOException e) {
            report("error.save", e);
            return;
        }
        if (locales.restartRequired()) {
            view.showInfo(Messages.commons(msg.locale()).format("language.restart", "amade-pad"));
        }
    }
    // --8<-- [end:language]

    private void report(String key, Throwable error) {
        LOG.error(key, error);
        view.showError(msg.format(key, describe(rootCause(error))));
    }

    // --8<-- [start:describe]
    /** A translated reason for the usual errors, else the exception's own message. */
    private String describe(Throwable error) {
        if (error instanceof NoSuchFileException) return msg.get("error.not-found");
        if (error instanceof AccessDeniedException) return msg.get("error.access-denied");
        if (error instanceof BinaryFileException) return msg.get("error.binary");
        if (error instanceof UnencodableTextException u) return msg.format("error.unencodable", u.charset().name());
        return error.getMessage();
    }
    // --8<-- [end:describe]

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
