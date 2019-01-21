# KDB JAVA CLIENT

Directory: deps/javakdb

obtained from https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java

latest snapshot: 1/17/2019


Optional edits to file: c.java
1. line 58: `public class c implements AutoCloseable {`

This will allow the client to be opened and automatically closed in a try block

```
try (c kdbClient = <initialize KDB Client >) {
    // Do something here
}
```
