
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.*;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.util.*;

public final class RSUProgram extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

    // Constants
    private static final int LIMIAR_CONGESTIONAMENTO = 3; // número de carros para considerar congestionamento

    private static final long SAMPLE_INTERVAL = 2 * TIME.SECOND;
    private static final long FOG_SEND_INTERVAL = 10 * TIME.SECOND;
    private static final int RSU_RECEIVE_DISTANCE = 100;
    private static final String SECRET_KEY = "ABRE";

    // Position of RSU
    private CartesianPoint rsuPosition;

    // State
    private final Map<String, Set<String>> vehicleRoutes = new HashMap<>();
    private final Set<String> seenGreenWaveMessages = new HashSet<>();

    @Override
    public void onStartup() {
        log("Initializing RSU application...");

        // Enable AdHoc communication
        getOs().getAdHocModule().enable(
            new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50.0D)
                .create()
        );

        rsuPosition = getOs().getPosition().toCartesian();

        vehicleRoutes.put("r_0", new HashSet<>());
        vehicleRoutes.put("r_1", new HashSet<>());

        scheduleNextSample();
        scheduleFogUpdate();
    }

@Override
public void onMessageReceived(ReceivedV2xMessage receivedMessage) {
    if (!(receivedMessage.getMessage() instanceof GreenWaveMsg)) return;

    GreenWaveMsg msg = (GreenWaveMsg) receivedMessage.getMessage();
    String messageId = msg.getSegredo() + "|" + msg.getRota() + "|" + msg.getId_carro();

    // Verifica o segredo antes de processar
    if (!SECRET_KEY.equals(msg.getSegredo())) {
        log("Message Ignored Invalid Secret: " + msg.getSegredo());
        return;
    }

    if (seenGreenWaveMessages.contains(messageId)) return;
    seenGreenWaveMessages.add(messageId);

    // Adiciona o carro à rota
    vehicleRoutes.computeIfAbsent(msg.getRota(), k -> new HashSet<>()).add(msg.getId_carro());

    // Verifica se há congestionamento
    Set<String> carrosNaRota = vehicleRoutes.get(msg.getRota());
    if (carrosNaRota.size() >= LIMIAR_CONGESTIONAMENTO) {
        // Envia comando para o semáforo mudar para verde nesta rota
        MessageRouting routing = getOs().getAdHocModule()
            .createMessageRouting()
            .topoCast("trafficlight_0", 1);
        String program = msg.getRota().equals("r_0") ? "0" : "2";
        getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, program, "trafficlight_0"));
        carrosNaRota.clear(); // Limpa para não enviar várias vezes seguidas
    }

    // Envia ACK normalmente
    sendAck(msg.getId_carro(), msg.getRouting().getSource().getSourceName());
}
    private void sendAck(String carId, String recipientName) {
        MessageRouting routing = getOs().getAdHocModule()
            .createMessageRouting()
            .topoCast(recipientName, 4);

        getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, "ACK", carId));
        log("Sent ACK to " + carId);
    }

    private void scheduleNextSample() {
        getOs().getEventManager().addEvent(
            getOs().getSimulationTime() + SAMPLE_INTERVAL,
            this
        );
    }

    private void scheduleFogUpdate() {
        getOs().getEventManager().addEvent(
            getOs().getSimulationTime() + FOG_SEND_INTERVAL,
            (e) -> sendAggregatedDataToFog()
        );
    }

    private void sendAggregatedDataToFog() {
        int r0Count = vehicleRoutes.getOrDefault("r_0", Collections.emptySet()).size();
        int r1Count = vehicleRoutes.getOrDefault("r_1", Collections.emptySet()).size();

        MessageRouting routing = getOs().getAdHocModule()
            .createMessageRouting()
            .topoCast("FogNode_0", 1);

        AggregatedTrafficMsg aggMsg = new AggregatedTrafficMsg(routing, r0Count, r1Count);
        getOs().getAdHocModule().sendV2xMessage(aggMsg);

        log("Sent aggregated data: r_0=" + r0Count + ", r_1=" + r1Count);

        vehicleRoutes.get("r_0").clear();
        vehicleRoutes.get("r_1").clear();

        scheduleFogUpdate();
    }

    @Override
    public void processEvent(Event event) {
        // Called periodically to sample or maintain activity
        scheduleNextSample();
    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement ack) {}

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {}

    @Override
    public void onMessageTransmitted(V2xMessageTransmission tx) {}

    @Override
    public void onShutdown() {
        log("Shutting down RSU application.");
    }

    private void log(String message) {
        getLog().infoSimTime(this, message);
    }
}
