package pe.edu.upc.ice.track.platform.iam.infrastructure.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Request wrapper that reads the beginning of the body ahead of the controller and replays it.
 *
 * <p>At most {@code maxInspectedBytes + 1} bytes are read up front. When the whole body fits in
 * {@code maxInspectedBytes}, it is exposed through {@link #completeBody()} for inspection. Either
 * way, the downstream reader receives the bytes read up front followed by whatever is left in the
 * original stream, so the body reaches the controller unchanged and no oversized body is ever
 * buffered whole.</p>
 *
 * <p>Only blocking reads are supported, which is what Spring MVC uses.</p>
 */
final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] head;
  private final boolean complete;
  private ReplayingServletInputStream inputStream;
  private BufferedReader reader;

  /**
   * Reads the beginning of the body of a request.
   *
   * @param request           the request to wrap
   * @param maxInspectedBytes the largest body exposed for inspection
   * @throws IOException when the body cannot be read
   */
  CachedBodyHttpServletRequest(HttpServletRequest request, int maxInspectedBytes) throws IOException {
    super(request);
    this.head = request.getInputStream().readNBytes(maxInspectedBytes + 1);
    this.complete = head.length <= maxInspectedBytes;
  }

  /**
   * @return the whole body when it fits in the inspection limit, or empty when it is larger
   */
  Optional<byte[]> completeBody() {
    return complete ? Optional.of(head.clone()) : Optional.empty();
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    if (inputStream == null) {
      inputStream = new ReplayingServletInputStream(head, super.getInputStream());
    }
    return inputStream;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    if (reader == null) {
      var encoding = getCharacterEncoding();
      var charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
    return reader;
  }

  /**
   * Stream yielding the bytes read up front, then the rest of the original stream.
   */
  private static final class ReplayingServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream head;
    private final ServletInputStream rest;
    private boolean finished;

    ReplayingServletInputStream(byte[] head, ServletInputStream rest) {
      this.head = new ByteArrayInputStream(head);
      this.rest = rest;
    }

    @Override
    public int read() throws IOException {
      var next = head.read();
      if (next == -1) {
        next = rest.read();
      }
      if (next == -1) {
        finished = true;
      }
      return next;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      if (length == 0) {
        return 0;
      }
      var count = head.read(buffer, offset, length);
      if (count > 0) {
        return count;
      }
      count = rest.read(buffer, offset, length);
      if (count == -1) {
        finished = true;
      }
      return count;
    }

    @Override
    public boolean isFinished() {
      return finished || (head.available() == 0 && rest.isFinished());
    }

    @Override
    public boolean isReady() {
      return head.available() > 0 || rest.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException("Non-blocking reads are not supported on rate-limited sign-in requests");
    }
  }
}
