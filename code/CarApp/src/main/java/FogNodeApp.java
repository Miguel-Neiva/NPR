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

public final class FogNodeApp extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

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
            getLog().infoSimTime(this, "Received aggregated data from RSU: r_0={} vehicles, r_1={} vehicles",
                msg.getRoute0VehicleCount(), msg.getRoute1VehicleCount());
            // Aqui podes guardar, agregar ou tomar decisões com estes dados
        }
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