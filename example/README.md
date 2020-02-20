# Example Payara Application

An example that uses `dbc-commons-mdc`

## Building

Remember to build library first (and install it) by running: `(cd .. && mvn install)`

The you can build the JakartaEE application by running: `mvn package` - this also downloads a `payara-micro.jar`

## Running

You can deploy it by running:
```
java -Dlogback.configurationFile=$PWD/logback.xml -DlogbackDisableServletContainerInitializer=true \
      -Dhazelcast.phone.home.enabled=false \
      -jar target/payara-micro.jar \
      --nocluster \
      --contextroot / --deploy target/dbc-commons-mdc-example-1.0-SNAPSHOT.war
```

## Testing

Using `curl` to test it

 * `curl -D /dev/stdout 'http://localhost:8080/api/ping?s=25'` - tells it to log sleep = 25 and random trackingId
 * `curl -D /dev/stdout 'http://localhost:8080/api/ping?s=25\&t='` - tells it to log sleep = 25 and random trackingId
 * `curl -D /dev/stdout 'http://localhost:8080/api/ping?s=25\&t=myTrack'` - tells it to log sleep = 25 and trackingId = myTrack

You will hit [this class](src/main/java/dk/dbc/example/Ping.java), and given it's annotations you should expect the output to be a lot like this (this is pretty printed using `jq`):

```
{
  "timestamp": "....",
  "version": "1",
  "message": "Ping?",
  "logger": "dk.dbc.example.Ping",
  "thread": "http-thread-pool::http-listener(1)",
  "level": "INFO",
  "level_value": 20000,
  "HOSTNAME": "...",
  "mdc": {
    "sleep": "25",
    "trackingId": "..."
  }
}
```