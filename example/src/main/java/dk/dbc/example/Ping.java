package dk.dbc.example;

import dk.dbc.commons.mdc.GenerateTrackingId;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import dk.dbc.commons.mdc.LogAs;

@Stateless
@Path("ping")
public class Ping {

    public static final Logger log = LoggerFactory.getLogger(Ping.class);

    @GET
    public Status ping(@Context UriInfo UriInfo,
                       @QueryParam("s") @LogAs("sleep") int sleep,
                       @QueryParam("t") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        log.info("Ping?", sleep);
        try {
            if (sleep > 0)
                Thread.sleep(sleep);
        } catch (InterruptedException ex) {
            throw new InternalServerErrorException(ex);
        }
        return Status.ok("pong!");
    }

    public static class Status {

        public boolean ok;
        public String message;

        public Status() {
        }

        private Status(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        static Status ok(String message) {
            return new Status(true, message);
        }

        @Override
        public String toString() {
            return "Status{" + "ok=" + ok + ", message=" + message + '}';
        }
    }
}
