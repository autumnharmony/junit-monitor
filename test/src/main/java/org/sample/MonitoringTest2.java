package org.sample;

import com.company.monitoring.service.MonitoringImpl;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Outcome(id = "1", expect = ACCEPTABLE, desc = "Starting ok")
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Running ok")
@Outcome(id = "3", expect = FORBIDDEN, desc = "Stopping not ok")
@Outcome(id = "4", expect = FORBIDDEN, desc = "Stopped not ok")
@State
public class MonitoringTest2 {

    private MonitoringImpl monitoring = new MonitoringImpl(){
        @Override
        protected void startImpl() {
            // nothing
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @Actor
    public void actor1() {
        monitoring.start();
    }

    @Actor
    public void actor2() {
        monitoring.stop();
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = monitoring.getState().get().getValue();
    }
}
