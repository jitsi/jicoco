# jicoco

Jitsi Common Components. This is a multi-module Maven project. It has the Java and Kotlin libraries that Jicofo, Jigasi, Jitsi Videobridge, and other Jitsi projects share.

Modules: `jicoco-config`, `jicoco-health-checker`, `jicoco-jetty`, `jicoco-jwt`, `jicoco-mediajson`, `jicoco-metrics`, `jicoco-mucclient`, `jicoco-test-kotlin`, `jicoco-tracing`.

## jicoco-mediajson

`jicoco-mediajson` defines the WebSocket wire protocol between the Jitsi Videobridge exporter and `opus-transcriber-proxy` (transcription and audio-translation export). The types are in `org.jitsi.mediajson` (`MediaJson.kt`). Both ends of the link use this module. Start here for a change to that protocol.

- `Event` is a sealed class. Jackson selects the subtype from the `"event"` JSON field (`@JsonTypeInfo(property = "event")`). The `@JsonSubTypes` list on `Event` registers the types that a receiver can parse: `media`, `ping`, `pong`, `start`, `stop`, `transcription-result`, `info`.
- Only the bridge sends `session-end`, `sources`, and `speakers`. They are not registered as subtypes, because no receiver parses them.
- To add an event type that a receiver must parse, add the class and add it to the `@JsonSubTypes` list. An unregistered type serializes correctly, but `Event.parse` throws `InvalidTypeIdException` for it. The bridge exporter catches this error, logs a warning, and keeps the connection open. The event does not get to application code.
- `TranscriptionResultEvent` and `InfoEvent` are free-form property bags (`@JsonAnySetter`/`@JsonAnyGetter`, `@JsonIgnoreProperties(["event"])`). Either side can add or remove fields without a schema change. Use this pattern for observability and metadata messages. Use a typed `data class` (for example `MediaEvent` or `StartEvent`) only when both ends require the fields.
- Some numeric fields are JSON strings on the wire, to match the VoxImplant format (for example `sequenceNumber`, `chunk`, `Media.timestamp`). These fields use `Int2StringSerializer`/`String2IntDeserializer` or `Long2StringSerializer`/`String2LongDeserializer`. Jackson does not do this by default. Copy this pattern for fields from the VoxImplant format. Encode new fields that are not in the VoxImplant format as JSON numbers (for example `Media.audioLevel`, `Start.timestamp`).

## Tests

Most modules use kotest (`kotest-runner-junit5-jvm`) with the default provider of `maven-surefire-plugin`. Kotest registers tests at run time, but surefire counts tests statically. So for each top-level `context()` block of a `ShouldSpec`, the report can show `Tests run: 0, Failures: 0, Errors: 0`, even when the tests ran and passed.

- To test one module, run `mvn -pl <module> test` (for example `mvn -pl jicoco-mediajson test`).
- Check for `BUILD SUCCESS`. Do not use the test count in the report. A failed test still fails the build.

## Maven Central release profiles

The root `pom.xml` puts the modules in two profiles. Both profiles are active by default.

- `central-modules`: the release job publishes these modules to Maven Central. They must not depend on an artifact that is not on Central.
- `noncentral-modules`: these modules are not published. This profile adds the Jitsi Maven repository on GitHub, which has the Jitsi fork of `smack-core` and other forks. `jicoco-mucclient` depends on the Jitsi `smack-core`, so it is only in this profile.

The release job builds with `-Pcentral-modules,release,signing`, so it does not use the GitHub repository. The test job enables both profiles. Thus a central module with a GitHub-only dependency passes the tests and fails only at release.

Before you add a module or a dependency to `central-modules`, examine the full dependency tree, including transitive dependencies. Look for any artifact that is only in the GitHub repository. For example, `jitsi-xmpp-extensions` is on Central, but its POM on Central pulls in the Jitsi fork of `smack-core`.

## Versioning

On each push to `master`, the release job sets the version to the output of `git describe --match "v[0-9\.]*" --long --dirty --always` without the leading `v`, then deploys. The released version has the form `<major>.<minor>-<commits>-g<sha>`, the same scheme as other Jitsi Maven projects (for example the `jitsi-utils` dependency `1.0-153-gd5c0102`). Use this form when you update a jicoco version in a downstream `pom.xml`.
