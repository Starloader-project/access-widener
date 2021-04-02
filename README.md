# AccessWidener

AccessWidener is a bytecode modification format designed around changing the
accessibillity of JVM classes, methods and fields. Fabric's AccessWideners
allow for changing the private/package/protected/public status of these as
well as making things mutable (that is stripping down the final flag).

Starloader's AccessWidener v2 additionally support stripping down the synthetic
and enum flags. And introduces an overhaul in the backend, however is largely
backwards compatible with AccessWidener v1 in both API and format.

The artifacts are currently stored within https://geolykt.de/maven/ repository
under the `de.geolykt.starloader:access-widener` artifact.

Additionally a gradle plugin making your life easier when using this format
is available at https://github.com/Starloader-project/widener-plugin/
