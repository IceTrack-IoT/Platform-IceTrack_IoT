package pe.edu.upc.ice.track.platform.iam.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl.ExternalProfileService;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetAllUsersQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetCurrentUserQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByUsernameQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserEmailByUserIdQuery;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves IAM user read queries.
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {
  private final UserRepository userRepository;
  private final ExternalProfileService externalProfileService;

  public UserQueryServiceImpl(UserRepository userRepository, ExternalProfileService externalProfileService) {
    this.userRepository = userRepository;
    this.externalProfileService = externalProfileService;
  }

  @Override
  public List<User> handle(GetAllUsersQuery query) {
    return userRepository.findAll();
  }

  @Override
  public Optional<User> handle(GetUserByIdQuery query) {
    return userRepository.findById(query.userId());
  }

  @Override
  public Optional<User> handle(GetUserByUsernameQuery query) {
    return userRepository.findByUsername(query.username());
  }

  @Override
  public Optional<User> handle(GetCurrentUserQuery query) {
    if (query == null) return Optional.empty();
    return userRepository.findByUsername(query.username());
  }

  @Override
  public Optional<String> handle(GetUserEmailByUserIdQuery query) {
    if (query == null || query.userId() == null) return Optional.empty();
    return externalProfileService.fetchEmailByUserId(query.userId());
  }
}
