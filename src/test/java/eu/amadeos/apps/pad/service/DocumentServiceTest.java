package eu.amadeos.apps.pad.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.domain.TextFormat;
import eu.amadeos.apps.pad.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {
    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final Path path = Path.of("sample.txt");
    private final TextDocument content = new TextDocument("content", TextFormat.DEFAULT);
    private final TextDocument snapshot = new TextDocument("snapshot", TextFormat.DEFAULT);

    @Test
    void runsIoOnVirtualThreadsOutsideEdt() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            DocumentService service = new DocumentService(repository, executor);
            when(repository.read(path)).thenAnswer(invocation -> {
                assertTrue(Thread.currentThread().isVirtual());
                assertFalse(SwingUtilities.isEventDispatchThread());
                return content;
            });
            doAnswer(invocation -> {
                assertTrue(Thread.currentThread().isVirtual());
                assertFalse(SwingUtilities.isEventDispatchThread());
                return null;
            }).when(repository).write(path, snapshot);
            assertEquals(content, service.open(path).join());
            service.save(path, snapshot).join();
            verify(repository).write(path, snapshot);
        }
    }

    @Test
    void preservesIoFailuresInFutures() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            DocumentService service = new DocumentService(repository, executor);
            IOException failure = new IOException("access denied");
            when(repository.read(path)).thenThrow(failure);
            doThrow(failure).when(repository).write(path, content);
            assertSame(failure, assertThrows(CompletionException.class, () -> service.open(path).join()).getCause());
            assertSame(failure, assertThrows(CompletionException.class, () -> service.save(path, content).join()).getCause());
        }
    }
}
