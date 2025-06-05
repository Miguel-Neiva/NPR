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
    private static final int LIMIAR_CONGESTIONAMENTO = 15;
    private static final long SAMPLE_INTERVAL = 2 * TIME.SECOND;
    private static final long FOG_SEND_INTERVAL = 10 * TIME.SECOND;
    private static final String SECRET_KEY = "OPEN!";
    private String lastProgramSent = "3"; 
    private boolean waitingForRed = false;

    public static final double MIN_DISTANCE_RSU = 40.0;

    private CartesianPoint rsuPosition;

    // State
    private final Map<String, Set<String>> vehicleRoutes = new HashMap<>();
    private final Map<String, Double> vehicleSpeeds = new HashMap<>(); // <ID do veículo, velocidade>

    @Override
    public void onStartup() {
        log("Initializing RSU application...");
        rsuPosition = getOs().getPosition().toCartesian();
        getLog().infoSimTime(this, "My position is: {}", rsuPosition);

        // Envia a posição da RSU para todos os carros
        MessageRouting routing = getOs().getAdHocModule().createMessageRouting().topoBroadCast();
        RSUMsg msg = new RSUMsg(routing, "RSU_POS|" + rsuPosition.getX() + "|" + rsuPosition.getY(), "ALL");
        getOs().getAdHocModule().sendV2xMessage(msg);

        // Enable AdHoc communication
        getOs().getAdHocModule().enable(
            new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50.0D)
                .create()
        );

        vehicleRoutes.put("r_0", new HashSet<>());
        vehicleRoutes.put("r_1", new HashSet<>());

        scheduleNextSample();
        scheduleFogUpdate();
    }

    @Override
public void onMessageReceived(ReceivedV2xMessage receivedMessage) {
    // Recebe dados dos carros
    if (receivedMessage.getMessage() instanceof GreenWaveMsg) {
        GreenWaveMsg msg = (GreenWaveMsg) receivedMessage.getMessage();

        // Usa a posição recebida na mensagem para calcular a distância ao RSU
        if (msg.getPosX() != 0.0 || msg.getPosY() != 0.0) {
            CartesianPoint senderPos = CartesianPoint.xy(msg.getPosX(), msg.getPosY());
            double distance = rsuPosition.distanceTo(senderPos);
            if (distance > MIN_DISTANCE_RSU) {
                log("Ignored message from " + msg.getId_carro() + " (distance " + String.format("%.2f", distance) + " > " + MIN_DISTANCE_RSU + ")");
                return;
            }
        }

        // Verifica o segredo antes de processar
        if (!SECRET_KEY.equals(msg.getSegredo())) {
            log("Message Ignored Invalid Secret: " + msg.getSegredo());
            return;
        }

        vehicleRoutes.computeIfAbsent(msg.getRota(), k -> new HashSet<>()).add(msg.getId_carro());
        log("Received GreenWaveMsg from " + msg.getId_carro() + " on route " + msg.getRota() + " with speed " + msg.getVelocidade());
        vehicleSpeeds.put(msg.getId_carro(), msg.getVelocidade());

        Set<String> carrosNaRota = vehicleRoutes.get(msg.getRota());
        // Só envia comando se não estiver à espera do vermelho
        if (carrosNaRota.size() >= LIMIAR_CONGESTIONAMENTO && !waitingForRed) {
            String program = msg.getRota().equals("r_0") ? "0" : "2";
            MessageRouting routing = getOs().getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();
            getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, program, "TrafficLight"));
            log("Sent program " + program + " to TrafficLight");
            waitingForRed = true;
            carrosNaRota.forEach(vehicleSpeeds::remove); // Limpa velocidades dos carros dessa rota
            carrosNaRota.clear();
        }

        sendAck(msg.getId_carro(), msg.getRouting().getSource().getSourceName());
    }

    // Recebe confirmação do semáforo que voltou a vermelho
    if (receivedMessage.getMessage() instanceof RSUMsg) {
        RSUMsg msg = (RSUMsg) receivedMessage.getMessage();
        if ("RED".equals(msg.getMessage())) {
            waitingForRed = false;
            log("TrafficLight voltou a vermelho, pronto para novo comando.");
        }
    }

    if (receivedMessage.getMessage() instanceof FogMetricsMsg) {
        FogMetricsMsg metrics = (FogMetricsMsg) receivedMessage.getMessage();
        MessageRouting routing = getOs().getAdHocModule().createMessageRouting().topoBroadCast();
        String metricsPayload = "METRICS|" + metrics.getAvgR0() + "|" + metrics.getAvgR1();
        getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, metricsPayload, "FogNode"));
        log("Forwarded metrics to vehicles: avgR0=" + metrics.getAvgR0() + ", avgR1=" + metrics.getAvgR1());
    }
}

    // Envia ACK normalmente
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

        double r0AvgSpeed = calculateAvgSpeed("r_0");
        double r1AvgSpeed = calculateAvgSpeed("r_1");

        MessageRouting routing = getOs().getAdHocModule()
            .createMessageRouting()
            .topoBroadCast();

        AggregatedTrafficMsg aggMsg = new AggregatedTrafficMsg(routing, r0Count, r1Count, r0AvgSpeed, r1AvgSpeed);
        getOs().getAdHocModule().sendV2xMessage(aggMsg);

        log(String.format("Sent aggregated data: r_0=%d (%.2f m/s), r_1=%d (%.2f m/s)", r0Count, r0AvgSpeed, r1Count, r1AvgSpeed));

        // Limpa velocidades e carros das rotas
        vehicleRoutes.get("r_0").forEach(vehicleSpeeds::remove);
        vehicleRoutes.get("r_0").clear();
        vehicleRoutes.get("r_1").forEach(vehicleSpeeds::remove);
        vehicleRoutes.get("r_1").clear();

        scheduleFogUpdate();
    }

    private double calculateAvgSpeed(String route) {
        Set<String> vehicles = vehicleRoutes.getOrDefault(route, Collections.emptySet());
        if (vehicles.isEmpty()) return 0.0;

        double totalSpeed = 0.0;
        int count = 0;
        for (String vehicleId : vehicles) {
            Double speed = getVehicleSpeed(vehicleId);
            if (speed != null) {
                totalSpeed += speed;
                count++;
            }
        }
        return count > 0 ? totalSpeed / count : 0.0;
    }

    private Double getVehicleSpeed(String vehicleId) {
        return vehicleSpeeds.get(vehicleId);
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

    @Override
    public void processEvent(Event event) {

        scheduleNextSample();
    }
}