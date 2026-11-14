package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency;

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

/**
 * Request wrapper that reads the body once, up front, and serves it again to whoever reads it next.
 *
 * <p>The idempotency filter has to digest the body before the controller reads it; the servlet input
 * stream can only be read once, so the bytes are kept here.</p>
 */
class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] body;

  CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    this.body = request.getInputStream().readAllBytes();
  }

  /**
   * @return the request body, as read
   */
  byte[] body() {
    return body;
  }

  @Override
  public ServletInputStream getInputStream() {
    var source = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return source.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Asynchronous reads are not supported");
      }

      @Override
      public int read() {
        return source.read();
      }

      @Override
      public int read(byte[] buffer, int offset, int length) {
        return source.read(buffer, offset, length);
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    var encoding = getCharacterEncoding();
    Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    return new BufferedReader(new InputStreamReader(getInputStream(), charset));
  }

  @Override
  public int getContentLength() {
    return body.length;
  }

  @Override
  public long getContentLengthLong() {
    return body.length;
  }
}
