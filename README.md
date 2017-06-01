# Annotation based configuration management

This 'libraries' (it's actually only on .java file) purpose is to provide a very simple and easy to use way to access command line arguments
and actually any property (system property, property files, etc.) in a type safe and default-value-enabled way.

This is achieved by wrapping a simple Map<String, ?> to an arbitrary caller-defined Annotation via dynamic proxies. This looks like this:

```
private @interface TestConfig {
    int intArg() default 1;
}
```

To map this annotation to a command line argument use this:

```
Configuration c = new Configuration();
c.add("--intArg=3"); // can directly feed the main() String[] in here.

TestConfig tc = c.get(TestConfig.class);
tc.intArg(); // yields 3
```

Or to map it to a property:

```
Configuration c = new Configuration();
c.add(Collections.singletonMap("intArg", 3)); // can alternatively also use the String "3" as value

TestConfig tc = c.get(TestConfig.class);
tc.intArg(); // yields 3

```

Some properties (when read from a file, or when accessing system properties) might have names which are invalid as annotation method name, so you can re-map any property to any method:

```
private @interface TestConfig {
    @ConfigurationNameMapping("user.home")
    String userHome();
}

...

Configuration c = new Configuration();
c.add(System.getProperties());

TestConfig tc = c.get(TestConfig.class);
tc.userHome();
```

Feel free to simply copy the Configuration.java file to your own project(s), but keep the copyright header intact :)
