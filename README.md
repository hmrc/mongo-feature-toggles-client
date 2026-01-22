
# mongo-feature-toggles-client

This library is a client to use mongo as storage for feature toggles.

## How it works

### Add the library
```sbt
"uk.gov.hmrc" %% s"mongo-feature-toggles-client-$playVersion" % hmrcMongoFeatureTogglesClientVersion
```

You can also add the following in Test which will bring bootstrap-play-test and hmrc-mongo-test
```sbt
"uk.gov.hmrc" %% s"mongo-feature-toggles-client-test-$playVersion" % hmrcMongoFeatureTogglesClientVersion
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
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientV2Module"
```

### Mongo transactions override
If the the configuration `mongo-feature-toggles-client.useMongoTransactions = false` is used then the client won't use Mongo transactions. Do not override this in any environment. It is best suited while the service is started via service manager.

### Create a toggle
Create a case object extending the FeatureFlagName trait.

The lockedEnvironments allows to move the toggle per environment in a different part of the UI where their use is restricted.

```scala
case object PertaxBackendToggle extends FeatureFlagName {
    override val description = Some("Description")
    override val name = "toggle-name"
    override val defaultState = false // State of the toggle when no entry is present in Mongo
    override val lockedEnvironments = Seq(Environment.Production) // Locked toggled are placed in a different part of the UI
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
if admin.routes does not exist, add an entry in prod.routes
```text
->         /admin                     admin.Routes
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
