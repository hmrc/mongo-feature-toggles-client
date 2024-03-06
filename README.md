
# mongo-feature-toggles-client

This library is a client to use mongo as storage for feature toggles.

## How it works

### Add the library
```sbt
"uk.gov.hmrc" %% s"mongo-feature-toggles-client-$playVersion" % hmrcMongoFeatureTogglesClientVersion
```

### Add the mongodb uri if not already present in application.conf and app-config-*
Note the library is using transaction in mongo which requires mongo to be run as a cluster. 
If you see the error `com.mongodb.MongoClientException, with message: This MongoDB deployment does not support retryable writes. Please add retryWrites=false to your connection string`
when running tests, your mongo is not running as a cluster.

### Add internal auth binding
The library uses [internal-auth-client](https://github.com/hmrc/internal-auth-client). 
You will need to add the following binding to your ```application.conf```, unless your service already uses the ```internal-auth-client```, in which case this binding will already exist.
```scala
play.modules.enabled += "uk.gov.hmrc.internalauth.client.modules.InternalAuthModule"
```

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
Inject the `FeatureFlagService` service and use the `get` method

```scala
FeatureFlagService.get(PertaxBackendToggle)
```

### Write a new state of a toggle
Inject the `FeatureFlagService` service and use the `set` method

```scala
FeatureFlagService.set(PertaxBackendToggle, true)
```

### Write the state of multiple toggles simultaneously
Inject the `FeatureFlagService` service and use the `setAll` method

```scala
FeatureFlagService.set(Map(
  PertaxBackendToggle -> true,
  AnOtherToggle -> false
))
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").