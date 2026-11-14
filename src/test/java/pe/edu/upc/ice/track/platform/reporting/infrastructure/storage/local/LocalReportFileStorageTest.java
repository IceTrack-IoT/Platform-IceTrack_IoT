package pe.edu.upc.ice.track.platform.reporting.infrastructure.storage.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalReportFileStorageTest {

  @TempDir Path directory;
  private LocalReportFileStorage storage;

  @BeforeEach
  void setUp() {
    storage = new LocalReportFileStorage(directory.resolve("root").toString());
  }

  @Test
  @DisplayName("store(key, bytes, type) -> creates intermediate folders and writes the content (AAA)")
  void stores() throws Exception {
    // Act
    storage.store("reports/5/500.pdf", new byte[] {1, 2, 3}, "application/pdf");

    // Assert
    assertThat(Files.readAllBytes(directory.resolve("root/reports/5/500.pdf"))).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("store(key, bytes, type) twice -> second call overwrites the first (AAA)")
  void overwrites() throws Exception {
    // Arrange
    storage.store("a.csv", new byte[] {1}, "text/csv");

    // Act
    storage.store("a.csv", new byte[] {9, 9}, "text/csv");

    // Assert
    assertThat(Files.readAllBytes(directory.resolve("root/a.csv"))).containsExactly(9, 9);
  }

  @Test
  @DisplayName("store(null content / null key) -> NullPointerException (AAA)")
  void nulls() {
    // Act & Assert
    assertThatThrownBy(() -> storage.store("a", null, "x")).hasMessage("content must not be null");
    assertThatThrownBy(() -> storage.store(null, new byte[0], "x")).hasMessage("key must not be null");
  }

  @Test
  @DisplayName("store/delete/generateDownloadUrl with a traversal key -> IllegalArgumentException, nothing written outside (AAA)")
  void pathTraversal() {
    // Act & Assert
    assertThatThrownBy(() -> storage.store("../escape.pdf", new byte[] {1}, "x"))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid storage key: ../escape.pdf");
    assertThatThrownBy(() -> storage.delete("reports/../../escape.pdf")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.generateDownloadUrl("../../etc/passwd", Duration.ofMinutes(1), "f"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(Files.exists(directory.resolve("escape.pdf"))).isFalse();
  }

  @Test
  @DisplayName("store(key under an existing file) -> UncheckedIOException (AAA)")
  void ioFailure() throws Exception {
    // Arrange
    Files.createDirectories(directory.resolve("root"));
    Files.writeString(directory.resolve("root/blocker"), "x");

    // Act & Assert
    assertThatThrownBy(() -> storage.store("blocker/child.pdf", new byte[] {1}, "x"))
        .isInstanceOf(UncheckedIOException.class).hasMessage("Could not store report file blocker/child.pdf");
  }

  @Test
  @DisplayName("delete(key) -> removes the file; deleting a missing file is a no-op (AAA)")
  void deletes() throws Exception {
    // Arrange
    storage.store("x.pdf", new byte[] {1}, "x");

    // Act
    storage.delete("x.pdf");
    storage.delete("x.pdf");

    // Assert
    assertThat(Files.exists(directory.resolve("root/x.pdf"))).isFalse();
  }

  @Test
  @DisplayName("delete(non-empty directory key) -> UncheckedIOException (AAA)")
  void deleteFailure() {
    // Arrange
    storage.store("folder/inner.pdf", new byte[] {1}, "x");

    // Act & Assert
    assertThatThrownBy(() -> storage.delete("folder"))
        .isInstanceOf(UncheckedIOException.class).hasMessage("Could not delete report file folder");
  }

  @Test
  @DisplayName("generateDownloadUrl(key, ttl, name) -> file URI expiring after the validity (AAA)")
  void downloadUrl() {
    // Arrange
    var before = Instant.now();

    // Act
    var signed = storage.generateDownloadUrl("reports/5/500.pdf", Duration.ofMinutes(15), "r.pdf");

    // Assert
    assertThat(signed.url()).startsWith("file:").endsWith("reports/5/500.pdf");
    assertThat(signed.expiresAt()).isBetween(before.plus(Duration.ofMinutes(15)), Instant.now().plus(Duration.ofMinutes(15)));
    assertThatThrownBy(() -> storage.generateDownloadUrl("k", null, "f")).hasMessage("validFor must not be null");
  }
}
