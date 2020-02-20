# MDC 

## Adding MDC fields from call parameters

This is intended to be used in JakartaEE applications, for logging parameters that are important to filter loglines with.

Two annotations has been added:

 * `@LogAs(value = "fieldname", includeNull = false)` - that adds _fieldname_ to the mdc of this call
 * `@GenerateTrackingId` - only applicable to `String` types, if valus is null or empty generate a `uuid` value and put it in the parameter


## Example

A fully running payara-micro demonstration can be found in the [example](example) directory.

```
@Stateless // Or any other bean notation
public class ... {
...
    public void process(@LogAs("recordId") int recordId,
                        @LogAs("trackingId) @GenerateTrackingId String t) {
...
        log.info("Starting");
    }

}
```

## Log output (MDC)

This is built upon the `slf4j` log framework.

