package pe.edu.upc.ice.track.platform.service.request.domain.model.queries;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;

import java.util.Set;

/**
 * Query to list one page of the caller's service requests, newest first.
 *
 * <p>The scope is never a parameter: an owner lists the requests it created and a technician the
 * requests assigned to it, both resolved from the caller, so no filter can widen what the caller
 * reaches. {@code assignedToMe} only makes that technician scope explicit, and is refused for an
 * owner, which has no assignments.</p>
 *
 * @param username     username of the authenticated caller; required
 * @param callerRole   the role the caller acts in; {@code null} is treated as
 *                     {@link CallerRole#OWNER}
 * @param statuses     restrict to these statuses; {@code null} or empty does not restrict
 * @param assignedToMe whether the caller asked for the requests assigned to it
 * @param page         zero-based page index; {@code null} means the first page
 * @param size         page size, from 1 to {@link #MAX_PAGE_SIZE}; {@code null} means
 *                     {@link #DEFAULT_PAGE_SIZE}
 */
public record ListServiceRequestsQuery(
    String username,
    CallerRole callerRole,
    Set<ServiceStatus> statuses,
    boolean assignedToMe,
    Integer page,
    Integer size) {

  /** Page size used when the client asks for none. */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /**
   * Largest page a client may ask for, for the same reason as the equipment listing: an
   * arbitrarily large page would reopen the unbounded read paging exists to prevent.
   */
  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Applies the defaults and validates the paging values and the scope.
   *
   * @throws IllegalArgumentException when the page index is negative, the page size is outside
   *                                  {@code 1..MAX_PAGE_SIZE}, or an owner asks for its assignments
   */
  public ListServiceRequestsQuery {
    callerRole = callerRole == null ? CallerRole.OWNER : callerRole;
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    page = page == null ? 0 : page;
    size = size == null ? DEFAULT_PAGE_SIZE : size;
    if (page < 0) {
      throw new IllegalArgumentException("Page index must not be negative");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("Page size must be between 1 and %d".formatted(MAX_PAGE_SIZE));
    }
    if (assignedToMe && callerRole != CallerRole.TECHNICIAN) {
      throw new IllegalArgumentException("assignedTo=me is only available to technicians");
    }
  }
}
