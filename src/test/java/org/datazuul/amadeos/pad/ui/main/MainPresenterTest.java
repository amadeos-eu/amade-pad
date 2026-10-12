package org.datazuul.amadeos.pad.ui.main;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;
import org.datazuul.amadeos.pad.domain.DocumentState;
import org.datazuul.amadeos.pad.service.DocumentService;
import org.datazuul.amadeos.pad.ui.common.EdtExecutor;
import org.datazuul.amadeos.pad.ui.editor.EditorView;
import org.mockito.InOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MainPresenterTest {
    private MainView view;
    private DocumentService service;
    private ExecutorService executor;
    private MainPresenter presenter;
    private EditorView editor;
    private DocumentState state;
    private final List<EditorView> editors = new ArrayList<>();
    private final Path path = Path.of("sample.txt").toAbsolutePath().normalize();

    @BeforeEach
    void setUp() throws Exception {
        onEdt(() -> {
            view = mock(MainView.class, invocation -> {
                assertTrue(SwingUtilities.isEventDispatchThread(), "All view access must be on EDT");
                return RETURNS_DEFAULTS.answer(invocation);
            });
            service = mock(DocumentService.class);
            executor = mock(ExecutorService.class);
            editor = mock(EditorView.class);
            state = new DocumentState();
            when(editor.state()).thenReturn(state);
            when(editor.text()).thenReturn("snapshot");
            doAnswer(invocation -> {
                assertTrue(SwingUtilities.isEventDispatchThread());
                state.loaded(invocation.getArgument(0));
                return null;
            }).when(editor).load(any(), anyString());
            doAnswer(invocation -> {
                assertTrue(SwingUtilities.isEventDispatchThread());
                state.saved(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(editor).saved(any(), anyLong());
            editors.add(editor);
            when(view.editors()).thenAnswer(invocation -> List.copyOf(editors));
            when(view.activeEditor()).thenReturn(editor);
            when(view.addEditor()).thenAnswer(invocation -> {
                if (!editors.contains(editor)) editors.add(editor);
                return editor;
            });
            doAnswer(invocation -> {
                editors.remove(invocation.getArgument(0));
                return null;
            }).when(view).removeEditor(any());
            presenter = new MainPresenter(view, service, executor, new EdtExecutor());
            return null;
        });
    }

    @Test
    void opensOnEdtAndSelectsAlreadyOpenNormalizedPath() throws Exception {
        CompletableFuture<String> io = new CompletableFuture<>();
        when(service.open(path)).thenReturn(io);
        onEdt(() -> { editors.clear(); return null; });
        CompletableFuture<Void> result = onEdt(() -> presenter.open(path));
        verify(editor, never()).load(any(), anyString());
        io.complete("loaded");
        result.join();
        verify(editor).load(path, "loaded");
        onEdt(() -> presenter.open(Path.of(".").resolve("sample.txt"))).join();
        verify(view).selectEditor(editor);
        verify(service, times(1)).open(path);
    }

    @Test
    void missingFileReportsFailureInsteadOfPuttingErrorTextInEditor() throws Exception {
        when(service.open(path)).thenReturn(CompletableFuture.failedFuture(new IOException("not found")));
        onEdt(() -> { editors.clear(); return null; });
        onEdt(() -> presenter.open(path)).join();
        verify(view).showError("Failed to open file: not found");
        verify(view).addEditor();
        verify(editor, never()).load(any(), anyString());
    }

    @Test
    void saveUsesImmutableSnapshotAndDoesNotClearLaterEdits() throws Exception {
        CompletableFuture<Void> io = new CompletableFuture<>();
        when(service.save(path, "snapshot")).thenReturn(io);
        onEdt(() -> { state.loaded(path); state.changed(); return null; });
        CompletableFuture<Boolean> result = onEdt(() -> presenter.saveCurrent(false));
        onEdt(() -> { state.changed(); return null; });
        io.complete(null);
        assertTrue(result.join());
        verify(editor).saved(path, 1L);
        assertTrue(onEdt(state::isModified));
    }

    @Test
    void saveAsFailurePreservesOriginalIdentityAndDirtyState() throws Exception {
        Path other = path.resolveSibling("other.txt");
        onEdt(() -> {
            state.loaded(path);
            state.changed();
            when(view.chooseSavePath(path)).thenReturn(other);
            return null;
        });
        when(service.save(other, "snapshot")).thenReturn(CompletableFuture.failedFuture(new IOException("access denied")));
        assertFalse(onEdt(() -> presenter.saveCurrent(true)).join());
        assertEquals(path, onEdt(state::path));
        assertTrue(onEdt(state::isModified));
        verify(view).showError("Failed to save file: access denied");
        verify(editor, never()).saved(any(), anyLong());
    }

    @Test
    void closeWaitsForSaveBeforeRemovingTheTab() throws Exception {
        CompletableFuture<Void> io = new CompletableFuture<>();
        when(service.save(path, "snapshot")).thenReturn(io);
        onEdt(() -> {
            state.loaded(path);
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.SAVE);
            return null;
        });
        CompletableFuture<Boolean> result = onEdt(presenter::closeActiveTab);
        assertFalse(result.isDone());
        verify(view, never()).removeEditor(any());
        io.complete(null);
        assertTrue(result.join());
        verify(view).removeEditor(editor);
        verify(view).addEditor();
    }

    @Test
    void failedCloseSaveLeavesTabOpen() throws Exception {
        when(service.save(path, "snapshot")).thenReturn(CompletableFuture.failedFuture(new IOException("disk full")));
        onEdt(() -> {
            state.loaded(path);
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.SAVE);
            return null;
        });
        assertFalse(onEdt(presenter::closeActiveTab).join());
        verify(view, never()).removeEditor(any());
        assertTrue(onEdt(state::isModified));
        verify(view).showError("Failed to save file: disk full");
    }

    @Test
    void cancelledSaveChooserLeavesUntitledTabOpen() throws Exception {
        onEdt(() -> {
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.SAVE);
            return null;
        });
        assertFalse(onEdt(presenter::closeActiveTab).join());
        verify(view).chooseSavePath(null);
        verify(view, never()).removeEditor(any());
        verifyNoInteractions(service);
    }

    @Test
    void cancelledCloseAndDiscardAreDistinct() throws Exception {
        onEdt(() -> {
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.CANCEL);
            return null;
        });
        assertFalse(onEdt(presenter::closeActiveTab).join());
        verify(view, never()).removeEditor(any());
        onEdt(() -> {
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.DISCARD);
            return null;
        });
        assertTrue(onEdt(presenter::closeActiveTab).join());
        verify(view).removeEditor(editor);
        verifyNoInteractions(service);
    }

    @Test
    void editingDuringCloseSavePreventsDataLoss() throws Exception {
        CompletableFuture<Void> io = new CompletableFuture<>();
        when(service.save(path, "snapshot")).thenReturn(io);
        onEdt(() -> {
            state.loaded(path);
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.SAVE);
            return null;
        });
        CompletableFuture<Boolean> result = onEdt(presenter::closeActiveTab);
        onEdt(() -> { state.changed(); return null; });
        io.complete(null);
        assertFalse(result.join());
        verify(view, never()).removeEditor(any());
        verify(view).showError(contains("changed while saving"));
    }

    @Test
    void closeWaitsForAlreadyRunningSaveAndDoesNotStartAnother() throws Exception {
        CompletableFuture<Void> io = new CompletableFuture<>();
        when(service.save(path, "snapshot")).thenReturn(io);
        onEdt(() -> { state.loaded(path); state.changed(); return null; });
        CompletableFuture<Boolean> save = onEdt(() -> presenter.saveCurrent(false));
        assertSame(save, onEdt(() -> presenter.saveCurrent(false)));
        CompletableFuture<Boolean> close = onEdt(presenter::closeActiveTab);
        verify(view, never()).confirmClose(any());
        io.complete(null);
        assertTrue(close.join());
        verify(service, times(1)).save(path, "snapshot");
    }

    @Test
    void exitWaitsForSaveThenShutsDownWithoutCreatingANewTab() throws Exception {
        CompletableFuture<Void> io = new CompletableFuture<>();
        when(service.save(path, "snapshot")).thenReturn(io);
        onEdt(() -> {
            state.loaded(path);
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.SAVE);
            return null;
        });
        CompletableFuture<Boolean> result = onEdt(presenter::exit);
        verify(executor, never()).shutdown();
        verify(view, never()).dispose();
        io.complete(null);
        assertTrue(result.join());
        InOrder order = inOrder(view, executor);
        order.verify(view).removeEditor(editor);
        order.verify(executor).shutdown();
        order.verify(view).dispose();
        verify(view, never()).addEditor();
    }

    @Test
    void cancelledExitKeepsExecutorAndWindowAlive() throws Exception {
        onEdt(() -> {
            state.changed();
            when(view.confirmClose(editor)).thenReturn(MainView.SaveChoice.CANCEL);
            return null;
        });
        assertFalse(onEdt(presenter::exit).join());
        verify(executor, never()).shutdown();
        verify(view, never()).dispose();
    }

    @Test
    void exitDuringOpenReportsBusyAndDoesNotCloseWindow() throws Exception {
        CompletableFuture<String> io = new CompletableFuture<>();
        when(service.open(path)).thenReturn(io);
        CompletableFuture<Void> open = onEdt(() -> presenter.open(path));
        assertFalse(onEdt(presenter::exit).join());
        verify(view).showError(contains("wait"));
        verify(executor, never()).shutdown();
        io.complete("content");
        open.join();
    }

    @Test
    void togglesApplyToExistingAndNewEditorsAndEditDelegates() throws Exception {
        onEdt(() -> {
            presenter.setWordWrap(true);
            presenter.setLineNumbers(false);
            presenter.newDocument();
            presenter.edit(EditorView.Edit.UNDO);
            presenter.search(true);
            presenter.findNext();
            presenter.about();
            return null;
        });
        verify(editor, times(2)).setWordWrap(true);
        verify(editor, times(2)).setLineNumbersVisible(false);
        verify(editor).edit(EditorView.Edit.UNDO);
        verify(view).showSearch(true);
        verify(view).findNext();
        verify(view).showAbout();
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
