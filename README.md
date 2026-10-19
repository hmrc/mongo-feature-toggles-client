
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
If the configuration `mongo-feature-toggles-client.useMongoTransactions = false` is used then the client won't use Mongo transactions. Do not override this in any environment. It is best suited while the service is started via service manager.

### Create a toggle
Write the toggle definition in application.conf. The value of the toggle can be either sourced from Mongo or from application.conf.
toggleSourceValue decide which type of source to use.

```hocon
mongo-feature-toggles-client {
  toggles {
    show-taxcalc-tile-toggle { # name of the toggle (1)
      description = "Toggle the display of the Payments and Repayments tile (NPS/ETMP data) on the home page"

      # Determines where the feature toggle state is sourced from.
      # Allowed values:
      #   - "remote-configuration" -> use remote-configuration
      #   - "local-configuration"  -> use local-configuration
      toggleSourceValue = "remote-configuration"

      # Configuration used when toggleSourceValue is set to "remote-configuration".
      # The defaultState is used when no value is available yet from the remote repository.
      remote-configuration {
        defaultState = true
      }

      # Configuration used when toggleSourceValue is set to "local-configuration".
      # The isEnabled value is read directly from this config file.
      local-configuration {
        isEnabled = true
      }

    }
}
```

In the code create an object extending `FeatureFlagName` with the name equal to the key(1) used to define the toggle.

### Add the additional admin routes in admin.routes
The routes will allow the admin frontend to operate the toggles configured remotely.

```text
-> /featureFlags mongoFeatureTogglesAdmin.Routes
```
if admin.routes does not exist, add an entry in prod.routes
```text
->         /admin                     admin.Routes
```

### Add the additional test-only routes in testOnlyDoNotUseInAppConf.routes
The testOnly routes make it easier to change the toggles from the acceptance tests.

```text
-> /<rootPath> mongoFeatureTogglesTestOnly.Routes
```

### Read the state of a toggle
Inject the `FeatureFlagService` service and use the `get` or `getAsEitherT` methods

```scala
featureFlagService.get(AgentClientRelationshipsToggle).flatMap { toggle =>
  if (toggle.isEnabled) {
    // Do something when the toggle is enabled
  } else {
    // Do something when the toggle is disabled  
  }
```

or

```scala
featureFlagService.getAsEitherT(AgentClientRelationshipsToggle).flatMap { toggle =>
  if (toggle.isEnabled) {
    // Do something when the toggle is enabled
  } else {
    // Do something when the toggle is disabled  
  }
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
