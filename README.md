
# mongo-feature-toggles-client

This library is a client to use mongo as storage for feature toggles.

## How it works

### Add the library
```scala
"uk.gov.hmrc" %% "mongo-feature-toggles-client" % "0.1.0"
```

### Add the mongodb uri if not already present in application.conf and app-config-*
Note the library is using transaction in mongo which requires mongo to be run as a cluster. 
If you see the error `com.mongodb.MongoClientException, with message: This MongoDB deployment does not support retryable writes. Please add retryWrites=false to your connection string`
when running tests, your mongo is not running as a cluster.

### Create a toggle
Create a case object extending the FeatureFlagName trait

```scala
case object PertaxBackendToggle extends FeatureFlagName {
  val name                                 = "pertax-backend-toggle"
  override val description: Option[String] = Some(
    "Enable/disable pertax backend during auth"
  )
}
```

### Register toggles on application start
Call the `addFlags` method from the `FeatureFlagNamesLibrary`. Add the class to a module and add the module to application.conf so it gets executed at startup.

```scala
@Singleton
class ApplicationStartUp {
  FeatureFlagNamesLibrary.addFlags(List(PertaxBackendToggle))
}
```

```scala
class HmrcModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[ApplicationStartUp].toSelf.eagerly()
  )
}
```

```text
play.modules.enabled += "config.HmrcModule"
```

### Add the additional admin routes in admin.routes
```text 
-> /featureFlags mongoFeatureTogglesAdmin.Routes
```

### Add the additional test-only routes in testOnlyDoNotUseInAppConf.routes
```text 
-> /<rootPath> mongoFeatureTogglesTestOnly.Routes
```

### Read the state of a toggle
The method `get` from `FeatureFlagName` gets the state of the toggle.

```scala
PertaxBackendToggle.get
```

### Write a new state of a toggle
The method `set` from `FeatureFlagName` sets a new state of the toggle.

```scala
PertaxBackendToggle.set(true)
```

### Write the state of multiple toggles simultaneously

```scala
import.ListFeatureFlagSetAll

List(FeatureFlag(Toggle1, true), FeatureFlag(Toggle2, false)).setAll()
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").