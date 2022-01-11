package org.sample;

import com.company.monitoring.service.MonitoringImpl;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
public class MonitoringTest3 {

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
        monitoring.monitorDir("/tmp/some");
    }

    @Actor
    public void actor2() {
        monitoring.monitorDir("/tmp/some");
    }

    @Actor
    public void actor3() {
        monitoring.monitorDir("/tmp/some");
    }


    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = 1;
    }
}
