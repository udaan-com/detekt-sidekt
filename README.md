# detekt-sidekt

detekt extension that shamelessly kangs IntelliJ annotations. may probably not be needed once Jetbrains releases their
gradle/maven plugin to do the same.

Inspections provided:

 - **BlockingCallContext**: inspect blocking method calls within non-blocking context
 - **BlockingCallContextReclaimable**: infer blocking calls within `Dispatchers.IO` context which can be migrated to
   non-blocking alternatives
 - **JerseyMethodParameterDefaultValue**: infer if a probable jersey method contains a parameter with a default value
- **JerseyMainThreadBlockingCall**: infer if a probable jersey resource method contains a main thread blocking call(runblocking)
- **JsonIgnorePropertiesOnDataClass**: infer if a data class is not annotated with `@JsonIgnoreProperties(ignoreUnknown=true)`.

## detekt run

requires type resolution, so ensure you are setting the `--classpath` argument

```bash
java -jar detekt-cli-1.16.0-all.jar \
  --plugins sidekt-1.0-SNAPSHOT.jar \
  --classpath 'dep1.jar:dep2.jar' \
  --jvm-target 1.8 \
  --input 'src/' \
  --config defalt-detekt-config.yml
```

find a way to fail the build if these errors come up by setting very high weights

```yml
build:
  maxIssues: 99999 # ...? not sure what's a better way to _force_ an error
  excludeCorrectable: false
  weights:
    BlockingCallContext: 100000
    JerseyMethodParameterDefaultValue: 100000
```

## BlockingCallContext, BlockingCallContextReclaimable

```yml
sidekt:
  BlockingCallContext:
    active: true
    # debug: 'stderr'
    blockingMethodAnnotations: ['com.custom.annotations.BlockingCall']
    blockingMethodFqNames: ['com.custom.wrapper.runBlocking']
    blockingExceptionTypes: ['com.amazonaws.http.timers.client.SdkInterruptedException']
    ioDispatcherFqNames: ['com.custom.Dispatchers.DB']
    reclaimableMethodAnnotations: ['com.custom.annotations.ReclaimableBlockingCall']
    reclaimableMethodFqNames: ['com.custom.wrapper.getSync']
    blockingClassAnnotations: ['com.custom.annotations.BlockingClass']
    blockingClassFqNames: ['com.custom.dao.SomeDao']

```

### debug

prints a whole lot of debug information, please do not set this.

takes one of 'stdout', 'stderr', or a directory path

### blockingMethodAnnotations

these are the annotations which you may use in your code to denote blocking methods

### blockingMethodFqNames

in case the annotations marking the method as blocking cannot be inferred (probably due to annotation retention at SOURCE), then,
such known methods' FQ names can be configured

### blockingExceptionTypes

methods annotated with `@kotlin.jvm.Throws`, and which end up throwing certain exceptions, can be used to identify blocking
methods.

please note: this will only work for methods in your own source code, and cannot come from the classpath since `@kotlin.jvm.Throws`
annotation retention is at SOURCE

### ioDispatcherFqNames

blocking calls are allowed within specific dispatchers, namely, Dispatchers.IO; given they are specifically for the
purpose. in case you have an additional IO dispatcher, you may configure the same

### reclaimableMethodAnnotations

albeit dispatchers specifically meant for IO are meant for such "blocking" calls, there may be certain methods which
already provide non-blocking method alternatives. such methods may be annotated with a distinct annotation, and configured

### reclaimableMethodFqNames

same as `reclaimableMethodAnnotations`, but allowing you to provide FQ names to the methods

### blockingClassAnnotations

these are the annotations which you may use in your code to denote blocking classes.

similar to [blockingMethodAnnotations](#blockingMethodAnnotations), for classes.

### blockingClassFqNames

in case the annotations marking the class as blocking cannot be inferred (probably due to annotation retention at SOURCE), then,
such known classes' FQ names can be configured.

similar to [blockingMethodFqNames](#blockingMethodFqNames), for classes.

## JerseyMethodParameterDefaultValue

```yml
sidekt:
  JerseyMethodParameterDefaultValue:
    active: true
    # debug: 'stderr'
```

**JerseyMethodParameterDefaultValue**

this is in reference to the following issues; even with this being "fixed" (as in, not crash at runtime)
in later versions, this is a pitfall; since nullable parameters always get set to null
(due to how method matching works), and non-nullable values, if primitives, get set to the default values,
and if not, throw a "Parameter specified as non-null is null" error on invocation.

 - [KT-27947](https://youtrack.jetbrains.com/issue/KT-27947)
 - [KT-28684](https://youtrack.jetbrains.com/issue/KT-28684)


## JerseyMainThreadBlockingCall

```yml
sidekt:
   JerseyMainThreadBlockingCall:
    active: true
    # debug: 'stderr'
```
## ResourceOnboardedOnAsec

```yml
sidekt:
   ResourceOnboardedOnAsec:
    active: true
    # debug: 'stderr'
```

## JsonIgnorePropertiesOnDataClass
```yml
sidekt:
   JsonIgnorePropertiesOnDataClass:
    active: true
    excludes: "com.example.excluded, another.package"
    # debug: 'stderr'
```
#### JsonIgnorePropertiesOnDataClass
This rule enforces the use of the `@JsonIgnoreProperties` annotation on all data classes within the codebase. 
Data classes must be annotated with `@JsonIgnoreProperties(ignoreUnknown = true)` to ensure compatibility with JSON deserialization.
### excludes
This allows exclusion of certain packages where you don't want to run the check
