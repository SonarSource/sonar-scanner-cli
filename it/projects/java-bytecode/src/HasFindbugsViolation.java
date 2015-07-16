public class HasFindbugsViolation {

  private String field;

  public void use() {
    new DeprecatedExample().deprecatedMethod(); // violation of squid rule: CallToDeprecatedMethod
    System.exit(33); // violation on findbugs rule: DM_EXIT
  }

  public void useFieldForLcom4() {
    System.out.println(field);
  }
}
