package pe.edu.upc.ice.track.platform.iam.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetAllUsersQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByUsernameQuery;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves IAM user read queries.
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {
  private final UserRepository userRepository;

  public UserQueryServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
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
}
