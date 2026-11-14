package pe.edu.upc.ice.track.platform.iam.test.util;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Runs a block inside a Spring transaction interceptor backed by a mocked
 * {@link PlatformTransactionManager}. Application services that mark their transaction as
 * rollback-only through {@code TransactionAspectSupport.currentTransactionStatus()} can then be
 * exercised without a database, and the test can assert on {@link #status()}.
 */
public final class TransactionTestSupport extends TransactionAspectSupport {

  private final TransactionStatus status = mock(TransactionStatus.class);
  private final Method method;

  public TransactionTestSupport() {
    var transactionManager = mock(PlatformTransactionManager.class);
    lenient().when(transactionManager.getTransaction(any())).thenReturn(status);
    setTransactionManager(transactionManager);
    setTransactionAttributeSource(new MatchAlwaysTransactionAttributeSource());
    try {
      this.method = Object.class.getMethod("toString");
    } catch (NoSuchMethodException exception) {
      throw new AssertionError(exception);
    }
  }

  /** The status object returned to the code running inside {@link #call(Supplier)}. */
  public TransactionStatus status() {
    return status;
  }

  /** Executes the supplier while a transaction is bound to the current thread. */
  @SuppressWarnings("unchecked")
  public <T> T call(Supplier<T> block) {
    try {
      return (T) invokeWithinTransaction(method, TransactionTestSupport.class, block::get);
    } catch (RuntimeException | Error exception) {
      throw exception;
    } catch (Throwable throwable) {
      throw new AssertionError(throwable);
    }
  }
}
