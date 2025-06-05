import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;

import java.util.*;

public final class FogNodeApp extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

    private final List<AggregatedTrafficMsg> history = new ArrayList<>();

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Fog Node started");
        getOs().getAdHocModule().enable(
            new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50)
                .create()
        );
    }

@Override
public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
    if (receivedV2xMessage == null || receivedV2xMessage.getMessage() == null) return;

    Object message = receivedV2xMessage.getMessage();

    if (message instanceof AggregatedTrafficMsg) {
        AggregatedTrafficMsg msg = (AggregatedTrafficMsg) message;
        history.add(msg);

        int totalR0 = msg.getRoute0VehicleCount();
        int totalR1 = msg.getRoute1VehicleCount();

        double avgR0 = history.stream().mapToInt(AggregatedTrafficMsg::getRoute0VehicleCount).average().orElse(0);
        double avgR1 = history.stream().mapToInt(AggregatedTrafficMsg::getRoute1VehicleCount).average().orElse(0);

        getLog().infoSimTime(this,
            "Received AggregatedTrafficMsg: route0VehicleCount={}, route1VehicleCount={}, avgRoute0VehicleCount={}, avgRoute1VehicleCount={}",
            totalR0, totalR1,
            String.format("%.2f", avgR0),
            String.format("%.2f", avgR1)
        );

        sendMetricsToRSU(avgR0, avgR1);
    }
}

private void sendMetricsToRSU(double avgR0, double avgR1) {
    MessageRouting routing = getOs().getAdHocModule()
        .createMessageRouting()
        .topoBroadCast();
    FogMetricsMsg metricsMsg = new FogMetricsMsg(routing, avgR0, avgR1);
    getOs().getAdHocModule().sendV2xMessage(metricsMsg);
    getLog().infoSimTime(this,
        "Sent FogMetricsMsg to RSU: avgRoute0VehicleCount={}, avgRoute1VehicleCount={}",
        String.format("%.2f", avgR0),
        String.format("%.2f", avgR1)
    );
}
    @Override
    public void processEvent(Event event) {}

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement ack) {}

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {}

    @Override
    public void onMessageTransmitted(V2xMessageTransmission tx) {}

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Fog Node shutdown");
    }
}