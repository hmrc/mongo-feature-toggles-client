
# mongo-feature-toggles-client

This library is a client to use mongo as storage for feature toggles.

## How it works

* Create a toggle

Create a case object extending the FeatureFlagName trait

```scala
case object PertaxBackendToggle extends FeatureFlagName {
  val name                                 = "pertax-backend-toggle"
  override val description: Option[String] = Some(
    "Enable/disable pertax backend during auth"
  )
}
```

* Register toggles on application start

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

* Read the state of a toggle

The method `get` from `FeatureFlagName` gets the state of the toggle.

```scala
PertaxBackendToggle.get
```

* Write a new state of a toggle

The method `set` from `FeatureFlagName` sets a new state of the toggle.

```scala
PertaxBackendToggle.set(true)
```

* Write the state of multiple toggles simultaneously

```scala
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagImplicits.ListFeatureFlagSetAll

List(FeatureFlag(Toggle1, true), FeatureFlag(Toggle2, false)).setAll()
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").