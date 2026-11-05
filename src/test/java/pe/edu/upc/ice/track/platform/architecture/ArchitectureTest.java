package pe.edu.upc.ice.track.platform.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture rules of the platform, checked against the compiled production classes.
 *
 * <p>Written as plain JUnit Jupiter tests on top of the ArchUnit core library rather than through
 * an ArchUnit test engine, so they run on whatever JUnit Platform version Spring Boot manages.</p>
 *
 * <ul>
 *   <li><strong>Domain isolation</strong> - the domain layer knows no other layer.</li>
 *   <li><strong>Hexagonal layer flow</strong> - the application layer knows no adapter, apart from
 *       the published contracts of other contexts ({@code interfaces.acl} facades and
 *       {@code interfaces.events} integration events), which the outbound services exist to
 *       consume.</li>
 *   <li><strong>Cross-context encapsulation</strong> - a bounded context reaches another one only
 *       through its {@code interfaces.acl} or {@code interfaces.events} packages, or through
 *       {@code shared}.</li>
 *   <li><strong>Enum parity</strong> - every REST enum mirrors its domain enum constant for
 *       constant, so no domain value can become unrepresentable on the wire or the other way
 *       around.</li>
 * </ul>
 */
class ArchitectureTest {

  private static final String ROOT_PACKAGE = "pe.edu.upc.ice.track.platform";

  /** Every bounded context, by its package below {@link #ROOT_PACKAGE}. {@code shared} is not one. */
  private static final List<String> BOUNDED_CONTEXTS = List.of(
      "assets", "device", "iam", "monitoring", "notifications", "profiles", "reporting", "service.request");

  /** The packages through which a context publishes itself to the others. */
  private static final String[] PUBLISHED_CONTRACTS = {"..interfaces.acl..", "..interfaces.events.."};

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() {
    productionClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT_PACKAGE);
  }

  // ---------------------------------------------------------------------------------------------
  // 1. Domain isolation
  // ---------------------------------------------------------------------------------------------

  @Test
  void domainDependsOnNoOtherLayer() {
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")
        .because("the domain model must stay free of use cases, adapters and delivery mechanisms")
        .check(productionClasses);
  }

  // ---------------------------------------------------------------------------------------------
  // 2. Hexagonal layer flow
  // ---------------------------------------------------------------------------------------------

  @Test
  void applicationDependsOnNoInfrastructure() {
    noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .because("adapters plug into the application layer, never the other way around")
        .check(productionClasses);
  }

  @Test
  void applicationDependsOnNoInterfacesExceptPublishedContracts() {
    DescribedPredicate<JavaClass> interfacesOtherThanPublishedContracts =
        resideInAnyPackage("..interfaces..").and(not(resideInAnyPackage(PUBLISHED_CONTRACTS)));
    noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat(interfacesOtherThanPublishedContracts)
        .because("only ACL facades and integration events are contracts the application layer may consume")
        .check(productionClasses);
  }

  @Test
  void notificationsApplicationDependsOnNoMonitoringClass() {
    noClasses().that().resideInAPackage(ROOT_PACKAGE + ".notifications.application..")
        .should().dependOnClassesThat().resideInAPackage(ROOT_PACKAGE + ".monitoring..")
        .because("alerts reach notifications through infrastructure.monitoring.events.AlertRaisedEventListener, "
            + "which translates them into a NotifyAlertRaisedCommand")
        .check(productionClasses);
  }

  // ---------------------------------------------------------------------------------------------
  // 3. Cross-context encapsulation
  // ---------------------------------------------------------------------------------------------

  static Stream<String> boundedContexts() {
    return BOUNDED_CONTEXTS.stream();
  }

  @ParameterizedTest(name = "{0} reaches other contexts only through their published contracts")
  @MethodSource("boundedContexts")
  void contextReachesOthersOnlyThroughPublishedContracts(String context) {
    var otherContexts = BOUNDED_CONTEXTS.stream()
        .filter(other -> !other.equals(context))
        .map(other -> ROOT_PACKAGE + "." + other + "..")
        .toArray(String[]::new);
    DescribedPredicate<JavaClass> internalsOfOtherContexts =
        resideInAnyPackage(otherContexts).and(not(resideInAnyPackage(PUBLISHED_CONTRACTS)));
    noClasses().that().resideInAPackage(ROOT_PACKAGE + "." + context + "..")
        .should().dependOnClassesThat(internalsOfOtherContexts)
        .because("another context's domain, application, infrastructure and REST layers are private to it")
        .check(productionClasses);
  }

  @Test
  void sharedKernelDependsOnNoBoundedContext() {
    var contexts = BOUNDED_CONTEXTS.stream().map(context -> ROOT_PACKAGE + "." + context + "..").toArray(String[]::new);
    noClasses().that().resideInAPackage(ROOT_PACKAGE + ".shared..")
        .should().dependOnClassesThat().resideInAnyPackage(contexts)
        .because("the shared kernel is depended upon by every context and must depend on none")
        .check(productionClasses);
  }

  // ---------------------------------------------------------------------------------------------
  // 4. Enum parity
  // ---------------------------------------------------------------------------------------------

  /**
   * Pairs every {@code <Name>Resource} enum of a context's {@code interfaces.rest} layer with the
   * {@code <Name>} enum of the same context's {@code domain.model.valueobjects} package and checks
   * they declare exactly the same constant names. A REST enum without a domain counterpart fails
   * too, so a renamed domain enum cannot silently leave its mirror behind.
   */
  @Test
  void restEnumsMirrorTheirDomainEnums() {
    var restEnums = StreamSupport.stream(productionClasses.spliterator(), false)
        .filter(JavaClass::isEnum)
        .filter(javaClass -> javaClass.getSimpleName().endsWith("Resource"))
        .filter(javaClass -> javaClass.getPackageName().contains(".interfaces.rest"))
        .toList();
    assertFalse(restEnums.isEmpty(), "No REST enum found: the parity check would pass vacuously");

    for (var restEnum : restEnums) {
      var contextPackage = restEnum.getPackageName().substring(0, restEnum.getPackageName().indexOf(".interfaces.rest"));
      var domainSimpleName = restEnum.getSimpleName().substring(0, restEnum.getSimpleName().length() - "Resource".length());
      var domainEnumName = contextPackage + ".domain.model.valueobjects." + domainSimpleName;

      assertTrue(productionClasses.contain(domainEnumName),
          () -> "%s has no domain counterpart %s".formatted(restEnum.getName(), domainEnumName));
      var domainEnum = productionClasses.get(domainEnumName);
      assertTrue(domainEnum.isEnum(), () -> "%s is not an enum".formatted(domainEnumName));

      assertEquals(constantNames(domainEnum), constantNames(restEnum),
          () -> "%s and %s must declare the same constants".formatted(domainEnum.getName(), restEnum.getName()));
    }
  }

  private static Set<String> constantNames(JavaClass enumClass) {
    return Arrays.stream(enumClass.reflect().getEnumConstants())
        .map(constant -> ((Enum<?>) constant).name())
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
